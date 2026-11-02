import { DEFAULT_APP_CONFIG, parseAppConfig } from './app-config';

describe('parseAppConfig', () => {
  it('uses the defaults when the file is empty', () => {
    expect(parseAppConfig({})).toEqual(DEFAULT_APP_CONFIG);
  });

  it('keeps valid values and fills the rest with defaults', () => {
    const config = parseAppConfig({ apiBaseUrl: 'https://api.example.test/api', auth: { enabled: true, clientId: 'abc' } });
    expect(config.apiBaseUrl).toBe('https://api.example.test/api');
    expect(config.auth.enabled).toBe(true);
    expect(config.auth.clientId).toBe('abc');
    expect(config.auth.scopes).toEqual(DEFAULT_APP_CONFIG.auth.scopes);
    expect(config.demoBanner).toBe(true);
  });

  it('drops a trailing slash from the API base URL', () => {
    expect(parseAppConfig({ apiBaseUrl: '/api/' }).apiBaseUrl).toBe('/api');
  });

  it('ignores unknown fields and wrong types', () => {
    const config = parseAppConfig({ apiBaseUrl: 42, auth: 'yes', unknown: 1 } as unknown);
    expect(config).toEqual(DEFAULT_APP_CONFIG);
  });

  it('is safe with null or non-object input', () => {
    expect(parseAppConfig(null)).toEqual(DEFAULT_APP_CONFIG);
    expect(parseAppConfig('x')).toEqual(DEFAULT_APP_CONFIG);
  });
});
