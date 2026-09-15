package eye.on.the.money.service.financial;

import eye.on.the.money.dto.in.SpendingCategoryEditDTO;
import eye.on.the.money.dto.out.SpendingCategoryDTO;
import eye.on.the.money.exception.ValidationException;
import eye.on.the.money.model.User;
import eye.on.the.money.model.financial.BankCategoryRule;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Slf4j
@RequiredArgsConstructor
public class SpendingCategoryService {

    private static final String DUPLICATE_MESSAGE = "A category with this name already exists";

    private final SpendingCategoryRepository spendingCategoryRepository;
    private final BankCategoryRuleRepository bankCategoryRuleRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final UserService userService;

    public List<SpendingCategoryDTO> getCategories(Long userId) {
        return this.spendingCategoryRepository.findByUserIdOrderByPositionAscNameAsc(userId)
                .stream().map(this::convertToDTO).toList();
    }

    @Transactional
    public SpendingCategoryDTO createCategory(Long userId, SpendingCategoryEditDTO editDTO) {
        String name = editDTO.name().trim();
        String normalized = this.normalizeOrReject(name);
        this.rejectDuplicate(userId, normalized, null);

        SpendingCategory category = SpendingCategory.builder()
                .name(name)
                .normalizedName(normalized)
                .color(editDTO.color())
                .position(editDTO.position() == null ? this.nextPosition(userId) : editDTO.position())
                .user(this.userService.getReference(userId))
                .build();

        return this.convertToDTO(this.save(category));
    }

    @Transactional
    public SpendingCategoryDTO updateCategory(Long userId, Long id, SpendingCategoryEditDTO editDTO) {
        SpendingCategory category = this.spendingCategoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Spending category not found: " + id));

        String name = editDTO.name().trim();
        String normalized = this.normalizeOrReject(name);
        this.rejectDuplicate(userId, normalized, id);

        category.setName(name);
        category.setNormalizedName(normalized);
        category.setColor(editDTO.color());
        if (editDTO.position() != null) {
            category.setPosition(editDTO.position());
        }

        return this.convertToDTO(this.save(category));
    }

    @Transactional
    public void deleteCategoriesByIds(Long userId, List<Long> ids) {
        this.bankTransactionRepository.clearCategoryByUserIdAndCategoryIdIn(userId, ids);
        this.bankCategoryRuleRepository.deleteByUserIdAndCategoryIdIn(userId, ids);
        this.spendingCategoryRepository.deleteByUserIdAndIdIn(userId, ids);
    }

    @Transactional
    public List<SpendingCategoryDTO> createStarterSet(Long userId) {
        if (this.spendingCategoryRepository.countByUserId(userId) > 0) {
            throw new ValidationException("The starter set can only be added while there are no categories");
        }
        User user = this.userService.getReference(userId);
        List<BankCategoryRule> rules = new ArrayList<>();
        int position = 0;
        for (StarterCategories.Definition definition : StarterCategories.DEFINITIONS) {
            SpendingCategory category = this.spendingCategoryRepository.save(SpendingCategory.builder()
                    .name(definition.name())
                    .normalizedName(CategoryRuleMatcher.normalize(definition.name()))
                    .color(definition.color())
                    .position(position++)
                    .user(user)
                    .build());
            for (String pattern : definition.patterns()) {
                rules.add(BankCategoryRule.builder()
                        .name(definition.name())
                        .pattern(pattern)
                        .normalizedPattern(CategoryRuleMatcher.normalize(pattern))
                        .active(true)
                        .category(category)
                        .user(user)
                        .build());
            }
        }
        rules.sort(Comparator.comparingInt((BankCategoryRule rule) -> rule.getNormalizedPattern().length())
                .reversed().thenComparing(BankCategoryRule::getNormalizedPattern));
        int priority = 0;
        for (BankCategoryRule rule : rules) {
            rule.setPriority(priority++);
        }
        this.bankCategoryRuleRepository.saveAll(rules);
        log.debug("Created the starter category set, {} categories and {} rules",
                StarterCategories.DEFINITIONS.size(), rules.size());
        return this.getCategories(userId);
    }

    private int nextPosition(Long userId) {
        return this.spendingCategoryRepository.findByUserIdOrderByPositionAscNameAsc(userId).stream()
                .mapToInt(SpendingCategory::getPosition).max().orElse(-1) + 1;
    }

    private SpendingCategory save(SpendingCategory category) {
        try {
            return this.spendingCategoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException e) {
            log.info("Rejected duplicate spending category {}", category.getNormalizedName());
            throw new ValidationException(DUPLICATE_MESSAGE);
        }
    }

    private String normalizeOrReject(String name) {
        String normalized = CategoryRuleMatcher.normalize(name);
        if (normalized.isEmpty()) {
            throw new ValidationException("Category name must contain at least one character");
        }
        return normalized;
    }

    private void rejectDuplicate(Long userId, String normalized, Long selfId) {
        this.spendingCategoryRepository.findByUserIdAndNormalizedName(userId, normalized)
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new ValidationException(DUPLICATE_MESSAGE);
                });
    }

    private SpendingCategoryDTO convertToDTO(SpendingCategory category) {
        return SpendingCategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .color(category.getColor())
                .position(category.getPosition())
                .build();
    }
}
