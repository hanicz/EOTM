# EOTM (Eye On The Money)

A hobby project to track stock, ETF, forex and crypto investments.

## Overview

- **Backend**: Java 25 / Spring Boot, built with Maven (multi-module: `backend` + `frontend`).
- **Frontend**: Angular 22 UI (`frontend/src/main/ng`), built and bundled into the Spring Boot app's static resources.
- **Database**: PostgreSQL.
- **Cache**: Redis.
- **Deployment**: Docker Compose (app, db, nginx, redis) on a Hetzner Cloud instance.

## Data sources

The application requires the following API subscriptions/keys, configured in the database so the app knows where to look:

- [EODHD APIs](https://eodhd.com/) — live stock and ETF prices (requires an active subscription).
- [CoinGecko](https://www.coingecko.com/en/api) — crypto prices.
- [Finnhub](https://finnhub.io/) — fundamental data.

## Project structure

```
backend/   Spring Boot application (REST API, persistence, business logic)
frontend/  Spring Boot module that serves the Angular UI
  src/main/ng/  Angular source (PrimeNG + ApexCharts)
docker/    Dockerfile, docker-compose.yml, nginx config and deployment notes
```

## Building

```
mvn clean install
```

This builds the Angular app and packages it together with the backend into `backend/target/EOTM.jar`.

## Running locally

```
cd frontend/src/main/ng
npm install
npm start          # ng serve, for UI development
```

```
mvn -pl backend spring-boot:run
```

## Deployment

The app ships as a Docker image (`thanicz/eotm`) and is run via `docker/docker-compose.yml`, which wires up the app container together with PostgreSQL, Redis and an nginx reverse proxy (TLS via Let's Encrypt). See `docker/Hetzner.txt` for the current deployment notes.
