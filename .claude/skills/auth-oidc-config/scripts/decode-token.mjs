#!/usr/bin/env node
// Usage: node decode-token.mjs <inputs.json>   (access token on STDIN; never as an argument, never logged)
// Checks the CLAIMS of a real access token against the inputs. It does NOT verify the signature (the API does that).
import fs from 'node:fs';
import { readJson, validateInputs, issuerOf } from './lib.mjs';

export function checkClaims(input, payload, header, now = Math.floor(Date.now() / 1000)) {
  const audiences = [].concat(payload.aud ?? []);
  const roles = payload[input.roles.claim];
  return [
    [header?.alg === 'RS256', `header alg is RS256 (got ${header?.alg})`],
    [payload.iss === issuerOf(input), `iss equals ${issuerOf(input)} (got ${payload.iss})`],
    [audiences.includes(input.api.identifier), `aud contains ${input.api.identifier} (got ${JSON.stringify(audiences)})`],
    [!audiences.some((a) => /\/api\/v2\/?$/.test(a)), 'aud does not contain the Auth0 Management API'],
    [typeof payload.sub === 'string' && payload.sub.length > 0, `sub present (${payload.sub ?? 'missing'})`],
    [typeof payload.exp === 'number' && payload.exp > now, `token not expired (exp=${payload.exp})`],
    [payload.azp === input.spa.clientId || payload.azp === undefined, `azp is the SPA client (got ${payload.azp})`],
    [Array.isArray(roles) && roles.length > 0 && roles.every((r) => input.roles.allowed.includes(r)), `roles claim "${input.roles.claim}" is a non-empty array of allowed roles (got ${JSON.stringify(roles)})`],
  ];
}

export function decodeJwt(token) {
  const parts = token.trim().split('.');
  if (parts.length !== 3) throw new Error('Not a JWT (expected 3 dot-separated parts). An opaque token means the audience was not sent on /authorize.');
  const json = (part) => {
    try { return JSON.parse(Buffer.from(part, 'base64url').toString('utf8')); }
    catch { throw new Error('Token header/payload are not valid base64url JSON.'); }
  };
  return { header: json(parts[0]), payload: json(parts[1]) };
}

if (import.meta.url === `file://${process.argv[1].replace(/\\/g, '/')}` || process.argv[1]?.endsWith('decode-token.mjs')) {
  const file = process.argv[2];
  if (!file || process.argv.length !== 3) { console.error('Usage: node decode-token.mjs <inputs.json>  (token on stdin)'); process.exit(2); }
  const input = readJson(file, fs);
  if (validateInputs(input).errors.length) { console.error('Inputs invalid; run check-inputs first.'); process.exit(1); }
  let decoded;
  try { decoded = decodeJwt(fs.readFileSync(0, 'utf8')); } catch (e) { console.error(e.message); process.exit(1); }
  const results = checkClaims(input, decoded.payload, decoded.header);
  for (const [ok, message] of results) console.log(`${ok ? 'OK  ' : 'FAIL'} ${message}`);
  console.log('Note: signature not verified here.');
  process.exit(results.every(([ok]) => ok) ? 0 : 1);
}
