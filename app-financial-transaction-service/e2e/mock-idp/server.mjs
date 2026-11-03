// Mock OIDC identity provider for the automated e2e (Auth0 path layout). Standard library only, no dependencies.
//
// WHY it exists: @auth0/auth0-spa-js talks to fixed Auth0 paths (/authorize, /oauth/token, /v2/logout), so a generic mock server
// does not fit, and the browser tests must sign in without a password. The API validates the tokens against this issuer exactly
// as it does against Auth0 (discovery + JWKS, issuer, audience, roles claim).
//
// NEVER use it outside tests: anyone can "sign in" as any listed user.
import { createHash, generateKeyPairSync, randomBytes, sign } from 'node:crypto';
import { createServer } from 'node:http';

const PORT = Number(process.env.PORT ?? 9099);
const PUBLIC_URL = (process.env.PUBLIC_URL ?? `http://localhost:${PORT}`).replace(/\/+$/, '');
const ISSUER = `${PUBLIC_URL}/`; // Auth0 style: the trailing slash is part of the issuer
const ROLES_CLAIM = process.env.ROLES_CLAIM ?? 'https://financial-transaction/roles';
const ACCESS_TTL_SECONDS = 300;

/** The people who can sign in. `expired` hands out access tokens that are already expired (for the session-expired scenario). */
export const USERS = {
  'user-1': { sub: 'auth0|mock-user-1', name: 'Maria Silva', email: 'maria.silva@example.test', label: 'Maria Silva - demo customer (account 2022)' },
  'user-2': { sub: 'auth0|mock-user-2', name: 'João Santos', email: 'joao.santos@example.test', label: 'João Santos - second customer (account 3033)' },
  unlinked: { sub: 'auth0|mock-unlinked', name: 'Carla Souza', email: 'carla.souza@example.test', label: 'Carla Souza - valid sign-in, not linked to a customer' },
  expired: { sub: 'auth0|mock-user-1', name: 'Maria Silva', email: 'maria.silva@example.test', label: 'Maria Silva - access tokens already expired', expired: true },
};

const { privateKey, publicKey } = generateKeyPairSync('rsa', { modulusLength: 2048 });
const KID = `mock-${randomBytes(4).toString('hex')}`;
const jwk = { ...publicKey.export({ format: 'jwk' }), kid: KID, use: 'sig', alg: 'RS256' };

const b64url = (input) => Buffer.from(input).toString('base64url');

function signJwt(claims) {
  const header = b64url(JSON.stringify({ alg: 'RS256', typ: 'JWT', kid: KID }));
  const payload = b64url(JSON.stringify(claims));
  const signature = sign('RSA-SHA256', Buffer.from(`${header}.${payload}`), privateKey).toString('base64url');
  return `${header}.${payload}.${signature}`;
}

const codes = new Map(); // authorization code -> request context
const refreshTokens = new Map(); // refresh token -> { userKey, clientId, audience, scope }

function issueTokens({ userKey, clientId, audience, scope, nonce }) {
  const user = USERS[userKey];
  const now = Math.floor(Date.now() / 1000);
  const accessExp = user.expired ? now - 60 : now + ACCESS_TTL_SECONDS;
  const accessToken = signJwt({
    iss: ISSUER, sub: user.sub, aud: audience ? [audience] : [`${PUBLIC_URL}/userinfo`],
    iat: now - (user.expired ? 600 : 0), exp: accessExp, scope, [ROLES_CLAIM]: ['customer'], jti: randomBytes(8).toString('hex'),
  });
  const idToken = signJwt({
    iss: ISSUER, sub: user.sub, aud: clientId, iat: now, exp: now + 3600, auth_time: now,
    name: user.name, email: user.email, email_verified: true, picture: '', ...(nonce ? { nonce } : {}),
  });
  const body = { access_token: accessToken, id_token: idToken, token_type: 'Bearer', expires_in: user.expired ? 1 : ACCESS_TTL_SECONDS, scope };
  if (scope.includes('offline_access')) {
    const refresh = randomBytes(24).toString('base64url');
    refreshTokens.set(refresh, { userKey, clientId, audience, scope });
    body.refresh_token = refresh;
  }
  return body;
}

const cors = {
  'access-control-allow-origin': '*',
  'access-control-allow-headers': 'content-type, auth0-client, authorization',
  'access-control-allow-methods': 'GET, POST, OPTIONS',
};

function send(res, status, body, headers = {}) {
  const isJson = typeof body === 'object';
  res.writeHead(status, { ...cors, 'content-type': isJson ? 'application/json' : 'text/html; charset=utf-8', 'cache-control': 'no-store', ...headers });
  res.end(isJson ? JSON.stringify(body) : body);
}

const escapeHtml = (s) => String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);

function readBody(req) {
  return new Promise((resolve) => {
    let data = '';
    req.on('data', (chunk) => (data += chunk));
    req.on('end', () => resolve(data));
  });
}

