--Test users
INSERT INTO EOTM_USER (EMAIL, PASSWORD) VALUES ('test@test.test', 'test');

--Test currencies
INSERT INTO EOTM_CURRENCY (ID,NAME) VALUES ('EUR', 'euro'),
('USD', 'US dollar'),
('HUF', 'forint');

--Test Stocks
INSERT INTO EOTM_STOCK (ID,NAME,SHORT_NAME,EXCHANGE) VALUES ('crsr', 'Corsair Gaming Inc', 'CRSR', 'US');

--Test dividends
INSERT INTO EOTM_STOCK_DIVIDEND (USER_ID, STOCK_ID, CURRENCY_ID, AMOUNT, DIVIDEND_DATE) VALUES (1, 'crsr', 'HUF', 225, {ts '2021-06-03 00:00:00.00'});

--Test configs
INSERT INTO EOTM_CONFIG (CONFIG_KEY,CONFIG_VALUE) VALUES ('eod', 'https://eodhost.com');

--Test credentials
INSERT INTO EOTM_CREDENTIAL (NAME,SECRET) VALUES ('email_user', 'user'),
('email_password', 'password'),
('eod', 'token');

INSERT INTO EOTM_STOCK_ALERT (USER_ID, STOCK_ID, TYPE, VALUE_POINT) VALUES (1, 'crsr', 'PERCENT_OVER', 3.0),
(1, 'crsr', 'PERCENT_UNDER', -3.0),
(1, 'crsr', 'PRICE_UNDER', 34.5);