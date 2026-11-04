# Frontend image: build the Angular app, serve it with nginx.
#   docker build -t maia-bank-web ./app-financial-transaction-service
#
# Runtime settings (nothing secret):
#   API_UPSTREAM  where nginx forwards /api/ (default: the compose service financial-transaction-api:8088)
#   /usr/share/nginx/html/config.json  the public Auth0 values the SPA reads at start. It ships with the build; mount your own
#   file over it (see docker-compose.yml) to point the same image at another tenant or at the e2e mock IdP.

FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginx:1.31-alpine
ENV API_UPSTREAM=http://financial-transaction-api:8088
# nginx:alpine renders /etc/nginx/templates/*.template with the environment at start (only the variables we name)
ENV NGINX_ENVSUBST_FILTER=API_
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/app-financial-transaction-service/browser /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
