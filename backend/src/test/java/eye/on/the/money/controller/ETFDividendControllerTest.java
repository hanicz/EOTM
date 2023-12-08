package eye.on.the.money.controller;

import eye.on.the.money.dto.out.ETFDividendDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.etf.ETFDividendService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class ETFDividendControllerTest {

    @Mock
    private ETFDividendService etfDividendService;

    @InjectMocks
    private ETFDividendController etfDividendController;

    private final User user = User.builder().id(1L).build();

    @Test
    public void getAllETFDividends() {
        List<ETFDividendDTO> edDTO = new ArrayList<>();
        edDTO.add(ETFDividendDTO.builder().dividendDate(new Date()).id(1L).amount(5.6).currencyId("eur").shortName("s1").exchange("e1").build());
        edDTO.add(ETFDividendDTO.builder().dividendDate(new Date()).id(2L).amount(5.36).currencyId("huf").shortName("s2").exchange("e2").build());
        edDTO.add(ETFDividendDTO.builder().dividendDate(new Date()).id(3L).amount(51.6).currencyId("usd").shortName("s3").exchange("e3").build());

        when(this.etfDividendService.getDividends(this.user.getId())).thenReturn(edDTO);

        Assertions.assertIterableEquals(edDTO, this.etfDividendController.getAllETFDividends(this.user).getBody());
    }

    @Test
    public void createDividend() {
        ETFDividendDTO edDTO = ETFDividendDTO.builder().dividendDate(new Date()).id(1L).amount(5.6).currencyId("eur").shortName("s1").exchange("e1").build();

        when(this.etfDividendService.createETFDividend(edDTO, this.user)).thenReturn(edDTO);

        Assertions.assertEquals(edDTO, this.etfDividendController.createDividend(this.user, edDTO).getBody());
    }

    @Test
    public void deleteByIds() {
        doNothing().when(this.etfDividendService).deleteETFDividendById(any(), any());

        Assertions.assertEquals(HttpStatus.OK, this.etfDividendController.deleteByIds(user, "1,2,3").getStatusCode());
    }

    @Test
    public void getCSV() throws IOException {
        HttpServletResponse httpSR = new MockHttpServletResponse();

        doNothing().when(this.etfDividendService).getCSV(any(), any());
        this.etfDividendController.getCSV(user, httpSR);

        verify(this.etfDividendService, times(1)).getCSV(any(), any());
    }

    @Test
    public void updateETFDividend() {
        ETFDividendDTO edDTO = ETFDividendDTO.builder().dividendDate(new Date()).id(1L).amount(5.6).currencyId("eur").shortName("s1").exchange("e1").build();

        when(this.etfDividendService.updateETFDividend(edDTO, this.user)).thenReturn(edDTO);

        Assertions.assertEquals(edDTO, this.etfDividendController.updateETFDividend(this.user, edDTO).getBody());
    }

    @Test
    public void processCSV() throws IOException {
        MultipartFile mpf = new MockMultipartFile("mpf", "mpf.csv", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());

        doNothing().when(this.etfDividendService).processCSV(user, mpf);

        Assertions.assertEquals(HttpStatus.CREATED, this.etfDividendController.processCSV(user, mpf).getStatusCode());
    }
}