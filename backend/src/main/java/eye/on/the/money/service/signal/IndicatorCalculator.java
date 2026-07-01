package eye.on.the.money.service.signal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class IndicatorCalculator {

    private IndicatorCalculator() {
    }

    static Optional<Double> sma(List<Double> values, int period) {
        if (values.size() < period) {
            return Optional.empty();
        }
        List<Double> slice = values.subList(values.size() - period, values.size());
        return Optional.of(slice.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }

    static List<Double> ema(List<Double> values, int period) {
        List<Double> result = new ArrayList<>(values.size());
        double k = 2.0 / (period + 1);
        for (int i = 0; i < values.size(); i++) {
            if (i == 0) {
                result.add(values.get(i));
            } else {
                result.add(values.get(i) * k + result.get(i - 1) * (1 - k));
            }
        }
        return result;
    }

    static Optional<Double> rsi(List<Double> values, int period) {
        if (values.size() < period + 1) {
            return Optional.empty();
        }
        List<Double> changes = new ArrayList<>(values.size() - 1);
        for (int i = 1; i < values.size(); i++) {
            changes.add(values.get(i) - values.get(i - 1));
        }
        List<Double> recent = changes.subList(changes.size() - period, changes.size());

        double gains = recent.stream().filter(c -> c > 0).mapToDouble(Double::doubleValue).sum() / period;
        double losses = recent.stream().filter(c -> c < 0).mapToDouble(c -> -c).sum() / period;

        if (losses == 0) {
            return Optional.of(100.0);
        }
        double rs = gains / losses;
        return Optional.of(100 - (100 / (1 + rs)));
    }

    static Optional<double[]> macd(List<Double> values, int fast, int slow, int signalPeriod) {
        if (values.size() < slow + signalPeriod) {
            return Optional.empty();
        }
        List<Double> emaFast = ema(values, fast);
        List<Double> emaSlow = ema(values, slow);
        List<Double> macdLine = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            macdLine.add(emaFast.get(i) - emaSlow.get(i));
        }
        List<Double> signalLine = ema(macdLine, signalPeriod);

        return Optional.of(new double[]{macdLine.get(macdLine.size() - 1), signalLine.get(signalLine.size() - 1)});
    }
}
