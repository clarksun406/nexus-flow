import { existsSync, readdirSync, readFileSync, rmSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const frontendRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const appsRoot = path.join(frontendRoot, "apps");
const apps = ["checkout", "merchant", "ops", "admin"];

let removed = 0;

for (const app of apps) {
  const distRoot = path.join(appsRoot, app, "dist-app");
  const indexPath = path.join(distRoot, "index.html");
  const assetsRoot = path.join(distRoot, "assets");

  if (!existsSync(indexPath) || !existsSync(assetsRoot)) {
    throw new Error(`${app} dist-app is missing; run npm.cmd run build first`);
  }

  const indexHtml = readFileSync(indexPath, "utf8");
  const referencedAssets = new Set(
    [...indexHtml.matchAll(/\.\/assets\/([^"']+)/g)].map((match) => match[1])
  );

  if (referencedAssets.size === 0) {
    throw new Error(`${app} dist-app index.html does not reference any assets`);
  }

  for (const assetName of readdirSync(assetsRoot)) {
    if (!referencedAssets.has(assetName)) {
      rmSync(path.join(assetsRoot, assetName), { force: true });
      removed += 1;
    }
  }
}

console.log(`Pruned ${removed} stale frontend asset(s).`);
