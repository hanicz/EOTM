package eye.on.the.money.service.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.repository.ConfigRepository;
import eye.on.the.money.repository.CredentialRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import java.text.MessageFormat;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

@Slf4j
public abstract class APIService {

    @Autowired
    private CredentialRepository credentialRepository;
    @Autowired
    private ConfigRepository configRepository;
    protected final RestTemplate restTemplate = new RestTemplate();
    protected final ObjectMapper mapper = new ObjectMapper();

    protected String createURL(String api, String path, String... params) {
        log.trace("Enter");
        String stockAPI = this.configRepository.findById(api).orElseThrow(NoSuchElementException::new).getConfigValue();
        Object secret = this.credentialRepository.findById(api).orElseThrow(NoSuchElementException::new).getSecret();
        Object[] array = Stream.concat(Stream.of(secret), Stream.of(params)).toArray();

        String URL = MessageFormat.format(stockAPI + path, array);
        log.trace(URL);
        return URL;
    }
}
