package eye.on.the.money.controller;

import eye.on.the.money.dto.out.DividendDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.stock.DividendService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class DividendControllerTest {

    @Mock
    private DividendService dividendService;

    @InjectMocks
    private DividendController dividendController;

    @Test
    public void getAllDividends() {
        List<DividendDTO> dividends = new ArrayList<>();
        dividends.add(DividendDTO.builder().dividendId(1L).exchange("e1").dividendDate(new Date()).amount(55.1).currencyId("c1").shortName("s1").build());
        dividends.add(DividendDTO.builder().dividendId(2L).exchange("e2").dividendDate(new Date()).amount(51.2).currencyId("c2").shortName("s2").build());
        dividends.add(DividendDTO.builder().dividendId(3L).exchange("e3").dividendDate(new Date()).amount(50.3).currencyId("c3").shortName("s3").build());

        when(this.dividendService.getDividends(1L)).thenReturn(dividends);

        Assertions.assertIterableEquals(dividends, this.dividendController.getAllDividends(User.builder().id(1L).build()).getBody());
    }
}