package eye.on.the.money.service.security;

import eye.on.the.money.dto.out.InterestDTO;
import eye.on.the.money.exception.CSVException;
import eye.on.the.money.model.Currency;
import eye.on.the.money.model.User;
import eye.on.the.money.model.security.Interest;
import eye.on.the.money.model.security.Security;
import eye.on.the.money.repository.forex.CurrencyRepository;
import eye.on.the.money.repository.security.InterestRepository;
import eye.on.the.money.service.shared.ICSVService;
import eye.on.the.money.service.user.UserServiceImpl;
import eye.on.the.money.util.DateFormats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.Writer;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterestService implements ICSVService {

    private final InterestRepository interestRepository;
    private final CurrencyRepository currencyRepository;
    private final SecurityService securityService;
    private final UserServiceImpl userService;
    private final ModelMapper modelMapper;
    public List<InterestDTO> getInterest(String userEmail) {
        return this.interestRepository.findByUserEmailOrderByInterestDateDesc(userEmail).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private InterestDTO convertToDTO(Interest interest) {
        return this.modelMapper.map(interest, InterestDTO.class);
    }

    @Transactional
    public InterestDTO createInterest(InterestDTO interestDTO, String userEmail) {
        Currency currency = this.currencyRepository.findById(interestDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        Security security = this.securityService.getOrCreateSecurity(interestDTO.getSecurityId(), interestDTO.getSecurityName());
        User user = this.userService.loadUserByEmail(userEmail);

        Interest interest = Interest.builder()
                .amount(interestDTO.getAmount())
                .currency(currency)
                .security(security)
                .interestDate(interestDTO.getInterestDate())
                .user(user)
                .build();

        interest = this.interestRepository.save(interest);
        return this.convertToDTO(interest);
    }

    @Transactional
    public InterestDTO updateInterest(InterestDTO interestDTO, String userEmail) {
        Currency currency = this.currencyRepository.findById(interestDTO.getCurrencyId()).orElseThrow(NoSuchElementException::new);
        Security security = this.securityService.getOrCreateSecurity(interestDTO.getSecurityId(), interestDTO.getSecurityName());
        Interest interest = this.interestRepository.findByIdAndUserEmail(interestDTO.getInterestId(), userEmail).orElseThrow(NoSuchElementException::new);

        interest.setInterestDate(interestDTO.getInterestDate());
        interest.setCurrency(currency);
        interest.setSecurity(security);
        interest.setAmount(interestDTO.getAmount());

        return this.convertToDTO(interest);
    }

    @Transactional
    public void deleteInterestById(String ids, String userEmail) {
        List<Long> idList = Stream.of(ids.split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        this.interestRepository.deleteByUserEmailAndIdIn(userEmail, idList);
    }

    public void getCSV(String userEmail, Writer writer) {
        List<InterestDTO> interestList =
                this.interestRepository.findByUserEmailOrderByInterestDateDesc(userEmail)
                        .stream()
                        .map(this::convertToDTO)
                        .toList();
        this.printRecords(interestList, writer);
    }

    @Transactional
    public void processCSV(String userEmail, MultipartFile file) {
        try (CSVParser csvParser = this.getParser(file,
                new String[]{"Interest Id", "Amount", "Interest Date", "Security Id", "Security Name", "Currency"})) {
            for (CSVRecord csvRecord : csvParser) {
                InterestDTO interest = InterestDTO.createFromCSVRecord(csvRecord, DateFormats.YYYY_MM_DD);

                if (interest.getInterestId() != null &&
                        this.interestRepository.findByIdAndUserEmail(interest.getInterestId(), userEmail).isPresent()) {
                    this.updateInterest(interest, userEmail);
                } else {
                    interest.setInterestId(null);
                    this.createInterest(interest, userEmail);
                }
            }
        } catch (IOException | DateTimeParseException | IllegalArgumentException e) {
            log.error("Error while processing CSV", e);
            throw new CSVException("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }
}
