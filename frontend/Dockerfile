FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:22-alpine AS builder
WORKDIR /app
ENV AUTH_SECRET="build-time-secret"
ENV BACKEND_URL="http://backend:8080"
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV BACKEND_URL="http://backend:8080"
COPY --from=builder /app ./
EXPOSE 3000
CMD ["npm", "run", "start"]
