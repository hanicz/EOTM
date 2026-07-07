package eye.on.the.money.controller;

import eye.on.the.money.dto.out.InterestDTO;
import eye.on.the.money.service.security.InterestService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class InterestControllerTest {

    @Mock
    private InterestService interestService;

    @InjectMocks
    private InterestController interestController;

    private List<InterestDTO> createInterestList() {
        List<InterestDTO> list = new ArrayList<>();
        list.add(InterestDTO.builder().interestId(1L).amount(50.0).interestDate(LocalDate.now())
                .securityId("SEC1").securityName("Security One").currencyId("EUR").build());
        list.add(InterestDTO.builder().interestId(2L).amount(75.0).interestDate(LocalDate.now())
                .securityId("SEC2").securityName("Security Two").currencyId("USD").build());
        return list;
    }

    @Test
    void getAllInterest() {
        List<InterestDTO> interests = this.createInterestList();
        when(this.interestService.getInterest("email")).thenReturn(interests);

        Assertions.assertIterableEquals(interests, this.interestController.getAllInterest("email").getBody());
    }

    @Test
    void createInterest() {
        InterestDTO dto = InterestDTO.builder().interestId(1L).amount(50.0).interestDate(LocalDate.now())
                .securityId("SEC1").securityName("Security One").currencyId("EUR").build();
        when(this.interestService.createInterest(dto, "email")).thenReturn(dto);

        Assertions.assertEquals(HttpStatus.CREATED, this.interestController.createInterest("email", dto).getStatusCode());
        Assertions.assertEquals(dto, this.interestController.createInterest("email", dto).getBody());
    }

    @Test
    void deleteByIds() {
        doNothing().when(this.interestService).deleteInterestById(any(), any());

        Assertions.assertEquals(HttpStatus.OK, this.interestController.deleteByIds("email", List.of(1L, 2L)).getStatusCode());
    }

    @Test
    void getCSV() throws IOException {
        HttpServletResponse httpSR = new MockHttpServletResponse();
        doNothing().when(this.interestService).getCSV(any(), any());

        this.interestController.getCSV("email", httpSR);

        verify(this.interestService, times(1)).getCSV(any(), any());
    }

    @Test
    void updateInterest() {
        InterestDTO dto = InterestDTO.builder().interestId(1L).amount(75.0).interestDate(LocalDate.now())
                .securityId("SEC1").securityName("Security One").currencyId("EUR").build();
        when(this.interestService.updateInterest(dto, "email")).thenReturn(dto);

        Assertions.assertEquals(dto, this.interestController.updateInterest("email", dto).getBody());
    }

    @Test
    void processCSV() throws IOException {
        doNothing().when(this.interestService).processCSV(any(), any());

        Assertions.assertEquals(HttpStatus.CREATED, this.interestController.processCSV("email", null).getStatusCode());
    }
}
