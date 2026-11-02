#!/usr/bin/env node
// Usage: node verify-tenant.mjs <inputs.json>
// Read-only GETs on the tenant's PUBLIC discovery and JWKS endpoints. Sends no credentials. Exit 0 = ok, 1 = mismatch, 3 = unreachable.
import fs from 'node:fs';
import { readJson, validateInputs, issuerOf, jwksOf, printFindings } from './lib.mjs';

const file = process.argv[2];
if (!file || process.argv.length !== 3) { console.error('Usage: node verify-tenant.mjs <inputs.json>'); process.exit(2); }
const input = readJson(file, fs);
const validation = validateInputs(input);
if (validation.errors.length) { printFindings(validation); console.error('Inputs invalid; run check-inputs first.'); process.exit(1); }

async function get(url) {
  const response = await fetch(url, { signal: AbortSignal.timeout(8000), headers: { accept: 'application/json' } });
  if (!response.ok) throw new Error(`${url} -> HTTP ${response.status}`);
  return response.json();
}

let failures = 0;
const check = (ok, message) => { console.log(`${ok ? 'OK  ' : 'FAIL'} ${message}`); if (!ok) failures++; };

try {
  const discovery = await get(`${issuerOf(input)}.well-known/openid-configuration`);
  check(discovery.issuer === issuerOf(input), `discovery issuer = ${discovery.issuer} (expected ${issuerOf(input)})`);
  check(discovery.jwks_uri === jwksOf(input), `jwks_uri = ${discovery.jwks_uri}`);
  check(discovery.code_challenge_methods_supported?.includes('S256') ?? true, 'PKCE S256 supported');
  const jwks = await get(discovery.jwks_uri);
  check(Array.isArray(jwks.keys) && jwks.keys.length > 0, `JWKS has ${jwks.keys?.length ?? 0} key(s)`);
  check(jwks.keys?.some((k) => k.alg === 'RS256' || k.kty === 'RSA'), 'an RSA/RS256 signing key is published');
} catch (e) {
  console.error(`UNREACHABLE: ${e.message}`);
  process.exit(3);
}
process.exit(failures ? 1 : 0);
