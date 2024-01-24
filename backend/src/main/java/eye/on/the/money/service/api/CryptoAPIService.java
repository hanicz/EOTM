package eye.on.the.money.service.api;

import com.fasterxml.jackson.databind.JsonNode;

public interface CryptoAPIService {
    JsonNode getLiveValueForCoins(String currency, String ids);
}
