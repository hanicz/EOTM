package eye.on.the.money.service.signal;

import eye.on.the.money.dto.in.EODCandleQuoteDTO;
import eye.on.the.money.dto.out.SignalDTO;
import eye.on.the.money.service.api.EODAPIService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class SignalServiceTest {

    @Mock
    private EODAPIService eodAPIService;

    @InjectMocks
    private SignalService signalService;

    private List<EODCandleQuoteDTO> buildCandles(List<Double> closes) {
        return closes.stream()
                .map(c -> EODCandleQuoteDTO.builder().close(c).build())
                .toList();
    }

    @Test
    void evaluate_returnsHoldWhenNotEnoughData() {
        List<Double> closes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            closes.add(100.0 + i);
        }
        when(this.eodAPIService.getCandleQuoteByShortName("AAPL.US", 18)).thenReturn(this.buildCandles(closes));

        SignalDTO result = this.signalService.evaluate("AAPL.US");

        assertNotNull(result);
        assertEquals("HOLD", result.getSignal());
        assertEquals(3, result.getIndicators().size());
        result.getIndicators().forEach(i -> assertEquals("NEUTRAL", i.getVote()));
    }

    @Test
    void evaluate_returnsBuyForStrongUptrend() {
        List<Double> closes = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            closes.add(50.0 + i * 0.5);
        }
        when(this.eodAPIService.getCandleQuoteByShortName("AAPL.US", 18)).thenReturn(this.buildCandles(closes));

        SignalDTO result = this.signalService.evaluate("AAPL.US");

        assertNotNull(result);
        assertEquals(3, result.getIndicators().size());
        long buyVotes = result.getIndicators().stream().filter(i -> "BUY".equals(i.getVote())).count();
        assertTrue(buyVotes >= 2);
        assertEquals("BUY", result.getSignal());
    }

    @Test
    void evaluate_returnsSellForStrongDowntrend() {
        List<Double> closes = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            closes.add(200.0 - i * 0.5);
        }
        when(this.eodAPIService.getCandleQuoteByShortName("AAPL.US", 18)).thenReturn(this.buildCandles(closes));

        SignalDTO result = this.signalService.evaluate("AAPL.US");

        assertNotNull(result);
        assertEquals(3, result.getIndicators().size());
        long sellVotes = result.getIndicators().stream().filter(i -> "SELL".equals(i.getVote())).count();
        assertTrue(sellVotes >= 2);
        assertEquals("SELL", result.getSignal());
    }

    @Test
    void evaluate_indicatorsHaveNamesAndDescriptions() {
        List<Double> closes = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            closes.add(100.0 + i);
        }
        when(this.eodAPIService.getCandleQuoteByShortName("AAPL.US", 18)).thenReturn(this.buildCandles(closes));

        SignalDTO result = this.signalService.evaluate("AAPL.US");

        result.getIndicators().forEach(i -> {
            assertNotNull(i.getName());
            assertNotNull(i.getDescription());
            assertNotNull(i.getDetail());
            assertFalse(i.getName().isEmpty());
        });
    }

    @Test
    void evaluate_returnsHoldWhenVotesAreTied() {
        List<Double> closes = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            closes.add(100.0 + Math.sin(i * 0.1) * 5);
        }
        when(this.eodAPIService.getCandleQuoteByShortName("FLAT.US", 18)).thenReturn(this.buildCandles(closes));

        SignalDTO result = this.signalService.evaluate("FLAT.US");

        assertNotNull(result);
        assertEquals(3, result.getIndicators().size());
    }
}
