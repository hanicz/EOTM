package eye.on.the.money.service.financial;

import eye.on.the.money.dto.in.BankExclusionRuleEditDTO;
import eye.on.the.money.dto.out.BankExclusionRuleDTO;
import eye.on.the.money.exception.ValidationException;
import eye.on.the.money.model.financial.AccountSide;
import eye.on.the.money.model.financial.BankExclusionRule;
import eye.on.the.money.repository.financial.BankExclusionRuleRepository;
import eye.on.the.money.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Slf4j
@RequiredArgsConstructor
public class BankExclusionRuleService {

    private static final String DUPLICATE_MESSAGE = "A rule already exists for this account number and side";

    private final BankExclusionRuleRepository bankExclusionRuleRepository;
    private final UserService userService;

    public List<BankExclusionRuleDTO> getRules(Long userId) {
        return this.bankExclusionRuleRepository.findByUserIdOrderByAccountNumberAsc(userId)
                .stream().map(this::convertToDTO).toList();
    }

    public ExclusionRuleMatcher matcherFor(Long userId) {
        return ExclusionRuleMatcher.of(this.bankExclusionRuleRepository.findByUserIdAndActiveTrue(userId));
    }

    @Transactional
    public BankExclusionRuleDTO createRule(Long userId, BankExclusionRuleEditDTO editDTO) {
        String accountNumber = editDTO.accountNumber().trim();
        String normalized = this.normalizeOrReject(accountNumber);
        this.rejectDuplicate(userId, normalized, editDTO.side(), null);

        BankExclusionRule rule = BankExclusionRule.builder()
                .name(this.trimToNull(editDTO.name()))
                .accountNumber(accountNumber)
                .normalizedAccount(normalized)
                .side(editDTO.side())
                .active(Boolean.TRUE.equals(editDTO.active()))
                .creationDate(LocalDate.now())
                .user(this.userService.getReference(userId))
                .build();

        return this.convertToDTO(this.save(rule));
    }

    @Transactional
    public BankExclusionRuleDTO updateRule(Long userId, Long id, BankExclusionRuleEditDTO editDTO) {
        BankExclusionRule rule = this.bankExclusionRuleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Exclusion rule not found: " + id));

        String accountNumber = editDTO.accountNumber().trim();
        String normalized = this.normalizeOrReject(accountNumber);
        this.rejectDuplicate(userId, normalized, editDTO.side(), id);

        rule.setName(this.trimToNull(editDTO.name()));
        rule.setAccountNumber(accountNumber);
        rule.setNormalizedAccount(normalized);
        rule.setSide(editDTO.side());
        rule.setActive(Boolean.TRUE.equals(editDTO.active()));

        return this.convertToDTO(this.save(rule));
    }

    @Transactional
    public void deleteRulesByIds(Long userId, List<Long> ids) {
        this.bankExclusionRuleRepository.deleteByUserIdAndIdIn(userId, ids);
    }

    private BankExclusionRule save(BankExclusionRule rule) {
        try {
            return this.bankExclusionRuleRepository.saveAndFlush(rule);
        } catch (DataIntegrityViolationException e) {
            log.info("Rejected duplicate exclusion rule for account {}", rule.getNormalizedAccount());
            throw new ValidationException(DUPLICATE_MESSAGE);
        }
    }

    private String trimToNull(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeOrReject(String accountNumber) {
        String normalized = ExclusionRuleMatcher.normalize(accountNumber);
        if (normalized.isEmpty()) {
            throw new ValidationException("Account number must contain at least one letter or digit");
        }
        return normalized;
    }

    private void rejectDuplicate(Long userId, String normalized, AccountSide side, Long selfId) {
        this.bankExclusionRuleRepository.findByUserIdAndNormalizedAccountAndSide(userId, normalized, side)
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new ValidationException(DUPLICATE_MESSAGE);
                });
    }

    private BankExclusionRuleDTO convertToDTO(BankExclusionRule rule) {
        return BankExclusionRuleDTO.builder()
                .id(rule.getId())
                .name(rule.getName())
                .accountNumber(rule.getAccountNumber())
                .side(rule.getSide())
                .active(rule.isActive())
                .build();
    }
}
