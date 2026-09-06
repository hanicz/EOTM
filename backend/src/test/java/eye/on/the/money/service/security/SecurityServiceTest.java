package eye.on.the.money.service.security;

import eye.on.the.money.exception.CSVException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void getOrCreateByName_returnsExistingWhenFoundByName() {
        Security existing = Security.builder().id("PMÁP 2099/Z").name("Prémium Magyar Állampapír 2099/Z").build();
        when(this.securityRepository.findByName("Prémium Magyar Állampapír 2099/Z")).thenReturn(Optional.of(existing));

        Security result = this.securityService.getOrCreateByName("Prémium Magyar Állampapír 2099/Z");

        assertEquals("PMÁP 2099/Z", result.getId());
        verify(this.securityRepository, never()).save(any());
    }

    @Test
    void getOrCreateByName_derivesIdAndSavesWhenNotFound() {
        when(this.securityRepository.findByName("Euró Magyar Állampapír 2099/Z")).thenReturn(Optional.empty());
        when(this.securityRepository.findById("EMÁP 2099/Z")).thenReturn(Optional.empty());
        when(this.securityRepository.save(any(Security.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Security result = this.securityService.getOrCreateByName("Euró Magyar Állampapír 2099/Z");

        assertEquals("EMÁP 2099/Z", result.getId());
        assertEquals("Euró Magyar Állampapír 2099/Z", result.getName());
        verify(this.securityRepository, times(1)).save(any(Security.class));
    }

    @Test
    void getOrCreateByName_derivesDiscountTreasuryBillId() {
        when(this.securityRepository.findByName("Diszkont Kincstárjegy D990101")).thenReturn(Optional.empty());
        when(this.securityRepository.findById("DKJ D990101")).thenReturn(Optional.empty());
        when(this.securityRepository.save(any(Security.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Security result = this.securityService.getOrCreateByName("Diszkont Kincstárjegy D990101");

        assertEquals("DKJ D990101", result.getId());
    }

    @Test
    void getOrCreateByName_throwsOnUnknownPrefix() {
        when(this.securityRepository.findByName("Valami Más Papír 2099/Z")).thenReturn(Optional.empty());

        CSVException exception = assertThrows(CSVException.class,
                () -> this.securityService.getOrCreateByName("Valami Más Papír 2099/Z"));

        assertEquals("Unknown instrument: Valami Más Papír 2099/Z", exception.getMessage());
        verify(this.securityRepository, never()).save(any());
    }
}
