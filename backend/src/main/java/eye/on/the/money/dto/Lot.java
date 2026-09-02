package eye.on.the.money.dto;

import java.time.LocalDate;

public interface Lot<T extends Lot<T>> {
    String getBuySell();

    LocalDate getTransactionDate();

    void negateAmountAndQuantity();

    T merge(T other);

    boolean isClosed();
}
