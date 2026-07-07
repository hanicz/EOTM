package eye.on.the.money.service.security;

import eye.on.the.money.dto.out.InterestDTO;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.security.Interest;
import eye.on.the.money.model.security.Security;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.security.InterestRepository;
import eye.on.the.money.service.user.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class InterestServiceTest {

    @Mock
    private InterestRepository interestRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private SecurityService securityService;
    @Mock
    private UserServiceImpl userService;
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private InterestService interestService;

    private final User user = User.builder().id(1L).email("test@email.com").build();
    private final Currency currency = new Currency("EUR", "Euro");
    private final Security security = Security.builder().id("SEC1").name("Security One").build();

    private Interest buildInterest(Long id) {
        return Interest.builder().id(id).amount(100.0).interestDate(LocalDate.of(2025, 6, 1))
                .currency(this.currency).security(this.security).user(this.user).build();
    }

    private InterestDTO buildInterestDTO(Long id) {
        return InterestDTO.builder().interestId(id).amount(100.0).interestDate(LocalDate.of(2025, 6, 1))
                .currencyId("EUR").securityId("SEC1").securityName("Security One").build();
    }

    @Test
    void getInterest_returnsMappedDTOs() {
        Interest interest = this.buildInterest(1L);
        InterestDTO dto = this.buildInterestDTO(1L);
        when(this.interestRepository.findByUserEmailOrderByInterestDateDesc("test@email.com")).thenReturn(List.of(interest));
        when(this.modelMapper.map(interest, InterestDTO.class)).thenReturn(dto);

        List<InterestDTO> result = this.interestService.getInterest("test@email.com");

        assertEquals(1, result.size());
        assertEquals(100.0, result.get(0).getAmount());
    }

    @Test
    void getInterest_returnsEmptyList() {
        when(this.interestRepository.findByUserEmailOrderByInterestDateDesc("test@email.com")).thenReturn(List.of());

        List<InterestDTO> result = this.interestService.getInterest("test@email.com");

        assertTrue(result.isEmpty());
    }

    @Test
    void createInterest_savesAndReturnsDTO() {
        InterestDTO inputDTO = this.buildInterestDTO(null);
        Interest savedInterest = this.buildInterest(1L);
        InterestDTO outputDTO = this.buildInterestDTO(1L);

        when(this.currencyRepository.findById("EUR")).thenReturn(Optional.of(this.currency));
        when(this.securityService.getOrCreateSecurity("SEC1", "Security One")).thenReturn(this.security);
        when(this.userService.loadUserByEmail("test@email.com")).thenReturn(this.user);
        when(this.interestRepository.save(any(Interest.class))).thenReturn(savedInterest);
        when(this.modelMapper.map(savedInterest, InterestDTO.class)).thenReturn(outputDTO);

        InterestDTO result = this.interestService.createInterest(inputDTO, "test@email.com");

        assertEquals(1L, result.getInterestId());
        assertEquals(100.0, result.getAmount());
        verify(this.interestRepository, times(1)).save(any(Interest.class));
    }

    @Test
    void createInterest_throwsWhenCurrencyNotFound() {
        InterestDTO inputDTO = this.buildInterestDTO(null);
        when(this.currencyRepository.findById("EUR")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> this.interestService.createInterest(inputDTO, "test@email.com"));
    }

    @Test
    void updateInterest_updatesAndReturnsDTO() {
        InterestDTO inputDTO = this.buildInterestDTO(1L);
        Interest existingInterest = this.buildInterest(1L);
        InterestDTO outputDTO = this.buildInterestDTO(1L);

        when(this.currencyRepository.findById("EUR")).thenReturn(Optional.of(this.currency));
        when(this.securityService.getOrCreateSecurity("SEC1", "Security One")).thenReturn(this.security);
        when(this.interestRepository.findByIdAndUserEmail(1L, "test@email.com")).thenReturn(Optional.of(existingInterest));
        when(this.modelMapper.map(existingInterest, InterestDTO.class)).thenReturn(outputDTO);

        InterestDTO result = this.interestService.updateInterest(inputDTO, "test@email.com");

        assertEquals(1L, result.getInterestId());
    }

    @Test
    void updateInterest_throwsWhenInterestNotFound() {
        InterestDTO inputDTO = this.buildInterestDTO(1L);
        when(this.currencyRepository.findById("EUR")).thenReturn(Optional.of(this.currency));
        when(this.securityService.getOrCreateSecurity("SEC1", "Security One")).thenReturn(this.security);
        when(this.interestRepository.findByIdAndUserEmail(1L, "test@email.com")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> this.interestService.updateInterest(inputDTO, "test@email.com"));
    }

    @Test
    void deleteInterestById_delegatesToRepository() {
        List<Long> ids = List.of(1L, 2L, 3L);
        doNothing().when(this.interestRepository).deleteByUserEmailAndIdIn("test@email.com", ids);

        this.interestService.deleteInterestById(ids, "test@email.com");

        verify(this.interestRepository, times(1)).deleteByUserEmailAndIdIn("test@email.com", ids);
    }
}
