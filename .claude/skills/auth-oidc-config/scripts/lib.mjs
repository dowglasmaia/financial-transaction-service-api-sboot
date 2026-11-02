// Shared, dependency-free helpers. Everything here is deterministic: same inputs -> same findings/outputs.

export const SUPPORTED_PROVIDERS = ['auth0'];
const SECRET_KEYS = /^(client_?secret|secret|password|passwd|token|access_?token|refresh_?token|api_?key|private_?key)$/i;
const RESERVED_NAMESPACES = /(^|\.)(auth0\.com|webtask\.io|webtask\.run)$/i;

export function isLocalHost(host) {
  return host === 'localhost' || host === '127.0.0.1' || host === '[::1]' || host.endsWith('.localhost');
}

function parseUrl(value) {
  try { return new URL(value); } catch { return undefined; }
}

function findSecretKeys(node, path = '') {
  const found = [];
  if (node && typeof node === 'object') {
    for (const [key, value] of Object.entries(node)) {
      const here = path ? `${path}.${key}` : key;
      if (SECRET_KEYS.test(key)) found.push(here);
      found.push(...findSecretKeys(value, here));
    }
  }
  return found;
}

/** Returns { errors: [{code,path,message}], warnings: [...] }. Pure function. */
export function validateInputs(input) {
  const errors = [];
  const warnings = [];
  const err = (code, path, message) => errors.push({ code, path, message });
  const warn = (code, path, message) => warnings.push({ code, path, message });

  if (!input || typeof input !== 'object') {
    err('E000', '$', 'Inputs must be a JSON object (copy examples/inputs.template.json).');
    return { errors, warnings };
  }

  for (const path of findSecretKeys(input)) {
    err('E001', path, 'Secrets must never be in this file (client secret, password, token). The SPA has no secret; remove the key.');
  }

  if (!SUPPORTED_PROVIDERS.includes(input.provider)) {
    err('E002', 'provider', `Unsupported provider "${input.provider}". Supported: ${SUPPORTED_PROVIDERS.join(', ')}.`);
  }

  const domain = input.tenantDomain;
  if (typeof domain !== 'string' || !domain) {
    err('E010', 'tenantDomain', 'Required (e.g. dev-abc123.eu.auth0.com), host only.');
  } else if (/^[a-z]+:\/\//i.test(domain) || domain.includes('/')) {
    err('E011', 'tenantDomain', 'Host only: no scheme, no path, no trailing slash (the issuer is derived from it).');
  } else if (!/^[a-z0-9]([a-z0-9.-]*[a-z0-9])?\.[a-z]{2,}$/i.test(domain)) {
    err('E012', 'tenantDomain', 'Not a valid host name.');
  } else if (!/\.auth0\.com$/i.test(domain)) {
    warn('W010', 'tenantDomain', 'Not an *.auth0.com host: valid only if this is the tenant custom domain. The issuer must equal the "iss" claim exactly.');
  }

  const spa = input.spa ?? {};
  if (spa.applicationType !== 'spa') {
    err('E020', 'spa.applicationType', `Must be "spa" (Single Page Application). "${spa.applicationType ?? ''}" implies a client secret that a browser cannot keep. Change the type in Applications > Settings.`);
  }
  if (typeof spa.clientId !== 'string' || !/^[A-Za-z0-9]{20,}$/.test(spa.clientId)) {
    err('E021', 'spa.clientId', 'Client ID missing or malformed (letters/digits, usually 32 characters). It is public.');
  }
  for (const key of ['callbackUrls', 'logoutUrls', 'webOrigins']) {
    const list = spa[key];
    if (!Array.isArray(list) || list.length === 0) { err('E022', `spa.${key}`, 'Non-empty list required.'); continue; }
    list.forEach((value, i) => {
      const url = parseUrl(value);
      const path = `spa.${key}[${i}]`;
      if (!url) return err('E023', path, `"${value}" is not an absolute URL.`);
      if (value.includes('*')) err('E024', path, 'Wildcards are not allowed; list exact URLs.');
      if (url.protocol === 'http:' && !isLocalHost(url.hostname)) err('E025', path, 'http is allowed only for localhost; use https.');
      if (key === 'webOrigins' && (url.pathname !== '/' || value.endsWith('/'))) err('E026', path, 'An origin has no path and no trailing slash (e.g. http://localhost:4200).');
    });
  }

  const api = input.api ?? {};
  const identifier = api.identifier;
  if (typeof identifier !== 'string' || !parseUrl(identifier)) {
    err('E030', 'api.identifier', 'Required: a logical URL (e.g. https://api.my-product.local). It becomes the token "aud" and cannot be changed after the API is created.');
  } else {
    if (typeof domain === 'string' && /\/api\/v2\/?$/.test(identifier)) {
      err('E031', 'api.identifier', 'This is the built-in Auth0 Management API audience. Never use it for your own API: create a new API under Applications > APIs.');
    }
    if (typeof domain === 'string' && identifier.toLowerCase().includes(`${domain.toLowerCase()}/userinfo`)) {
      err('E032', 'api.identifier', 'The /userinfo URL is not an API audience.');
    }
    if (identifier.endsWith('/')) warn('W030', 'api.identifier', 'Trailing slash: "aud" must then match WITH the slash everywhere (backend and SPA). Prefer no trailing slash.');
  }
  if (api.signingAlg !== 'RS256') err('E033', 'api.signingAlg', 'Must be RS256 (asymmetric; the API validates via JWKS without any secret).');

  const roles = input.roles ?? {};
  const claim = parseUrl(roles.claim ?? '');
  if (!claim || !/^https?:$/.test(claim.protocol)) {
    err('E040', 'roles.claim', 'Required: a namespaced claim name that is a URL (e.g. https://my-product/roles). Auth0 drops un-namespaced custom claims.');
  } else if (RESERVED_NAMESPACES.test(claim.hostname)) {
    err('E041', 'roles.claim', 'Namespaces on auth0.com / webtask.io / webtask.run are reserved by Auth0.');
  }
  if (!Array.isArray(roles.allowed) || roles.allowed.length === 0) err('E042', 'roles.allowed', 'List at least one role.');
  else if (!roles.allowed.includes(roles.defaultRole)) err('E043', 'roles.defaultRole', 'defaultRole must be one of roles.allowed.');

  const cors = input.backend?.corsOrigins;
  if (!Array.isArray(cors) || cors.length === 0) err('E050', 'backend.corsOrigins', 'Non-empty list of SPA origins required.');
  else cors.forEach((origin, i) => {
    const url = parseUrl(origin);
    if (!url || origin.includes('*') || origin.endsWith('/') || url.pathname !== '/') err('E051', `backend.corsOrigins[${i}]`, 'Exact origin only: scheme://host[:port], no path, no slash, no wildcard.');
  });
  if (!parseUrl(input.frontend?.apiBaseUrl ?? '')) err('E052', 'frontend.apiBaseUrl', 'Absolute URL of the API required.');

  (input.testUsers ?? []).forEach((user, i) => {
    if (typeof user.sub !== 'string' || !/^[A-Za-z0-9-]+\|[A-Za-z0-9._-]+$/.test(user.sub)) {
      err('E060', `testUsers[${i}].sub`, 'Expected the Auth0 user_id, e.g. auth0|65f0c0ffee0123456789abcd.');
    }
  });
  if (!input.testUsers?.length) warn('W060', 'testUsers', 'No test users yet: needed later to link users to accounts (a second one to test access denial).');

  return { errors, warnings };
}

export function issuerOf(input) { return `https://${input.tenantDomain}/`; }
export function jwksOf(input) { return `${issuerOf(input)}.well-known/jwks.json`; }

export function readJson(path, fs) {
  try { return JSON.parse(fs.readFileSync(path, 'utf8')); }
  catch (e) { console.error(`Cannot read JSON ${path}: ${e.message}`); process.exit(2); }
}

export function printFindings({ errors, warnings }) {
  for (const w of warnings) console.warn(`WARN  ${w.code} ${w.path}: ${w.message}`);
  for (const e of errors) console.error(`ERROR ${e.code} ${e.path}: ${e.message}`);
}
