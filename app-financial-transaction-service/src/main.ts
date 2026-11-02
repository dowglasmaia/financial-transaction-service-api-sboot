import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { APP_CONFIG, loadAppConfig } from './app/core/config/app-config';

// Runtime configuration is loaded first so the same build runs in every environment.
loadAppConfig()
  .then((config) => bootstrapApplication(App, { ...appConfig, providers: [...appConfig.providers, { provide: APP_CONFIG, useValue: config }] }))
  .catch((error: unknown) => console.error(error));
