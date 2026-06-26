FROM node:20-alpine AS build
WORKDIR /app
ARG VITE_SITE_URL=http://localhost:5173
ENV VITE_SITE_URL=$VITE_SITE_URL
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ .
RUN npm run build

FROM nginx:1.27-alpine
COPY deploy/frontend/nginx-spa.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
