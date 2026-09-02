package eye.on.the.money.util;

import eye.on.the.money.dto.Lot;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

class LotsTest {

    @Getter
    @Setter
    @AllArgsConstructor
    private static final class Trade implements Lot<Trade> {
        private String symbol;
        private String buySell;
        private LocalDate transactionDate;
        private Integer quantity;
        private Double amount;

        @Override
        public void negateAmountAndQuantity() {
            this.amount = -this.amount;
            this.quantity = -this.quantity;
        }

        @Override
        public Trade merge(Trade other) {
            this.amount += other.amount;
            this.quantity += other.quantity;
            return this;
        }

        @Override
        public boolean isClosed() {
            return this.quantity == 0;
        }
    }

    private Trade trade(String symbol, String buySell, String date, int quantity, double amount) {
        return new Trade(symbol, buySell, LocalDate.parse(date), quantity, amount);
    }

    @Test
    public void aReopenedPositionStartsAFreshCostBasis() {
        Map<String, Trade> lots = Lots.aggregate(List.of(
                this.trade("AAA", "B", "2024-01-01", 10, 100.0),
                this.trade("AAA", "S", "2024-02-01", 10, 150.0),
                this.trade("AAA", "B", "2024-03-01", 5, 50.0)), Trade::getSymbol);

        List<Trade> trades = List.copyOf(lots.values());

        Assertions.assertAll("The realised gain stays on the closed lot",
                () -> Assertions.assertEquals(2, trades.size()),
                () -> Assertions.assertEquals(0, trades.get(0).getQuantity()),
                () -> Assertions.assertEquals(-50.0, trades.get(0).getAmount()),
                () -> Assertions.assertEquals(5, trades.get(1).getQuantity()),
                () -> Assertions.assertEquals(50.0, trades.get(1).getAmount()));
    }

    @Test
    public void anUnsortedInputIsFoldedInTransactionDateOrder() {
        Map<String, Trade> lots = Lots.aggregate(List.of(
                this.trade("AAA", "B", "2024-03-01", 5, 50.0),
                this.trade("AAA", "S", "2024-02-01", 10, 150.0),
                this.trade("AAA", "B", "2024-01-01", 10, 100.0)), Trade::getSymbol);

        List<Trade> trades = List.copyOf(lots.values());

        Assertions.assertAll("A descending list must give the same two lots as an ascending one",
                () -> Assertions.assertEquals(2, trades.size()),
                () -> Assertions.assertEquals(-50.0, trades.get(0).getAmount()),
                () -> Assertions.assertEquals(50.0, trades.get(1).getAmount()));
    }

    @Test
    public void eachKeySealsItsOwnLots() {
        Map<String, Trade> lots = Lots.aggregate(List.of(
                this.trade("AAA", "B", "2024-01-01", 10, 100.0),
                this.trade("BBB", "B", "2024-01-02", 4, 80.0),
                this.trade("AAA", "S", "2024-02-01", 10, 150.0),
                this.trade("AAA", "B", "2024-03-01", 5, 50.0)), Trade::getSymbol);

        Assertions.assertAll("Closing AAA must not touch BBB",
                () -> Assertions.assertEquals(3, lots.size()),
                () -> Assertions.assertEquals(-50.0, lots.get("AAA_0").getAmount()),
                () -> Assertions.assertEquals(50.0, lots.get("AAA_1").getAmount()),
                () -> Assertions.assertEquals(80.0, lots.get("BBB_0").getAmount()));
    }

    @Test
    public void anOverSoldPositionKeepsAccumulatingInTheSameLot() {
        Map<String, Trade> lots = Lots.aggregate(List.of(
                this.trade("AAA", "B", "2024-01-01", 10, 100.0),
                this.trade("AAA", "S", "2024-02-01", 12, 180.0),
                this.trade("AAA", "B", "2024-03-01", 5, 50.0)), Trade::getSymbol);

        Assertions.assertAll("Only an exact zero seals a lot",
                () -> Assertions.assertEquals(1, lots.size()),
                () -> Assertions.assertEquals(3, lots.get("AAA_0").getQuantity()),
                () -> Assertions.assertEquals(-30.0, lots.get("AAA_0").getAmount()));
    }

    @Test
    public void anEmptyInputYieldsNoLots() {
        Assertions.assertTrue(Lots.aggregate(List.<Trade>of(), Trade::getSymbol).isEmpty());
    }
}
