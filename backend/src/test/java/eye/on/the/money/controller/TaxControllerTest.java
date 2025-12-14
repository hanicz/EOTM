package eye.on.the.money.controller;

import eye.on.the.money.service.shared.TaxService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.mockito.Mockito.doNothing;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class TaxControllerTest {

    @Mock
    private TaxService taxService;

    @InjectMocks
    private TaxController taxController;

    @Test
    void processMNBExcel() throws IOException {
        MultipartFile mpf = new MockMultipartFile("mpf", "mpf.csv", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());

        doNothing().when(this.taxService).loadRatesFromXLS(mpf);

        Assertions.assertEquals(HttpStatus.CREATED, this.taxController.processMNBExcel(mpf).getStatusCode());
    }
}