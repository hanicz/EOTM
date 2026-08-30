package eye.on.the.money.dto.in;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.model.financial.AccountSide;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
class BankExclusionRuleEditDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void readsThePayloadTheUiSends() throws JsonProcessingException {
        String json = "{\"name\":\"Rent\",\"accountNumber\":\"1111-2222\","
                + "\"side\":\"PARTNER_ACCOUNT\",\"active\":true}";

        BankExclusionRuleEditDTO dto = this.objectMapper.readValue(json, BankExclusionRuleEditDTO.class);

        assertEquals("Rent", dto.name());
        assertEquals("1111-2222", dto.accountNumber());
        assertEquals(AccountSide.PARTNER_ACCOUNT, dto.side());
        assertTrue(dto.active());
    }

    @Test
    void readsAPayloadWithoutAName() throws JsonProcessingException {
        String json = "{\"accountNumber\":\"1111-2222\",\"side\":\"ANY\",\"active\":false}";

        BankExclusionRuleEditDTO dto = this.objectMapper.readValue(json, BankExclusionRuleEditDTO.class);

        assertNull(dto.name());
        assertEquals(AccountSide.ANY, dto.side());
    }
}