function parseBody(contentType, raw) {
  if ((contentType ?? '').includes('application/json')) return JSON.parse(raw || '{}');
  return Object.fromEntries(new URLSearchParams(raw));
}

function loginPage(query) {
  const hidden = [...query.entries()].map(([k, v]) => `<input type="hidden" name="${escapeHtml(k)}" value="${escapeHtml(v)}">`).join('');
  const buttons = Object.entries(USERS).map(([key, u]) => `<button name="user" value="${key}" type="submit">${escapeHtml(u.label)}</button>`).join('');
  return `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Mock sign-in</title>
<style>body{font-family:system-ui,sans-serif;background:#0b1234;color:#f1f4ff;display:grid;place-items:center;min-height:100vh;margin:0}main{background:#121c47;border:1px solid #2a3770;border-radius:16px;padding:28px;max-width:420px;width:calc(100% - 32px)}
h1{font-size:20px;margin:0 0 4px}p{color:#9aa6d1;margin:0 0 18px;font-size:14px}button{display:block;width:100%;margin:8px 0;padding:14px;border-radius:12px;border:1px solid #6674b3;background:#0e1738;color:#f1f4ff;font-size:15px;text-align:left;cursor:pointer}button:hover,button:focus-visible{background:#2d68fa;outline:none}</style></head>
<body><main><h1>Mock identity provider</h1><p>Test environment only. Pick who signs in - no password is needed.</p>
<form method="get" action="/authorize/login">${hidden}${buttons}</form></main></body></html>`;
}

const server = createServer(async (req, res) => {
  const url = new URL(req.url ?? '/', PUBLIC_URL);
  try {
    if (req.method === 'OPTIONS') return send(res, 204, '');

    if (url.pathname === '/.well-known/openid-configuration') {
      return send(res, 200, {
        issuer: ISSUER, authorization_endpoint: `${PUBLIC_URL}/authorize`, token_endpoint: `${PUBLIC_URL}/oauth/token`,
        jwks_uri: `${PUBLIC_URL}/.well-known/jwks.json`, end_session_endpoint: `${PUBLIC_URL}/v2/logout`,
        response_types_supported: ['code'], subject_types_supported: ['public'], id_token_signing_alg_values_supported: ['RS256'],
      });
    }
    if (url.pathname === '/.well-known/jwks.json') return send(res, 200, { keys: [jwk] });

    if (url.pathname === '/authorize' && req.method === 'GET') return send(res, 200, loginPage(url.searchParams));

    if (url.pathname === '/authorize/login' && req.method === 'GET') {
      const q = url.searchParams;
      const userKey = q.get('user');
      const redirectUri = q.get('redirect_uri');
      if (!USERS[userKey] || !redirectUri) return send(res, 400, { error: 'invalid_request' });
      const code = randomBytes(16).toString('base64url');
      codes.set(code, {
        userKey, clientId: q.get('client_id') ?? '', redirectUri, audience: q.get('audience') ?? '', scope: q.get('scope') ?? 'openid',
        nonce: q.get('nonce') ?? '', challenge: q.get('code_challenge') ?? '', method: q.get('code_challenge_method') ?? 'S256',
      });
      const back = new URL(redirectUri);
      back.searchParams.set('code', code);
      if (q.get('state')) back.searchParams.set('state', q.get('state'));
      return send(res, 302, '', { location: back.toString() });
    }

    if (url.pathname === '/oauth/token' && req.method === 'POST') {
      const body = parseBody(req.headers['content-type'], await readBody(req));
      if (body.grant_type === 'authorization_code') {
        const ctx = codes.get(body.code);
        codes.delete(body.code);
        if (!ctx) return send(res, 400, { error: 'invalid_grant', error_description: 'Unknown or used code' });
        if (ctx.challenge) {
          const computed = createHash('sha256').update(String(body.code_verifier ?? '')).digest('base64url');
          if (computed !== ctx.challenge) return send(res, 400, { error: 'invalid_grant', error_description: 'PKCE verification failed' });
        }
        return send(res, 200, issueTokens(ctx));
      }
      if (body.grant_type === 'refresh_token') {
        const ctx = refreshTokens.get(body.refresh_token);
        refreshTokens.delete(body.refresh_token); // rotation
        if (!ctx) return send(res, 403, { error: 'invalid_grant', error_description: 'Unknown refresh token' });
        return send(res, 200, issueTokens({ ...ctx, nonce: '' }));
      }
      return send(res, 400, { error: 'unsupported_grant_type' });
    }

    if (url.pathname === '/v2/logout') {
      const returnTo = url.searchParams.get('returnTo');
      return returnTo ? send(res, 302, '', { location: returnTo }) : send(res, 200, '<p>Signed out.</p>');
    }

    if (url.pathname === '/health') return send(res, 200, { status: 'UP' });
    return send(res, 404, { error: 'not_found' });
  } catch (error) {
    return send(res, 500, { error: 'server_error', message: String(error?.message ?? error) });
  }
});

server.listen(PORT, '0.0.0.0', () => console.log(`mock idp listening on :${PORT}, issuer ${ISSUER}`));
