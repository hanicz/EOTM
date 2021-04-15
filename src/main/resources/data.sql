
--Users
INSERT INTO USER (ID,EMAIL, PASSWORD) VALUES (1, 'teste', '$2a$10$B85LrKjKCdRASw7XVxngcep/rp6RwIZ5uvlp9BEnWB0fZUhAmc94G');

--Currencies
INSERT INTO CURRENCY (ID,NAME) VALUES ('eur', 'EURO');
INSERT INTO CURRENCY (ID,NAME) VALUES ('usd', 'DOLLAR');
INSERT INTO CURRENCY (ID,NAME) VALUES ('huf', 'FORINT');

--Coins
INSERT INTO COIN (ID,NAME,SYMBOL) VALUES ('polkadot', 'Polkadot', 'dot');
INSERT INTO COIN (ID,NAME,SYMBOL) VALUES ('bitcoin', 'Bitcoin', 'btc');
INSERT INTO COIN (ID,NAME,SYMBOL) VALUES ('cardano', 'Cardano', 'ada');
INSERT INTO COIN (ID,NAME,SYMBOL) VALUES ('ethereum', 'Ethereum', 'eth');

--Payments
INSERT INTO PAYMENT (ID,AMOUNT,CURRENCY_ID) VALUES (1, 10.123, 'eur');
INSERT INTO PAYMENT (ID,AMOUNT,CURRENCY_ID) VALUES (2, 5.66, 'eur');
INSERT INTO PAYMENT (ID,AMOUNT,CURRENCY_ID) VALUES (3, 155.66, 'eur');

--Coin Transactions
INSERT INTO COIN_TRANSACTION (ID,BUY_SELL,CREATION_DATE,QUANTITY,TRANSACTION_STRING,COIN_ID,PAYMENT_ID,USER_ID,TRANSACTION_DATE) VALUES (1,'B',CURRENT_TIMESTAMP(),6.77,'Hadwd23HD231','polkadot',1,1,CURRENT_TIMESTAMP());
INSERT INTO COIN_TRANSACTION (ID,BUY_SELL,CREATION_DATE,QUANTITY,TRANSACTION_STRING,COIN_ID,PAYMENT_ID,USER_ID,TRANSACTION_DATE) VALUES (2,'B',CURRENT_TIMESTAMP(),96.23,'f44FFFFweqcawd34','cardano',2,1,CURRENT_TIMESTAMP());
INSERT INTO COIN_TRANSACTION (ID,BUY_SELL,CREATION_DATE,QUANTITY,TRANSACTION_STRING,COIN_ID,PAYMENT_ID,USER_ID,TRANSACTION_DATE) VALUES (3,'B',CURRENT_TIMESTAMP(),101.22,'kjZhfef321Jzh','polkadot',3,1,CURRENT_TIMESTAMP());

--Configs
INSERT INTO CONFIG (CONFIG_KEY,CONFIG_VALUE) VALUES ('COIN_GECKO_URL', 'https://api.coingecko.com/api/v3/');