package eye.on.the.money.service.financial;

import eye.on.the.money.config.AppConfig;
import eye.on.the.money.dto.out.BankTransactionDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.financial.BankTransaction;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
class BankTransactionMappingTest {

    private final ModelMapper modelMapper = new AppConfig().modelMapper();

    @Test
    void mapsEveryFieldToTheRightDestination() {
        BankTransaction transaction = BankTransaction.builder()
                .id(42L)
                .bankTransactionId("AAACT253654SV4TMYZ")
                .bookingDate(LocalDate.of(2025, 12, 31))
                .type("Kamatado")
                .accountNumber("104040278676776881541004")
                .accountName("HANICZ TAMAS")
                .partnerAccount("120010000000000000000000")
                .partnerName("PARTNER KFT")
                .amount(-275.0)
                .memo("Ref.: AAACT253654SV4TMYZ")
                .creationDate(LocalDate.of(2026, 1, 1))
                .currency(new Currency("HUF", "forint"))
                .user(User.builder().id(1L).email("test@email.com").build())
                .build();

        BankTransactionDTO dto = this.modelMapper.map(transaction, BankTransactionDTO.class);

        assertEquals(42L, dto.getId());
        assertEquals("AAACT253654SV4TMYZ", dto.getBankTransactionId());
        assertEquals(LocalDate.of(2025, 12, 31), dto.getBookingDate());
        assertEquals("Kamatado", dto.getType());
        assertEquals("104040278676776881541004", dto.getAccountNumber());
        assertEquals("HANICZ TAMAS", dto.getAccountName());
        assertEquals("120010000000000000000000", dto.getPartnerAccount());
        assertEquals("PARTNER KFT", dto.getPartnerName());
        assertEquals(-275.0, dto.getAmount());
        assertEquals("Ref.: AAACT253654SV4TMYZ", dto.getMemo());
        assertEquals("HUF", dto.getCurrencyId());
    }
}
