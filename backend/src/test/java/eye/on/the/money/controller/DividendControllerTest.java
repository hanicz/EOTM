package eye.on.the.money.controller;

import eye.on.the.money.dto.out.DividendDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.stock.DividendService;
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
class DividendControllerTest {

    @Mock
    private DividendService dividendService;

    @InjectMocks
    private DividendController dividendController;

    private final User user = User.builder().id(1L).build();

    @Test
    public void getAllDividends() {
        List<DividendDTO> dividends = new ArrayList<>();
        dividends.add(DividendDTO.builder().dividendId(1L).exchange("e1").dividendDate(new Date()).amount(55.1).currencyId("c1").shortName("s1").build());
        dividends.add(DividendDTO.builder().dividendId(2L).exchange("e2").dividendDate(new Date()).amount(51.2).currencyId("c2").shortName("s2").build());
        dividends.add(DividendDTO.builder().dividendId(3L).exchange("e3").dividendDate(new Date()).amount(50.3).currencyId("c3").shortName("s3").build());

        when(this.dividendService.getDividends(1L)).thenReturn(dividends);

        Assertions.assertIterableEquals(dividends, this.dividendController.getAllDividends(this.user).getBody());
    }

    @Test
    public void createDividend() {
        DividendDTO dividendDTO = DividendDTO.builder().dividendId(1L).exchange("e1").dividendDate(new Date()).amount(55.1).currencyId("c1").shortName("s1").build();
        when(this.dividendService.createDividend(dividendDTO, this.user)).thenReturn(dividendDTO);

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
        DividendDTO dividendDTO = DividendDTO.builder().dividendId(1L).exchange("e1").dividendDate(new Date()).amount(55.1).currencyId("c1").shortName("s1").build();
        when(this.dividendService.updateDividend(dividendDTO, this.user)).thenReturn(dividendDTO);

        Assertions.assertEquals(dividendDTO, this.dividendController.updateDividend(this.user, dividendDTO).getBody());
    }

    @Test
    public void processCSV() throws IOException {
        MultipartFile mpf = new MockMultipartFile("mpf", "mpf.csv", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());

        doNothing().when(this.dividendService).processCSV(this.user, mpf);

        Assertions.assertEquals(HttpStatus.CREATED, this.dividendController.processCSV(this.user, mpf).getStatusCode());
    }
}