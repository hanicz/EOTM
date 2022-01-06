package eye.on.the.money.service.impl;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.DividendDTO;
import eye.on.the.money.model.stock.Dividend;
import eye.on.the.money.repository.DividendRepository;
import eye.on.the.money.service.DividendService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EotmApplication.class)
class DividendServiceImplTest {

    @Autowired
    private DividendRepository dividendRepository;

    @Autowired
    private DividendService dividendService;

    @Test
    public void getDividends() {
        List<DividendDTO> dividends = this.dividendService.getDividends(1L);
        List<Dividend> dividendsActual = this.dividendRepository.findByUser_IdOrderByDividendDate(1L);
        assertEquals(dividendsActual.size(), dividends.size());
    }
}