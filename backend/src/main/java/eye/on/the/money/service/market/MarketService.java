package eye.on.the.money.service.market;

import eye.on.the.money.dto.out.MarketExchangeDTO;
import eye.on.the.money.dto.out.MarketHolidayDTO;
import eye.on.the.money.model.market.MarketExchange;
import eye.on.the.money.model.market.MarketHoliday;
import eye.on.the.money.repository.market.MarketExchangeRepository;
import eye.on.the.money.repository.market.MarketHolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketService {

    private final MarketExchangeRepository marketExchangeRepository;
    private final MarketHolidayRepository marketHolidayRepository;

    public List<MarketExchangeDTO> getExchanges() {
        log.trace("Enter getExchanges");
        Map<String, List<MarketHolidayDTO>> holidaysByExchange =
                this.marketHolidayRepository.findByHolidayDateGreaterThanEqualOrderByHolidayDateAsc(LocalDate.now())
                        .stream()
                        .collect(Collectors.groupingBy(holiday -> holiday.getExchange().getId(),
                                Collectors.mapping(this::convertToDTO, Collectors.toList())));

        return this.marketExchangeRepository.findAllByOrderByIdAsc().stream()
                .map(exchange -> this.convertToDTO(exchange,
                        holidaysByExchange.getOrDefault(exchange.getId(), Collections.emptyList())))
                .toList();
    }

    private MarketExchangeDTO convertToDTO(MarketExchange exchange, List<MarketHolidayDTO> holidays) {
        return MarketExchangeDTO.builder()
                .code(exchange.getId())
                .name(exchange.getName())
                .timeZone(exchange.getTimeZone())
                .currency(exchange.getCurrency())
                .countryISO2(exchange.getCountryISO2())
                .openTime(exchange.getOpenTime())
                .closeTime(exchange.getCloseTime())
                .holidays(holidays)
                .build();
    }

    private MarketHolidayDTO convertToDTO(MarketHoliday holiday) {
        return MarketHolidayDTO.builder()
                .holidayDate(holiday.getHolidayDate())
                .name(holiday.getName())
                .closeTime(holiday.getCloseTime())
                .build();
    }
}
