import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { existsSync, readdirSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const frontendRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const repoRoot = path.resolve(frontendRoot, "..");

const apps = [
  {
    key: "checkout",
    module: "frontend-checkout",
    packageName: "@nexusflow/checkout",
    apiFile: "checkoutApi.ts",
    requiredEndpoints: ["/cashier/order/status", "/cashier/pay/submit"],
    requiredUi: ["Generate Address", "Polling every 5s", "Address QR"]
  },
  {
    key: "merchant",
    module: "frontend-merchant",
    packageName: "@nexusflow/merchant",
    apiFile: "merchantApi.ts",
    requiredEndpoints: ["/auth/login", "/auth/me", "/auth/active-merchant", "/pay/order", "/refund/order"],
    requiredUi: ["Create order", "Order lookup", "Submit Refund", "Recent orders"]
  },
  {
    key: "ops",
    module: "frontend-ops",
    packageName: "@nexusflow/ops",
    apiFile: "opsApi.ts",
    requiredEndpoints: ["/ops/dashboard", "/crypto/orphan-transactions", "/ops/webhook-dead-letters"],
    requiredUi: ["Channel monitor", "Risk alerts", "Orphan transactions", "Webhook dead letters"]
  },
  {
    key: "admin",
    module: "frontend-admin",
    packageName: "@nexusflow/admin",
    apiFile: "adminApi.ts",
    requiredEndpoints: ["/admin/roles", "/admin/permissions", "/admin/users/grant-role"],
    requiredUi: ["Roles and permissions", "Permission catalog", "Lookup user roles", "Grant role"]
  }
];

function read(relativePath, root = frontendRoot) {
  return readFileSync(path.join(root, relativePath), "utf8");
}

function assertIncludes(haystack, needles, label) {
  for (const needle of needles) {
    assert.match(haystack, new RegExp(escapeRegExp(needle), "i"), `${label} should include ${needle}`);
  }
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

describe("frontend workspace", () => {
  test("declares the Vue workspace and verification scripts", () => {
    const pkg = JSON.parse(read("package.json"));
    assert.equal(pkg.private, true);
    assert.deepEqual(pkg.workspaces, ["apps/*", "packages/*"]);
    assert.match(pkg.scripts.build, /build:checkout/);
    assert.match(pkg.scripts.build, /build:admin/);
    assert.match(pkg.scripts.typecheck, /typecheck:checkout/);
    assert.match(pkg.scripts.typecheck, /typecheck:admin/);
    assert.equal(pkg.scripts["test:smoke"], "node --test tests/*.test.mjs");
    assert.equal(pkg.scripts["prune:dist"], "node scripts/prune-dist-app.mjs");
    assert.equal(pkg.scripts["test:e2e"], "playwright test");
    assert.match(pkg.scripts.verify, /typecheck/);
    assert.match(pkg.scripts.verify, /build/);
    assert.match(pkg.scripts.verify, /prune:dist/);
    assert.match(pkg.scripts.verify, /test:smoke/);
    assert.equal(pkg.scripts["verify:e2e"], "npm run verify && npm run test:e2e");
    assert.ok(pkg.devDependencies["@playwright/test"], "Playwright should be available for browser E2E");
  });

  test("shared API client keeps session credentials and core HTTP verbs", () => {
    const source = read("packages/api-client/src/index.ts");
    assertIncludes(source, ["credentials: \"include\"", "apiGet", "apiPost", "apiPut", "apiDelete"], "api client");
  });

  test("Vite builds use relative assets and lock-tolerant output names", () => {
    const source = read("vite.shared.ts");
    assertIncludes(source, ["base: \"./\"", "outDir: \"dist-app\"", "emptyOutDir: false", "NEXUSFLOW_FRONTEND_BUILD_ID"], "shared Vite config");
    assert.match(source, /entryFileNames: `assets\/\[name\]-\$\{buildId\}-\[hash\]\.js`/);
    assert.match(source, /assetFileNames: `assets\/\[name\]-\$\{buildId\}-\[hash\]\[extname\]`/);
  });

  test("Playwright serves built apps over HTTP", () => {
    const config = read("playwright.config.ts");
    const e2e = read("e2e/vue-apps.spec.ts");
    const server = read("scripts/serve-dist-apps.mjs");
    assertIncludes(config, ["baseURL: \"http://127.0.0.1:4179\"", "projects", "chromium"], "Playwright config");
    assertIncludes(e2e, ["createServer", "server.close", "dist-app", "Vue dist-app boots in Chromium"], "Playwright E2E");
    assertIncludes(server, ["checkout", "merchant", "ops", "admin", "dist-app", "/health"], "dist-app server");
  });

  test("CI verifies frontend assets before Maven packaging", () => {
    const workflow = read(".github/workflows/ci.yml", repoRoot);
    assertIncludes(workflow, [
      "actions/setup-node@v4",
      "cache-dependency-path: frontend/package-lock.json",
      "working-directory: frontend",
      "run: npm ci",
      "npx playwright install --with-deps chromium",
      "run: npm run verify:e2e",
      "frontend/apps/*/dist-app/**"
    ], "CI workflow");
    assert.ok(
      workflow.indexOf("run: npm run verify:e2e") < workflow.indexOf("run: mvn -B -DskipTests clean install"),
      "CI should build Vue assets before Maven packages frontend-* modules"
    );
  });
});

describe("Vue apps", () => {
  for (const app of apps) {
    test(`${app.key} has package, Vite entry, UI flow, and API wiring`, () => {
      const appRoot = `apps/${app.key}`;
      const pkg = JSON.parse(read(`${appRoot}/package.json`));
      const viteConfig = read(`${appRoot}/vite.config.ts`);
      const source = read(`${appRoot}/src/App.vue`);
      const api = read(`${appRoot}/src/${app.apiFile}`);

      assert.equal(pkg.name, app.packageName);
      assert.equal(pkg.type, "module");
      assert.equal(pkg.scripts.build, "vue-tsc --noEmit && vite build");
      assert.match(viteConfig, /createViteConfig/);
      assert.match(source, /<script setup lang="ts">/);
      assert.match(source, /configureApiClient/);
      assertIncludes(source, app.requiredUi, `${app.key} App.vue`);
      assertIncludes(api, app.requiredEndpoints, `${app.key} API`);
    });
  }
});

describe("build outputs and Maven handoff", () => {
  for (const app of apps) {
    test(`${app.key} dist can be served from static/app`, () => {
      const distRoot = path.join(frontendRoot, "apps", app.key, "dist-app");
      const indexPath = path.join(distRoot, "index.html");
      const assetsRoot = path.join(distRoot, "assets");

      assert.equal(existsSync(indexPath), true, `${app.key} dist-app index.html is missing; run npm.cmd run build`);
      assert.equal(existsSync(assetsRoot), true, `${app.key} dist-app assets directory is missing; run npm.cmd run build`);

      const indexHtml = readFileSync(indexPath, "utf8");
      const assets = readdirSync(assetsRoot);
      assert.match(indexHtml, /type="module"/);
      assert.match(indexHtml, /\.\/assets\//);
      assert.ok(assets.some((name) => name.endsWith(".js")), `${app.key} should emit a JavaScript asset`);
      assert.ok(assets.some((name) => name.endsWith(".css")), `${app.key} should emit a CSS asset`);
      for (const assetName of indexHtml.matchAll(/\.\/assets\/([^"']+)/g)) {
        assert.equal(existsSync(path.join(assetsRoot, assetName[1])), true, `${app.key} references missing asset ${assetName[1]}`);
      }
      assert.equal(assets.length, 2, `${app.key} should keep only the current JS and CSS assets`);
    });

    test(`${app.module} packages ${app.key} dist under static/app`, () => {
      const pom = read(`${app.module}/pom.xml`, repoRoot);
      const moduleIndex = read(`${app.module}/src/main/resources/static/index.html`, repoRoot);

      assertIncludes(pom, [
        `<artifactId>${app.module}</artifactId>`,
        `<directory>../frontend/apps/${app.key}/dist-app</directory>`,
        "<targetPath>static/app</targetPath>"
      ], `${app.module} pom`);
      assert.match(moduleIndex, /app\/index\.html/);
    });
  }
});
