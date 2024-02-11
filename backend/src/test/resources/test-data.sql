--Test users
INSERT INTO EOTM_USER (EMAIL, PASSWORD) VALUES ('test@test.test', 'test');

--Test currencies
INSERT INTO EOTM_CURRENCY (ID,NAME) VALUES ('EUR', 'euro'),
('USD', 'US dollar'),
('HUF', 'forint');

--Test Stocks
INSERT INTO EOTM_STOCK (ID,NAME,SHORT_NAME,EXCHANGE) VALUES ('crsr', 'Corsair Gaming Inc', 'CRSR', 'US'),
('intc', 'Intel Corporation', 'INTC', 'US'),
('amd', 'Advanced Micro Devices Inc', 'AMD', 'US');

--Test coins
INSERT INTO EOTM_COIN (ID,NAME,SYMBOL) VALUES ('polkadot', 'Polkadot', 'DOT')
,('bitcoin', 'Bitcoin', 'BTC')
,('cardano', 'Cardano', 'ADA');

--Test coin payments
INSERT INTO EOTM_COIN_PAYMENT (AMOUNT,CURRENCY_ID) VALUES (156.80, 'EUR'),
(100, 'EUR'),
(200.44, 'EUR'),
(10.87, 'EUR'),
(1000.87, 'EUR'),
(2032.11, 'EUR');

--Test coin transactions
INSERT INTO EOTM_COIN_TRANSACTION (BUY_SELL,CREATION_DATE,QUANTITY,TRANSACTION_STRING,COIN_ID,PAYMENT_ID,USER_ID,TRANSACTION_DATE,FEE) VALUES
('B',CURRENT_TIMESTAMP(),4.98,'ttt','polkadot',1,1,{ts '2021-05-20 05:00:00.00'},3.2),
('B',CURRENT_TIMESTAMP(),98.5,'ttt','bitcoin',2,1,{ts '2021-05-07 05:00:00.00'},0),
('S',CURRENT_TIMESTAMP(),4.0,'ttt','polkadot',3,1,{ts '2021-05-20 05:00:00.00'},3.2),
('S',CURRENT_TIMESTAMP(),11.1,'ttt','bitcoin',4,1,{ts '2021-05-20 05:00:00.00'},3.2),
('B',CURRENT_TIMESTAMP(),100.23,'ttt','cardano',5,1,{ts '2021-05-07 05:00:00.00'},0),
('S',CURRENT_TIMESTAMP(),100.23,'ttt','cardano',6,1,{ts '2021-05-20 05:00:00.00'},0);

--Test stock Payments
INSERT INTO EOTM_STOCK_PAYMENT (AMOUNT,CURRENCY_ID) VALUES (100.60, 'USD'),
(200.17, 'USD'),
(310.3, 'USD'),
(200.60, 'USD'),
(156.4, 'USD'),
(500.2, 'USD');

--Test stock Investments
INSERT INTO EOTM_STOCK_INVESTMENT (BUY_SELL,CREATION_DATE,QUANTITY,STOCK_ID,STOCK_PAYMENT_ID,USER_ID,TRANSACTION_DATE,FEE) VALUES
('B',CURRENT_TIMESTAMP(),10,'crsr',1,1,{ts '2023-05-05 16:04:25.00'},0.6),
('B',CURRENT_TIMESTAMP(),7,'intc',2,1,{ts '2023-09-08 16:04:25.00'},2),
('B',CURRENT_TIMESTAMP(),136,'amd',3,1,{ts '2023-09-08 16:04:25.00'}, 199),
('S',CURRENT_TIMESTAMP(),10,'crsr',4,1,{ts '2023-07-05 16:04:25.00'},0.6),
('S',CURRENT_TIMESTAMP(),5,'intc',5,1,{ts '2023-12-08 16:04:25.00'},2),
('S',CURRENT_TIMESTAMP(),100,'amd',6,1,{ts '2023-09-08 16:04:25.00'}, 199);

--Test dividends
INSERT INTO EOTM_STOCK_DIVIDEND (USER_ID, STOCK_ID, CURRENCY_ID, AMOUNT, DIVIDEND_DATE) VALUES (1, 'crsr', 'HUF', 225, {ts '2021-06-03 00:00:00.00'});
INSERT INTO EOTM_STOCK_DIVIDEND (USER_ID, STOCK_ID, CURRENCY_ID, AMOUNT, DIVIDEND_DATE) VALUES (1, 'crsr', 'HUF', 225, {ts '2021-08-03 00:00:00.00'});

--Test configs
INSERT INTO EOTM_CONFIG (CONFIG_KEY,CONFIG_VALUE) VALUES ('eod', 'https://eodhost.com');
INSERT INTO EOTM_CONFIG (CONFIG_KEY,CONFIG_VALUE) VALUES ('finnhub', 'https://finnhubhost.com');

--Test credentials
INSERT INTO EOTM_CREDENTIAL (NAME,SECRET) VALUES ('email_user', 'user'),
('email_password', 'password'),
('eod', 'token'),
('finnhub', 'token');

--Test coin watch
INSERT INTO EOTM_COIN_WATCH (USER_ID, COIN_ID) VALUES
(1, 'cardano'),
(1, 'bitcoin');
