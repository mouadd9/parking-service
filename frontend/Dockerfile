FROM node:18-alpine as build
WORKDIR /app
COPY package*.json ./

# Nettoyer le cache npm + installation fiable
RUN npm cache clean --force \
 && rm -rf node_modules package-lock.json \
 && npm install --no-audit --no-fund

# 2. Copier le code source 
COPY . .

# 3. Build frontend
RUN npm run build

# Production stage with nginx
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html

# Create nginx config inline
RUN echo 'server { \
    listen 3000; \
    location / { \
        root /usr/share/nginx/html; \
        index index.html; \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 3000
CMD ["nginx", "-g", "daemon off;"]