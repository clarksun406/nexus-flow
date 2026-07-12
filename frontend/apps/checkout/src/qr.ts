export function drawQrToCanvas(text: string, canvas: HTMLCanvasElement) {
  const matrix = createQrMatrix(text);
  const context = canvas.getContext("2d");
  if (!context) {
    throw new Error("Canvas is unavailable");
  }
  const quiet = 4;
  const scale = Math.floor(canvas.width / (matrix.length + quiet * 2));
  const size = matrix.length * scale + quiet * 2 * scale;
  const offset = Math.floor((canvas.width - size) / 2);
  context.fillStyle = "#ffffff";
  context.fillRect(0, 0, canvas.width, canvas.height);
  context.fillStyle = "#111827";
  matrix.forEach((row, y) => {
    row.forEach((dark, x) => {
      if (dark) {
        context.fillRect(offset + (x + quiet) * scale, offset + (y + quiet) * scale, scale, scale);
      }
    });
  });
}

function createQrMatrix(text: string) {
  const version = 5;
  const size = 21 + (version - 1) * 4;
  const dataCodewords = 108;
  const eccCodewords = 26;
  const mask = 0;
  const bytes = Array.from(new TextEncoder().encode(text));
  if (bytes.length > 106) {
    throw new Error("QR payload too long");
  }

  const data = makeDataCodewords(bytes, dataCodewords);
  const ecc = reedSolomonRemainder(data, reedSolomonDivisor(eccCodewords));
  const codewords = data.concat(ecc);
  const modules = Array.from({ length: size }, () => Array<boolean>(size).fill(false));
  const reserved = Array.from({ length: size }, () => Array<boolean>(size).fill(false));

  const set = (row: number, col: number, dark: boolean) => {
    if (row < 0 || row >= size || col < 0 || col >= size) {
      return;
    }
    modules[row][col] = dark;
    reserved[row][col] = true;
  };

  drawFinder(set, 0, 0);
  drawFinder(set, 0, size - 7);
  drawFinder(set, size - 7, 0);
  for (let i = 8; i < size - 8; i += 1) {
    set(6, i, i % 2 === 0);
    set(i, 6, i % 2 === 0);
  }
  drawAlignment(set, 30, 30);
  drawFormatBits(set, size, mask);

  let bitIndex = 0;
  for (let right = size - 1; right >= 1; right -= 2) {
    if (right === 6) {
      right = 5;
    }
    for (let vert = 0; vert < size; vert += 1) {
      const row = (((right + 1) & 2) === 0) ? size - 1 - vert : vert;
      for (let j = 0; j < 2; j += 1) {
        const col = right - j;
        if (!reserved[row][col]) {
          const bit = bitIndex < codewords.length * 8
            ? ((codewords[bitIndex >>> 3] >>> (7 - (bitIndex & 7))) & 1) === 1
            : false;
          modules[row][col] = bit !== ((row + col) % 2 === 0);
          bitIndex += 1;
        }
      }
    }
  }
  return modules;
}

function makeDataCodewords(bytes: number[], dataCodewords: number) {
  const bits: number[] = [];
  appendBits(bits, 0x4, 4);
  appendBits(bits, bytes.length, 8);
  bytes.forEach((value) => appendBits(bits, value, 8));
  const capacity = dataCodewords * 8;
  appendBits(bits, 0, Math.min(4, capacity - bits.length));
  while (bits.length % 8 !== 0) {
    bits.push(0);
  }
  const data: number[] = [];
  for (let i = 0; i < bits.length; i += 8) {
    let value = 0;
    for (let j = 0; j < 8; j += 1) {
      value = (value << 1) | bits[i + j];
    }
    data.push(value);
  }
  for (let i = 0; data.length < dataCodewords; i += 1) {
    data.push(i % 2 === 0 ? 0xec : 0x11);
  }
  return data;
}

function appendBits(bits: number[], value: number, length: number) {
  for (let i = length - 1; i >= 0; i -= 1) {
    bits.push((value >>> i) & 1);
  }
}

function drawFinder(set: (row: number, col: number, dark: boolean) => void, row: number, col: number) {
  for (let y = -1; y <= 7; y += 1) {
    for (let x = -1; x <= 7; x += 1) {
      const distance = Math.max(Math.abs(x - 3), Math.abs(y - 3));
      set(row + y, col + x, distance !== 2 && distance !== 4);
    }
  }
}

function drawAlignment(set: (row: number, col: number, dark: boolean) => void, row: number, col: number) {
  for (let y = -2; y <= 2; y += 1) {
    for (let x = -2; x <= 2; x += 1) {
      set(row + y, col + x, Math.max(Math.abs(x), Math.abs(y)) !== 1);
    }
  }
}

function drawFormatBits(set: (row: number, col: number, dark: boolean) => void, size: number, mask: number) {
  const bits = formatBits(mask);
  const bit = (i: number) => ((bits >>> i) & 1) === 1;
  for (let i = 0; i <= 5; i += 1) {
    set(i, 8, bit(i));
  }
  set(7, 8, bit(6));
  set(8, 8, bit(7));
  set(8, 7, bit(8));
  for (let i = 9; i < 15; i += 1) {
    set(8, 14 - i, bit(i));
  }
  for (let i = 0; i < 8; i += 1) {
    set(8, size - 1 - i, bit(i));
  }
  for (let i = 8; i < 15; i += 1) {
    set(size - 15 + i, 8, bit(i));
  }
  set(size - 8, 8, true);
}

function formatBits(mask: number) {
  const eclLow = 1;
  const data = (eclLow << 3) | mask;
  let rem = data << 10;
  const generator = 0x537;
  for (let i = 14; i >= 10; i -= 1) {
    if (((rem >>> i) & 1) !== 0) {
      rem ^= generator << (i - 10);
    }
  }
  return ((data << 10) | rem) ^ 0x5412;
}

function reedSolomonDivisor(degree: number) {
  const result = Array<number>(degree).fill(0);
  result[degree - 1] = 1;
  let root = 1;
  for (let i = 0; i < degree; i += 1) {
    for (let j = 0; j < degree; j += 1) {
      result[j] = gfMultiply(result[j], root);
      if (j + 1 < degree) {
        result[j] ^= result[j + 1];
      }
    }
    root = gfMultiply(root, 2);
  }
  return result;
}

function reedSolomonRemainder(data: number[], divisor: number[]) {
  const result = Array<number>(divisor.length).fill(0);
  data.forEach((value) => {
    const factor = value ^ result[0];
    result.shift();
    result.push(0);
    divisor.forEach((coefficient, index) => {
      result[index] ^= gfMultiply(coefficient, factor);
    });
  });
  return result;
}

const gfTables = (() => {
  const exp = Array<number>(512).fill(0);
  const log = Array<number>(256).fill(0);
  let value = 1;
  for (let i = 0; i < 255; i += 1) {
    exp[i] = value;
    log[value] = i;
    value <<= 1;
    if (value & 0x100) {
      value ^= 0x11d;
    }
  }
  for (let i = 255; i < 512; i += 1) {
    exp[i] = exp[i - 255];
  }
  return { exp, log };
})();

function gfMultiply(left: number, right: number) {
  if (left === 0 || right === 0) {
    return 0;
  }
  return gfTables.exp[gfTables.log[left] + gfTables.log[right]];
}
