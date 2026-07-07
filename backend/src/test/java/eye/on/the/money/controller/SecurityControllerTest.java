package eye.on.the.money.controller;

import eye.on.the.money.model.security.Security;
import eye.on.the.money.service.security.SecurityService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class SecurityControllerTest {

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private SecurityController securityController;

    @Test
    void getAllSecurities() {
        List<Security> securities = new ArrayList<>();
        securities.add(Security.builder().id("SEC1").name("Security One").build());
        securities.add(Security.builder().id("SEC2").name("Security Two").build());
        securities.add(Security.builder().id("SEC3").name("Security Three").build());
        when(this.securityService.getAllSecurities()).thenReturn(securities);

        Assertions.assertIterableEquals(securities, this.securityController.getAllSecurities().getBody());
    }

    @Test
    void getAllSecurities_returnsEmptyList() {
        when(this.securityService.getAllSecurities()).thenReturn(List.of());

        Assertions.assertIterableEquals(List.of(), this.securityController.getAllSecurities().getBody());
    }
}
