import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { validateInputs, issuerOf } from '../lib.mjs';
import { OUTPUT_FILES } from '../render.mjs';
import { checkClaims, decodeJwt } from '../decode-token.mjs';

const here = path.dirname(fileURLToPath(import.meta.url));
const good = () => JSON.parse(fs.readFileSync(path.join(here, '../../examples/financial-transaction.inputs.json'), 'utf8'));
const codes = (input) => validateInputs(input).errors.map((e) => e.code);

test('the project example is valid (only the missing-test-users warning)', () => {
  const { errors, warnings } = validateInputs(good());
  assert.deepEqual(errors, []);
  assert.deepEqual(warnings.map((w) => w.code), ['W060']);
});

test('the empty template placeholders are rejected', () => {
  const template = JSON.parse(fs.readFileSync(path.join(here, '../../examples/inputs.template.json'), 'utf8'));
  assert.ok(codes(template).length > 0);
});

test('regular web application type is rejected', () => {
  const i = good(); i.spa.applicationType = 'regular_web';
  assert.ok(codes(i).includes('E020'));
});

test('the Management API audience is rejected', () => {
  const i = good(); i.api.identifier = `https://${i.tenantDomain}/api/v2/`;
  assert.ok(codes(i).includes('E031'));
});

test('domain with scheme or slash is rejected', () => {
  const i = good(); i.tenantDomain = `https://${i.tenantDomain}/`;
  assert.ok(codes(i).includes('E011'));
});

test('secret-like keys anywhere are rejected', () => {
  const i = good(); i.spa.clientSecret = 'x';
  assert.ok(codes(i).includes('E001'));
});

test('namespaced claim must be a URL outside reserved namespaces', () => {
  let i = good(); i.roles.claim = 'roles';
  assert.ok(codes(i).includes('E040'));
  i = good(); i.roles.claim = 'https://tenant.auth0.com/roles';
  assert.ok(codes(i).includes('E041'));
});

test('http is allowed only for localhost; wildcards and origin paths are rejected', () => {
  let i = good(); i.spa.callbackUrls = ['http://example.com'];
  assert.ok(codes(i).includes('E025'));
  i = good(); i.spa.callbackUrls = ['https://*.example.com'];
  assert.ok(codes(i).includes('E024'));
  i = good(); i.spa.webOrigins = ['http://localhost:4200/'];
  assert.ok(codes(i).includes('E026'));
  i = good(); i.backend.corsOrigins = ['*'];
  assert.ok(codes(i).includes('E051'));
});

test('defaultRole must be allowed; RS256 only; sub format', () => {
  let i = good(); i.roles.defaultRole = 'admin';
  assert.ok(codes(i).includes('E043'));
  i = good(); i.api.signingAlg = 'HS256';
  assert.ok(codes(i).includes('E033'));
  i = good(); i.testUsers = [{ label: 'a', sub: 'not-a-sub' }];
  assert.ok(codes(i).includes('E060'));
});

test('generation is deterministic and carries the exact values', () => {
  const i = good();
  const a = OUTPUT_FILES(i); const b = OUTPUT_FILES(i);
  assert.deepEqual(a, b);
  assert.ok(a['backend/application-auth.yaml'].includes('https://dev-twljocmfwpvlxx1q.eu.auth0.com/'));
  assert.ok(a['backend/auth.env.example'].includes('AUTH_AUDIENCE=https://api.financial-transaction.local'));
  assert.equal(JSON.parse(a['frontend/config.json']).auth.clientId, i.spa.clientId);
  assert.ok(a['portal/auth0-post-login-roles.js'].includes("'https://financial-transaction/roles'"));
  assert.ok(a['portal/portal-checklist.md'].includes('Single Page Application'));
  assert.ok(!JSON.stringify(a).toLowerCase().includes('secret": "'));
});

test('token claim checks pass for a good token and fail for wrong audience / missing role / opaque token', () => {
  const i = good(); const now = 1_000;
  const payload = { iss: issuerOf(i), aud: [i.api.identifier], sub: 'auth0|1', exp: now + 60, azp: i.spa.clientId, [i.roles.claim]: ['customer'] };
  const header = { alg: 'RS256' };
  assert.ok(checkClaims(i, payload, header, now).every(([ok]) => ok));
  assert.ok(checkClaims(i, { ...payload, aud: [`https://${i.tenantDomain}/api/v2/`] }, header, now).some(([ok]) => !ok));
  assert.ok(checkClaims(i, { ...payload, [i.roles.claim]: undefined }, header, now).some(([ok]) => !ok));
  assert.ok(checkClaims(i, { ...payload, iss: `https://${i.tenantDomain}` }, header, now).some(([ok]) => !ok));
  assert.throws(() => decodeJwt('opaque-token-without-dots'), /opaque/);
});
