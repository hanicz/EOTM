package eye.on.the.money.service.financial;

import eye.on.the.money.config.AppConfig;
import eye.on.the.money.dto.out.BankTransactionDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.financial.BankTransaction;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
class BankTransactionMappingTest {

    private final ModelMapper modelMapper = new AppConfig().modelMapper();

    @Test
    void mapsEveryFieldToTheRightDestination() {
        BankTransaction transaction = BankTransaction.builder()
                .id(42L)
                .bankTransactionId("ABCDE123456AB1CDEF")
                .bookingDate(LocalDate.of(2025, 12, 31))
                .type("Kamatado")
                .accountNumber("111111112222222233333333")
                .accountName("ACCOUNT HOLDER")
                .partnerAccount("120010000000000000000000")
                .partnerName("PARTNER KFT")
                .amount(new BigDecimal("-275"))
                .memo("Ref.: ABCDE123456AB1CDEF")
                .currency(new Currency("HUF", "forint"))
                .user(User.builder().id(1L).email("test@email.com").build())
                .build();

        BankTransactionDTO dto = this.modelMapper.map(transaction, BankTransactionDTO.class);

        assertEquals(42L, dto.getId());
        assertEquals("ABCDE123456AB1CDEF", dto.getBankTransactionId());
        assertEquals(LocalDate.of(2025, 12, 31), dto.getBookingDate());
        assertEquals("Kamatado", dto.getType());
        assertEquals("111111112222222233333333", dto.getAccountNumber());
        assertEquals("ACCOUNT HOLDER", dto.getAccountName());
        assertEquals("120010000000000000000000", dto.getPartnerAccount());
        assertEquals("PARTNER KFT", dto.getPartnerName());
        assertEquals(new BigDecimal("-275"), dto.getAmount());
        assertEquals("Ref.: ABCDE123456AB1CDEF", dto.getMemo());
        assertEquals("HUF", dto.getCurrencyId());
    }
}
