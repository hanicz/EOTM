# EOTM (Eye On The Money)

A hobby project to track stock, ETF, forex, crypto and interest-bearing investments in one place, and to
plan what they add up to.

## Features

### Portfolio tracking

- **Stocks, ETFs, crypto, forex and securities** — record buys and sells per asset class, with holdings,
  open positions and full transaction history. Stock investments can be split across named accounts.
- **Live valuation** — holdings are priced against live market data and shown next to what was paid, with the
  gain or loss on each position.
- **Dividends and interest** — stock and ETF dividends, and interest credited on securities. Securities have
  no live market price, so they are valued at what was paid for them. Interest is recorded but deliberately
  left out of that valuation: it gets reinvested by buying more, and those purchases are already counted, so
  adding the interest as well would count the same money twice.
- **Multi-currency** — every amount is held in the currency it was traded in and converted on the fly, so a
  portfolio spread across HUF, EUR and USD still totals up.

### Dashboard

- Net worth across all asset classes in a currency of your choosing, with the change against what was spent.
- Allocation by asset class, and the currently active alerts.

### Watchlist and lookup

- **Watchlists** for stocks, crypto and currency pairs, with live prices.
- **Lookup** — search any ticker for its profile, fundamentals, price history and analyst recommendations.
- **Signals** — a buy/hold/sell view built from SMA, EMA, RSI and MACD, each indicator shown with its own
  reading rather than just the verdict.

### Alerts

- Price and percentage-change alerts on stocks and crypto (`PRICE_OVER`, `PRICE_UNDER`, `PERCENT_OVER`,
  `PERCENT_UNDER`), checked every five minutes in the background and delivered by email.

### News

- Market and company news, plus posts from a configurable list of subreddits.

### Tax (Hungary)

- Works out **szja** and **szocho** on RSUs: each grant is valued at the closing price on its date and
  converted to forint at that day's official MNB rate, falling back to the last published rate for weekends
  and holidays.
- The same calculation is available for a plain forint amount, and the report exports to CSV.

### FIRE (financial independence / early retirement)

- Projects your portfolio forward from what you actually hold, plus any cash or other assets not tracked
  here, at **1, 3, 5, 10, 15 and 20 years** — and at the year you reach your target.
- Every assumption is editable: monthly contribution and how fast it rises, expected return, inflation,
  withdrawal rate, current age, retirement age and how long the money has to last.
- **FIRE number** either derived from the annual spending you want (`spending ÷ withdrawal rate`) or typed in
  directly. A typed target is taken at face value; a derived one is measured in today's money, since that is
  the money the spending behind it was quoted in.
- **Drawdown** — past retirement, contributions stop and an inflation-linked income is drawn from the pot,
  which stays invested. Reports whether the money lasts or the age it runs out.
- **Pension** — a monthly amount from a chosen age. It meets your spending first, so only the shortfall comes
  out of the pot; anything left over goes into it.
- Every year is shown in both nominal and today's money, with a growth chart and CSV export.

### Data in and out

- **CSV import and export** on every transaction type, so records can be moved in bulk.
- **Full account export** to JSON from the settings page: every holding, dividend, watchlist, alert and
  preference in one document, read straight from the database so it cannot fail on an expired API key.

## Overview

- **Backend**: Java 25 / Spring Boot, built with Maven (multi-module: `backend` + `frontend`).
- **Frontend**: Angular 22 UI (`frontend/src/main/ng`), built and bundled into the Spring Boot app's static resources.
- **Database**: PostgreSQL.
- **Cache**: Redis.
- **Deployment**: Docker Compose (app, db, nginx, redis) on a Hetzner Cloud instance.

## Data sources

The application requires the following API subscriptions/keys, configured in the database so the app knows where to look:

- [EODHD APIs](https://eodhd.com/) — live stock, ETF and forex prices, and historical quotes (requires an active subscription).
- [CoinGecko](https://www.coingecko.com/en/api) — crypto prices.
- [Finnhub](https://finnhub.io/) — company profiles, fundamentals, analyst recommendations, market and company news.
- [Reddit](https://www.reddit.com/dev/api/) — hot posts from the subreddits configured in settings (OAuth client credentials).
- [MNB](https://www.mnb.hu/arfolyamok.asmx?wsdl) — official HUF exchange rates for the tax report (SOAP, no key required). The service answers over plain http only; a POST to the https host returns 404.

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

## Testing

```
mvn verify         # backend unit tests, the same command CI runs
```

## Deployment

The app ships as a Docker image (`thanicz/eotm`) and is run via `docker/docker-compose.yml`, which wires up the app container together with PostgreSQL, Redis and an nginx reverse proxy (TLS via Let's Encrypt). See `docker/Hetzner.txt` for the current deployment notes.
