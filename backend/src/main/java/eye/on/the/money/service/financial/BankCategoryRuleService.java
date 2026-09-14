package eye.on.the.money.service.financial;

import eye.on.the.money.dto.in.BankCategoryRuleEditDTO;
import eye.on.the.money.dto.out.BankCategoryRuleDTO;
import eye.on.the.money.dto.out.CategorizeResultDTO;
import eye.on.the.money.exception.ValidationException;
import eye.on.the.money.model.financial.BankCategoryRule;
import eye.on.the.money.model.financial.BankTransaction;
import eye.on.the.money.model.financial.SpendingCategory;
import eye.on.the.money.repository.financial.BankCategoryRuleRepository;
import eye.on.the.money.repository.financial.BankTransactionRepository;
import eye.on.the.money.repository.financial.SpendingCategoryRepository;
import eye.on.the.money.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class BankCategoryRuleService {

    private static final String DUPLICATE_MESSAGE = "A rule already exists for this pattern";

    private final BankCategoryRuleRepository bankCategoryRuleRepository;
    private final SpendingCategoryRepository spendingCategoryRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final UserService userService;

    public List<BankCategoryRuleDTO> getRules(Long userId) {
        return this.bankCategoryRuleRepository.findByUserIdOrderByPriorityAscPatternAsc(userId)
                .stream().map(this::convertToDTO).toList();
    }

    public CategoryRuleMatcher matcherFor(Long userId) {
        return CategoryRuleMatcher.of(
                this.bankCategoryRuleRepository.findByUserIdAndActiveTrueOrderByPriorityAscPatternAsc(userId));
    }

    @Transactional
    public BankCategoryRuleDTO createRule(Long userId, BankCategoryRuleEditDTO editDTO) {
        String pattern = editDTO.pattern().trim();
        String normalized = this.normalizeOrReject(pattern);
        this.rejectDuplicate(userId, normalized, null);

        BankCategoryRule rule = BankCategoryRule.builder()
                .name(this.trimToNull(editDTO.name()))
                .pattern(pattern)
                .normalizedPattern(normalized)
                .category(this.category(userId, editDTO.categoryId()))
                .priority(editDTO.priority() == null ? this.nextPriority(userId) : editDTO.priority())
                .active(Boolean.TRUE.equals(editDTO.active()))
                .creationDate(LocalDate.now())
                .user(this.userService.getReference(userId))
                .build();

        return this.convertToDTO(this.save(rule));
    }

    @Transactional
    public BankCategoryRuleDTO updateRule(Long userId, Long id, BankCategoryRuleEditDTO editDTO) {
        BankCategoryRule rule = this.bankCategoryRuleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Category rule not found: " + id));

        String pattern = editDTO.pattern().trim();
        String normalized = this.normalizeOrReject(pattern);
        this.rejectDuplicate(userId, normalized, id);

        rule.setName(this.trimToNull(editDTO.name()));
        rule.setPattern(pattern);
        rule.setNormalizedPattern(normalized);
        rule.setCategory(this.category(userId, editDTO.categoryId()));
        rule.setActive(Boolean.TRUE.equals(editDTO.active()));
        if (editDTO.priority() != null) {
            rule.setPriority(editDTO.priority());
        }

        return this.convertToDTO(this.save(rule));
    }

    @Transactional
    public void deleteRulesByIds(Long userId, List<Long> ids) {
        this.bankCategoryRuleRepository.deleteByUserIdAndIdIn(userId, ids);
    }

    @Transactional
    public CategorizeResultDTO applyRules(Long userId) {
        CategoryRuleMatcher matcher = this.matcherFor(userId);
        List<BankTransaction> transactions = this.bankTransactionRepository
                .findByUserIdAndCategoryLockedFalse(userId);
        int categorized = 0;
        int cleared = 0;
        for (BankTransaction transaction : transactions) {
            SpendingCategory matched = matcher.match(transaction.getPartnerName());
            Long before = transaction.getCategory() == null ? null : transaction.getCategory().getId();
            Long after = matched == null ? null : matched.getId();
            if (Objects.equals(before, after)) {
                continue;
            }
            transaction.setCategory(matched);
            if (matched == null) {
                cleared++;
            } else {
                categorized++;
            }
        }
        this.bankTransactionRepository.saveAll(transactions);
        log.debug("Re-applied category rules, categorized {}, cleared {}", categorized, cleared);
        return CategorizeResultDTO.builder().categorized(categorized).cleared(cleared).build();
    }

    private SpendingCategory category(Long userId, Long categoryId) {
        return this.spendingCategoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new NoSuchElementException("Spending category not found: " + categoryId));
    }

    private int nextPriority(Long userId) {
        return this.bankCategoryRuleRepository.findByUserIdOrderByPriorityAscPatternAsc(userId).stream()
                .mapToInt(BankCategoryRule::getPriority).max().orElse(-1) + 1;
    }

    private BankCategoryRule save(BankCategoryRule rule) {
        try {
            return this.bankCategoryRuleRepository.saveAndFlush(rule);
        } catch (DataIntegrityViolationException e) {
            log.info("Rejected duplicate category rule for pattern {}", rule.getNormalizedPattern());
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

    private String normalizeOrReject(String pattern) {
        String normalized = CategoryRuleMatcher.normalize(pattern);
        if (normalized.isEmpty()) {
            throw new ValidationException("Pattern must contain at least one character");
        }
        return normalized;
    }

    private void rejectDuplicate(Long userId, String normalized, Long selfId) {
        this.bankCategoryRuleRepository.findByUserIdAndNormalizedPattern(userId, normalized)
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new ValidationException(DUPLICATE_MESSAGE);
                });
    }

    private BankCategoryRuleDTO convertToDTO(BankCategoryRule rule) {
        return BankCategoryRuleDTO.builder()
                .id(rule.getId())
                .name(rule.getName())
                .pattern(rule.getPattern())
                .categoryId(rule.getCategory().getId())
                .categoryName(rule.getCategory().getName())
                .categoryColor(rule.getCategory().getColor())
                .priority(rule.getPriority())
                .active(rule.isActive())
                .build();
    }
}
