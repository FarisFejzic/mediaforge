# ---- build stage ----
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- runtime stage ----
FROM nginx:1.27-alpine
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
# only substitute these two vars — leaves nginx's own $-vars intact
ENV NGINX_ENVSUBST_FILTER='NGINX_RESOLVER|API_UPSTREAM'
# defaults (k8s values) — overridden by Compose
ENV NGINX_RESOLVER='10.96.0.10'
ENV API_UPSTREAM='http://api.mediaforge.svc.cluster.local:8080'
EXPOSE 80
