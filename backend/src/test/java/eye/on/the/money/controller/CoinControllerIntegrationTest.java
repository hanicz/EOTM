package eye.on.the.money.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.EotmApplication;
import eye.on.the.money.model.crypto.Coin;
import eye.on.the.money.repository.crypto.CoinRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = EotmApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CoinControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CoinRepository coinRepository;

    private final ObjectMapper om = new ObjectMapper();

    @Test
    public void getAllCoins() throws Exception {
        List<Coin> coins = this.coinRepository.findAllByOrderByNameAsc();
        MvcResult response = this.mockMvc.perform(get("/api/v1/coin")).andExpect(status().isOk()).andReturn();
        List<Coin> result = om.readValue(response.getResponse().getContentAsString(), new TypeReference<>() {});

        Assertions.assertIterableEquals(coins,result);
    }
}