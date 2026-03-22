FROM node:20-alpine

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm install

COPY . .

EXPOSE 5555

CMD ["sh", "-c", "npm run dev -- --port ${FRONTEND_PORT:-5555}"]
