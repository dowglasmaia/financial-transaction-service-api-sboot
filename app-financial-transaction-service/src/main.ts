import { bootstrapApplication } from '@angular/platform-browser';
import { provideAuth0 } from '@auth0/auth0-angular';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { buildAuthConfig } from './app/core/auth/auth.config';
import { APP_CONFIG, loadAppConfig } from './app/core/config/app-config';

// Runtime configuration is loaded first so the same build runs in every environment (and the sign-in client is configured from it).
loadAppConfig()
  .then((config) =>
    bootstrapApplication(App, {
      ...appConfig,
      providers: [...appConfig.providers, { provide: APP_CONFIG, useValue: config }, provideAuth0(buildAuthConfig(config, window.location.origin))],
    }),
  )
  .catch((error: unknown) => console.error(error));
