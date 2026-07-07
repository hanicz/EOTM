package eye.on.the.money.service.security;

import eye.on.the.money.model.security.Security;
import eye.on.the.money.repository.security.SecurityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    @InjectMocks
    private SecurityService securityService;

    @Test
    void getAllSecurities_returnsList() {
        List<Security> securities = List.of(
                Security.builder().id("AAPL").name("Apple").build(),
                Security.builder().id("MSFT").name("Microsoft").build()
        );
        when(this.securityRepository.findAllByOrderByNameAsc()).thenReturn(securities);

        List<Security> result = this.securityService.getAllSecurities();

        assertEquals(2, result.size());
        assertEquals("AAPL", result.get(0).getId());
    }

    @Test
    void getAllSecurities_returnsEmptyList() {
        when(this.securityRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

        List<Security> result = this.securityService.getAllSecurities();

        assertEquals(0, result.size());
    }

    @Test
    void getOrCreateSecurity_returnsExistingWhenFound() {
        Security existing = Security.builder().id("AAPL").name("Apple").build();
        when(this.securityRepository.findById("AAPL")).thenReturn(Optional.of(existing));

        Security result = this.securityService.getOrCreateSecurity("AAPL", "Apple");

        assertEquals("AAPL", result.getId());
        verify(this.securityRepository, never()).save(any());
    }

    @Test
    void getOrCreateSecurity_createsNewWhenNotFound() {
        Security newSecurity = Security.builder().id("NEW").name("New Security").build();
        when(this.securityRepository.findById("NEW")).thenReturn(Optional.empty());
        when(this.securityRepository.save(any(Security.class))).thenReturn(newSecurity);

        Security result = this.securityService.getOrCreateSecurity("NEW", "New Security");

        assertEquals("NEW", result.getId());
        assertEquals("New Security", result.getName());
        verify(this.securityRepository, times(1)).save(any(Security.class));
    }
}
