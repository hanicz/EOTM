package eye.on.the.money.service.signal;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IndicatorCalculatorTest {

    @Test
    void sma_calculatesCorrectAverage() {
        List<Double> values = List.of(10.0, 20.0, 30.0, 40.0, 50.0);

        Optional<Double> result = IndicatorCalculator.sma(values, 3);

        assertTrue(result.isPresent());
        assertEquals(40.0, result.get(), 0.001);
    }

    @Test
    void sma_returnsEmptyWhenNotEnoughData() {
        List<Double> values = List.of(10.0, 20.0);

        Optional<Double> result = IndicatorCalculator.sma(values, 5);

        assertTrue(result.isEmpty());
    }

    @Test
    void sma_usesLastNValues() {
        List<Double> values = List.of(1.0, 2.0, 3.0, 100.0, 200.0);

        Optional<Double> result = IndicatorCalculator.sma(values, 2);

        assertTrue(result.isPresent());
        assertEquals(150.0, result.get(), 0.001);
    }

    @Test
    void sma_worksWhenPeriodEqualsSize() {
        List<Double> values = List.of(10.0, 20.0, 30.0);

        Optional<Double> result = IndicatorCalculator.sma(values, 3);

        assertTrue(result.isPresent());
        assertEquals(20.0, result.get(), 0.001);
    }

    @Test
    void ema_returnsFirstValueUnchanged() {
        List<Double> values = List.of(100.0, 110.0, 120.0);

        List<Double> result = IndicatorCalculator.ema(values, 10);

        assertEquals(100.0, result.get(0), 0.001);
    }

    @Test
    void ema_returnsSameSizeList() {
        List<Double> values = List.of(10.0, 20.0, 30.0, 40.0, 50.0);

        List<Double> result = IndicatorCalculator.ema(values, 3);

        assertEquals(values.size(), result.size());
    }

    @Test
    void ema_appliesExponentialSmoothing() {
        List<Double> values = List.of(10.0, 20.0, 30.0);
        double k = 2.0 / (3 + 1);

        List<Double> result = IndicatorCalculator.ema(values, 3);

        assertEquals(10.0, result.get(0), 0.001);
        double expected1 = 20.0 * k + 10.0 * (1 - k);
        assertEquals(expected1, result.get(1), 0.001);
        double expected2 = 30.0 * k + expected1 * (1 - k);
        assertEquals(expected2, result.get(2), 0.001);
    }

    @Test
    void ema_withConstantValues_convergesOnThatValue() {
        List<Double> values = new ArrayList<>(Collections.nCopies(20, 50.0));

        List<Double> result = IndicatorCalculator.ema(values, 5);

        assertEquals(50.0, result.get(result.size() - 1), 0.001);
    }

    @Test
    void rsi_returnsEmptyWhenNotEnoughData() {
        List<Double> values = List.of(10.0, 20.0);

        Optional<Double> result = IndicatorCalculator.rsi(values, 14);

        assertTrue(result.isEmpty());
    }

    @Test
    void rsi_returns100WhenOnlyGains() {
        List<Double> values = new ArrayList<>();
        for (int i = 0; i <= 14; i++) {
            values.add((double) (100 + i));
        }

        Optional<Double> result = IndicatorCalculator.rsi(values, 14);

        assertTrue(result.isPresent());
        assertEquals(100.0, result.get(), 0.001);
    }

    @Test
    void rsi_lowValueForOnlyLosses() {
        List<Double> values = new ArrayList<>();
        for (int i = 0; i <= 14; i++) {
            values.add((double) (200 - i));
        }

        Optional<Double> result = IndicatorCalculator.rsi(values, 14);

        assertTrue(result.isPresent());
        assertTrue(result.get() < 1.0);
    }

    @Test
    void rsi_around50ForEqualGainsAndLosses() {
        List<Double> values = new ArrayList<>();
        values.add(100.0);
        for (int i = 1; i <= 14; i++) {
            values.add(i % 2 == 0 ? 110.0 : 90.0);
        }

        Optional<Double> result = IndicatorCalculator.rsi(values, 14);

        assertTrue(result.isPresent());
        assertTrue(result.get() > 30 && result.get() < 70);
    }

    @Test
    void macd_returnsEmptyWhenNotEnoughData() {
        List<Double> values = List.of(10.0, 20.0, 30.0);

        Optional<double[]> result = IndicatorCalculator.macd(values, 12, 26, 9);

        assertTrue(result.isEmpty());
    }

    @Test
    void macd_returnsArrayOfTwo() {
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            values.add(100.0 + i);
        }

        Optional<double[]> result = IndicatorCalculator.macd(values, 12, 26, 9);

        assertTrue(result.isPresent());
        assertEquals(2, result.get().length);
    }

    @Test
    void macd_positiveForUptrend() {
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            values.add(100.0 + i * 2);
        }

        Optional<double[]> result = IndicatorCalculator.macd(values, 12, 26, 9);

        assertTrue(result.isPresent());
        assertTrue(result.get()[0] > 0);
    }

    @Test
    void macd_negativeForDowntrend() {
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            values.add(200.0 - i * 2);
        }

        Optional<double[]> result = IndicatorCalculator.macd(values, 12, 26, 9);

        assertTrue(result.isPresent());
        assertTrue(result.get()[0] < 0);
    }
}
