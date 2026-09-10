# ---- build stage ----
FROM node:22-alpine AS build
WORKDIR /app

# install deps first (layer caching — only re-runs when package files change)
COPY package.json package-lock.json ./
RUN npm ci

# build the app
COPY . .
RUN npm run build

# ---- runtime stage ----
FROM nginx:1.27-alpine
# the built SPA
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
# our nginx config (serves SPA + proxies /api and /ws)
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
