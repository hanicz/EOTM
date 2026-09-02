package eye.on.the.money.util;

import eye.on.the.money.dto.Lot;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class Lots {

    private Lots() {
    }

    public static <T extends Lot<T>> Map<String, T> aggregate(List<T> items, Function<T, String> baseKey) {
        Map<String, T> lots = new LinkedHashMap<>();
        Map<String, Integer> lotIndexByKey = new HashMap<>();

        items.stream()
                .sorted(Comparator.comparing(Lot::getTransactionDate))
                .forEach(item -> {
                    if ("S".equals(item.getBuySell())) {
                        item.negateAmountAndQuantity();
                    }
                    String base = baseKey.apply(item);
                    int lotIndex = lotIndexByKey.getOrDefault(base, 0);

                    T merged = lots.compute(base + "_" + lotIndex,
                            (key, value) -> (value == null) ? item : value.merge(item));

                    // Position fully closed: seal this lot and start a fresh cost basis for any later re-buy.
                    if (merged.isClosed()) {
                        lotIndexByKey.put(base, lotIndex + 1);
                    }
                });
        return lots;
    }
}
