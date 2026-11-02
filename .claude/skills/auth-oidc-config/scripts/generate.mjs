#!/usr/bin/env node
// Usage: node generate.mjs <inputs.json> --out <dir> [--force]
// Validates first (refuses invalid inputs), then writes the artifacts. Never overwrites without --force.
import fs from 'node:fs';
import path from 'node:path';
import { readJson, validateInputs, printFindings } from './lib.mjs';
import { OUTPUT_FILES } from './render.mjs';

const args = process.argv.slice(2);
const force = args.includes('--force');
const outIndex = args.indexOf('--out');
const file = args.find((a, i) => !a.startsWith('--') && args[i - 1] !== '--out');
if (!file || outIndex < 0 || !args[outIndex + 1]) { console.error('Usage: node generate.mjs <inputs.json> --out <dir> [--force]'); process.exit(2); }

const input = readJson(file, fs);
const result = validateInputs(input);
printFindings(result);
if (result.errors.length) { console.error('Refusing to generate: fix the errors above.'); process.exit(1); }

const out = path.resolve(args[outIndex + 1]);
const files = OUTPUT_FILES(input);
const existing = Object.keys(files).filter((rel) => fs.existsSync(path.join(out, rel)));
if (existing.length && !force) { console.error(`Refusing to overwrite: ${existing.join(', ')} (use --force).`); process.exit(1); }

for (const [rel, content] of Object.entries(files)) {
  const target = path.join(out, rel);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.writeFileSync(target, content, 'utf8');
  console.log(`wrote ${rel}`);
}
