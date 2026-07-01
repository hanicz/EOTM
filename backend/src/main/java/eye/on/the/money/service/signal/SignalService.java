package eye.on.the.money.service.signal;

import eye.on.the.money.dto.in.EODCandleQuoteDTO;
import eye.on.the.money.dto.out.IndicatorDetailDTO;
import eye.on.the.money.dto.out.SignalDTO;
import eye.on.the.money.service.api.EODAPIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SignalService {

    // Must stay <= 23: EODAPIService switches from daily to weekly candles above 23 months,
    // which would leave too few data points for the 200-day moving average.
    private static final int HISTORY_MONTHS = 18;
    private static final String BUY = "BUY";
    private static final String SELL = "SELL";
    private static final String NEUTRAL = "NEUTRAL";

    private final EODAPIService eodAPIService;

    public SignalDTO evaluate(String shortName) {
        log.trace("Enter evaluate");
        List<EODCandleQuoteDTO> candles = this.eodAPIService.getCandleQuoteByShortName(shortName, HISTORY_MONTHS);
        List<Double> closes = candles.stream().map(EODCandleQuoteDTO::getClose).toList();

        List<IndicatorDetailDTO> indicators = List.of(
                this.buildMovingAverageIndicator(closes),
                this.buildRsiIndicator(closes),
                this.buildMacdIndicator(closes)
        );

        long buyVotes = indicators.stream().filter(i -> BUY.equals(i.getVote())).count();
        long sellVotes = indicators.stream().filter(i -> SELL.equals(i.getVote())).count();

        String signal = "HOLD";
        if (buyVotes > sellVotes) {
            signal = BUY;
        } else if (sellVotes > buyVotes) {
            signal = SELL;
        }

        return SignalDTO.builder().signal(signal).indicators(indicators).build();
    }

    private IndicatorDetailDTO buildMovingAverageIndicator(List<Double> closes) {
        String name = "Moving Average Crossover (50/200)";
        String description = "Compares the average closing price over the last 50 days against the last 200 days. "
                + "When the shorter-term average overtakes the longer-term one (a \"golden cross\"), it signals upward "
                + "momentum building; when it falls below (a \"death cross\"), it signals the trend is turning down.";

        Optional<Double> sma50 = IndicatorCalculator.sma(closes, 50);
        Optional<Double> sma200 = IndicatorCalculator.sma(closes, 200);

        if (sma50.isEmpty() || sma200.isEmpty()) {
            return IndicatorDetailDTO.builder().name(name).description(description).vote(NEUTRAL)
                    .detail("Not enough price history yet for a reliable reading.").build();
        }

        if (sma50.get() > sma200.get()) {
            return IndicatorDetailDTO.builder().name(name).description(description).vote(BUY)
                    .detail(String.format("50-day average (%.2f) is above the 200-day average (%.2f) — golden cross, bullish trend.",
                            sma50.get(), sma200.get())).build();
        }

        return IndicatorDetailDTO.builder().name(name).description(description).vote(SELL)
                .detail(String.format("50-day average (%.2f) is below the 200-day average (%.2f) — death cross, bearish trend.",
                        sma50.get(), sma200.get())).build();
    }

    private IndicatorDetailDTO buildRsiIndicator(List<Double> closes) {
        String name = "Relative Strength Index (14)";
        String description = "The Relative Strength Index measures how fast and how far price has moved recently, on a "
                + "0-100 scale. Readings below 30 suggest the stock has been sold off more than usual and may be due for a "
                + "bounce (oversold). Readings above 70 suggest it has been bought up more than usual and may be due for a "
                + "pullback (overbought).";

        Optional<Double> rsiValue = IndicatorCalculator.rsi(closes, 14);
        if (rsiValue.isEmpty()) {
            return IndicatorDetailDTO.builder().name(name).description(description).vote(NEUTRAL)
                    .detail("Not enough price history yet for a reliable reading.").build();
        }

        double value = rsiValue.get();
        if (value < 30) {
            return IndicatorDetailDTO.builder().name(name).description(description).vote(BUY)
                    .detail(String.format("RSI is %.1f — oversold.", value)).build();
        }
        if (value > 70) {
            return IndicatorDetailDTO.builder().name(name).description(description).vote(SELL)
                    .detail(String.format("RSI is %.1f — overbought.", value)).build();
        }
        return IndicatorDetailDTO.builder().name(name).description(description).vote(NEUTRAL)
                .detail(String.format("RSI is %.1f — neutral, no extreme.", value)).build();
    }

    private IndicatorDetailDTO buildMacdIndicator(List<Double> closes) {
        String name = "MACD (12, 26, 9)";
        String description = "The Moving Average Convergence/Divergence indicator tracks the gap between a fast and a "
                + "slow moving average of price to gauge momentum. When the MACD line is above its own signal line, "
                + "momentum is shifting upward (bullish); when it dips below, momentum is shifting downward (bearish).";

        Optional<double[]> macdResult = IndicatorCalculator.macd(closes, 12, 26, 9);
        if (macdResult.isEmpty()) {
            return IndicatorDetailDTO.builder().name(name).description(description).vote(NEUTRAL)
                    .detail("Not enough price history yet for a reliable reading.").build();
        }

        double macdValue = macdResult.get()[0];
        double signalValue = macdResult.get()[1];

        if (macdValue > signalValue) {
            return IndicatorDetailDTO.builder().name(name).description(description).vote(BUY)
                    .detail(String.format("MACD (%.2f) is above its signal line (%.2f) — bullish momentum.", macdValue, signalValue)).build();
        }

        return IndicatorDetailDTO.builder().name(name).description(description).vote(SELL)
                .detail(String.format("MACD (%.2f) is below its signal line (%.2f) — bearish momentum.", macdValue, signalValue)).build();
    }
}
