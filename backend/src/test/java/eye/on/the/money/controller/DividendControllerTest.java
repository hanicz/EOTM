package eye.on.the.money.controller;

import eye.on.the.money.dto.out.DividendDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.stock.DividendService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class DividendControllerTest {

    @Mock
    private DividendService dividendService;

    @InjectMocks
    private DividendController dividendController;

    private final User user = User.builder().id(1L).email("email").build();

    @Test
    public void getAllDividends() {
        List<DividendDTO> dividends = new ArrayList<>();
        dividends.add(DividendDTO.builder().dividendId(1L).exchange("e1").dividendDate(LocalDate.now()).amount(55.1).currencyId("c1").shortName("s1").build());
        dividends.add(DividendDTO.builder().dividendId(2L).exchange("e2").dividendDate(LocalDate.now()).amount(51.2).currencyId("c2").shortName("s2").build());
        dividends.add(DividendDTO.builder().dividendId(3L).exchange("e3").dividendDate(LocalDate.now()).amount(50.3).currencyId("c3").shortName("s3").build());

        when(this.dividendService.getDividends("email")).thenReturn(dividends);

        Assertions.assertIterableEquals(dividends, this.dividendController.getAllDividends(this.user).getBody());
    }

    @Test
    public void createDividend() {
        DividendDTO dividendDTO = DividendDTO.builder().dividendId(1L).exchange("e1").dividendDate(LocalDate.now()).amount(55.1).currencyId("c1").shortName("s1").build();
        when(this.dividendService.createDividend(dividendDTO, "email")).thenReturn(dividendDTO);

        Assertions.assertEquals(dividendDTO, this.dividendController.createDividend(this.user, dividendDTO).getBody());
    }

    @Test
    public void deleteByIds() {
        doNothing().when(this.dividendService).deleteDividendById(any(), any());

        Assertions.assertEquals(HttpStatus.OK, this.dividendController.deleteByIds(this.user, "1,2,3").getStatusCode());
    }

    @Test
    public void getCSV() throws IOException {
        HttpServletResponse httpSR = new MockHttpServletResponse();

        doNothing().when(this.dividendService).getCSV(any(), any());
        this.dividendController.getCSV(this.user, httpSR);

        verify(this.dividendService, times(1)).getCSV(any(), any());
    }

    @Test
    public void updateDividend() {
        DividendDTO dividendDTO = DividendDTO.builder().dividendId(1L).exchange("e1").dividendDate(LocalDate.now()).amount(55.1).currencyId("c1").shortName("s1").build();
        when(this.dividendService.updateDividend(dividendDTO, "email")).thenReturn(dividendDTO);

        Assertions.assertEquals(dividendDTO, this.dividendController.updateDividend(this.user, dividendDTO).getBody());
    }

    @Test
    public void processCSV() throws IOException {
        MultipartFile mpf = new MockMultipartFile("mpf", "mpf.csv", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());

        doNothing().when(this.dividendService).processCSV("email", mpf);

        Assertions.assertEquals(HttpStatus.CREATED, this.dividendController.processCSV(this.user, mpf).getStatusCode());
    }
}