package eye.on.the.money.dto.out;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
class CryptoWatchDTOTest {

    @Test
    public void compareToEqual() {
        CryptoWatchDTO cDTO1 = CryptoWatchDTO.builder().liveValue(50.6).build();
        CryptoWatchDTO cDTO2 = CryptoWatchDTO.builder().liveValue(50.6).build();

        Assertions.assertEquals(0, cDTO1.compareTo(cDTO2));
    }

    @Test
    public void compareToGreater() {
        CryptoWatchDTO cDTO1 = CryptoWatchDTO.builder().liveValue(50.61).build();
        CryptoWatchDTO cDTO2 = CryptoWatchDTO.builder().liveValue(50.6).build();

        Assertions.assertTrue(cDTO1.compareTo(cDTO2) > 0);
    }

    @Test
    public void compareToLess() {
        CryptoWatchDTO cDTO1 = CryptoWatchDTO.builder().liveValue(50.59).build();
        CryptoWatchDTO cDTO2 = CryptoWatchDTO.builder().liveValue(50.6).build();

        Assertions.assertTrue(cDTO1.compareTo(cDTO2) < 0);
    }

    @Test
    public void compareToNull() {
        CryptoWatchDTO cDTO1 = CryptoWatchDTO.builder().build();
        CryptoWatchDTO cDTO2 = CryptoWatchDTO.builder().liveValue(50.6).build();

        Assertions.assertTrue(cDTO1.compareTo(cDTO2) < 0);
    }

    @Test
    public void compareToNull2() {
        CryptoWatchDTO cDTO1 = CryptoWatchDTO.builder().liveValue(50.6).build();
        CryptoWatchDTO cDTO2 = CryptoWatchDTO.builder().build();

        Assertions.assertTrue(cDTO1.compareTo(cDTO2) > 0);
    }
}