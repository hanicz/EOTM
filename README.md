This is a hobby project for myself to track my stock, etf, forex and crypto related invesments. The application is deployed on a DigitalOcean droplet. The docker file is included with the docker-compose yml.

The application to be able to run requires an active EODHD APIs subscription for live stock and etf prices. Crypto prices are collected from coingecko. Fundamental data is collected from finnhub. These api's need to be set in the underlying database as well so the application knows where to look.
