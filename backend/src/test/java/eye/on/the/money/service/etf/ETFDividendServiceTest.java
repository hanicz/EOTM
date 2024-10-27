package eye.on.the.money.service.etf;

import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.out.ETFDividendDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.etf.ETFDividend;
import eye.on.the.money.repository.UserRepository;
import eye.on.the.money.repository.etf.ETFDividendRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
class ETFDividendServiceTest {

    @Autowired
    private ETFDividendRepository etfDividendRepository;

    @Autowired
    private ETFDividendService etfDividendService;

    @Autowired
    private UserRepository userRepository;

    private User user;

    private final ModelMapper modelMapper = new ModelMapper();

    private final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @BeforeEach
    public void init() {
        this.user = this.userRepository.findByEmail("test@test.test");
    }


    @Test
    public void getDividends() {
        List<ETFDividendDTO> dividends = this.etfDividendService.getDividends(this.user.getUsername());
        List<ETFDividend> dividendsActual = this.etfDividendRepository.findByUserEmailOrderByDividendDate("test@test.test");
        Assertions.assertIterableEquals(dividendsActual.stream()
                .map(this::convertToETFDividendDTO).collect(Collectors.toList()), dividends);
    }

    @Test
    public void getDividends_NoResult() {
        List<ETFDividendDTO> dividends = this.etfDividendService.getDividends("nouseremail");
        assertEquals(0, dividends.size());
    }


    @Test
    public void createETFDividend() throws ParseException {
        ETFDividendDTO etfDividendDTO = this.getETFDividendDTO();
        ETFDividendDTO created = this.etfDividendService.createETFDividend(etfDividendDTO, this.user.getUsername());
        etfDividendDTO.setId(created.getId());
        assertEquals(etfDividendDTO, created);
    }

    private ETFDividendDTO getETFDividendDTO() throws ParseException {
        return ETFDividendDTO.builder()
                .id(1L)
                .amount(105.7)
                .dividendDate(LocalDate.parse("2021-07-03", FORMATTER))
                .shortName("VWRL")
                .currencyId("EUR")
                .exchange("AS")
                .build();
    }

    private ETFDividendDTO convertToETFDividendDTO(ETFDividend dividend) {
        return this.modelMapper.map(dividend, ETFDividendDTO.class);
    }
}