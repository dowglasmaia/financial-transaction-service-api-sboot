#!/usr/bin/env node
// Usage: node check-inputs.mjs <inputs.json>   Exit 0 = valid (warnings allowed), 1 = invalid, 2 = unreadable.
import fs from 'node:fs';
import { readJson, validateInputs, printFindings } from './lib.mjs';

const file = process.argv[2];
if (!file || process.argv.length !== 3) { console.error('Usage: node check-inputs.mjs <inputs.json>'); process.exit(2); }
const result = validateInputs(readJson(file, fs));
printFindings(result);
console.log(result.errors.length ? `INVALID: ${result.errors.length} error(s), ${result.warnings.length} warning(s).` : `VALID: 0 errors, ${result.warnings.length} warning(s).`);
process.exit(result.errors.length ? 1 : 0);
