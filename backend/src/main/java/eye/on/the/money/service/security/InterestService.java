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
import eye.on.the.money.service.user.UserService;
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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterestService implements ICSVService {

    private final InterestRepository interestRepository;
    private final CurrencyRepository currencyRepository;
    private final SecurityService securityService;
    private final UserService userService;
    private final ModelMapper modelMapper;

    public List<InterestDTO> getInterest(Long userId) {
        return this.interestRepository.findByUserIdOrderByInterestDateDesc(userId).stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<InterestDTO> getInterestBetween(Long userId, LocalDate from, LocalDate to) {
        return this.interestRepository.findByUserIdAndInterestDateBetweenOrderByInterestDate(userId, from, to)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private InterestDTO convertToDTO(Interest interest) {
        return this.modelMapper.map(interest, InterestDTO.class);
    }

    @Transactional
    public InterestDTO createInterest(InterestDTO interestDTO, Long userId) {
        Currency currency = this.currencyRepository.findById(interestDTO.getCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + interestDTO.getCurrencyId()));
        Security security = this.securityService.getOrCreateSecurity(interestDTO.getSecurityId(), interestDTO.getSecurityName());
        User user = this.userService.getReference(userId);

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
    public InterestDTO updateInterest(InterestDTO interestDTO, Long userId) {
        Currency currency = this.currencyRepository.findById(interestDTO.getCurrencyId()).orElseThrow(() -> new NoSuchElementException("Currency not found: " + interestDTO.getCurrencyId()));
        Security security = this.securityService.getOrCreateSecurity(interestDTO.getSecurityId(), interestDTO.getSecurityName());
        Interest interest = this.interestRepository.findByIdAndUserId(interestDTO.getInterestId(), userId).orElseThrow(() -> new NoSuchElementException("Interest not found: " + interestDTO.getInterestId()));

        interest.setInterestDate(interestDTO.getInterestDate());
        interest.setCurrency(currency);
        interest.setSecurity(security);
        interest.setAmount(interestDTO.getAmount());

        return this.convertToDTO(interest);
    }

    @Transactional
    public void deleteInterestById(List<Long> ids, Long userId) {
        this.interestRepository.deleteByUserIdAndIdIn(userId, ids);
    }

    public void getCSV(Long userId, Writer writer) {
        List<InterestDTO> interestList =
                this.interestRepository.findByUserIdOrderByInterestDateDesc(userId)
                        .stream()
                        .map(this::convertToDTO)
                        .toList();
        this.printRecords(interestList, writer);
    }

    @Transactional
    public void processCSV(Long userId, MultipartFile file) {
        try (CSVParser csvParser = this.getParser(file,
                new String[]{"Interest Id", "Amount", "Interest Date", "Security Id", "Security Name", "Currency"})) {
            for (CSVRecord csvRecord : csvParser) {
                InterestDTO interest = InterestDTO.createFromCSVRecord(csvRecord, DateFormats.YYYY_MM_DD);

                if (interest.getInterestId() != null &&
                        this.interestRepository.findByIdAndUserId(interest.getInterestId(), userId).isPresent()) {
                    this.updateInterest(interest, userId);
                } else {
                    interest.setInterestId(null);
                    this.createInterest(interest, userId);
                }
            }
        } catch (IOException | DateTimeParseException | IllegalArgumentException e) {
            log.error("Error while processing CSV", e);
            throw new CSVException("Failed to parse CSV file: " + e.getMessage(), e);
        }
    }
}
