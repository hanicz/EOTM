package eye.on.the.money.controller;

import eye.on.the.money.dto.in.BankCategoryRuleEditDTO;
import eye.on.the.money.dto.in.BankExclusionRuleEditDTO;
import eye.on.the.money.dto.out.BankCategoryRuleDTO;
import eye.on.the.money.dto.out.BankExclusionRuleDTO;
import eye.on.the.money.dto.out.CategorizeResultDTO;
import eye.on.the.money.security.CurrentUserId;
import eye.on.the.money.service.financial.BankCategoryRuleService;
import eye.on.the.money.service.financial.BankExclusionRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/financial/rule")
@Validated
@RequiredArgsConstructor
public class FinancialRuleController {

    private final BankExclusionRuleService bankExclusionRuleService;
    private final BankCategoryRuleService bankCategoryRuleService;

    @GetMapping()
    public ResponseEntity<List<BankExclusionRuleDTO>> getAllRules(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.bankExclusionRuleService.getRules(userId));
    }

    @PostMapping()
    public ResponseEntity<BankExclusionRuleDTO> createRule(@CurrentUserId Long userId,
                                                           @RequestBody @Valid BankExclusionRuleEditDTO editDTO) {
        return ResponseEntity.ok(this.bankExclusionRuleService.createRule(userId, editDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BankExclusionRuleDTO> updateRule(@CurrentUserId Long userId, @PathVariable Long id,
                                                           @RequestBody @Valid BankExclusionRuleEditDTO editDTO) {
        return ResponseEntity.ok(this.bankExclusionRuleService.updateRule(userId, id, editDTO));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserId Long userId, @RequestParam List<Long> ids) {
        this.bankExclusionRuleService.deleteRulesByIds(userId, ids);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/category")
    public ResponseEntity<List<BankCategoryRuleDTO>> getAllCategoryRules(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.bankCategoryRuleService.getRules(userId));
    }

    @PostMapping("/category")
    public ResponseEntity<BankCategoryRuleDTO> createCategoryRule(@CurrentUserId Long userId,
                                                                  @RequestBody @Valid BankCategoryRuleEditDTO editDTO) {
        return ResponseEntity.ok(this.bankCategoryRuleService.createRule(userId, editDTO));
    }

    @PutMapping("/category/{id}")
    public ResponseEntity<BankCategoryRuleDTO> updateCategoryRule(@CurrentUserId Long userId, @PathVariable Long id,
                                                                  @RequestBody @Valid BankCategoryRuleEditDTO editDTO) {
        return ResponseEntity.ok(this.bankCategoryRuleService.updateRule(userId, id, editDTO));
    }

    @DeleteMapping("/category")
    public ResponseEntity<Void> deleteCategoryRulesByIds(@CurrentUserId Long userId, @RequestParam List<Long> ids) {
        this.bankCategoryRuleService.deleteRulesByIds(userId, ids);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/category/apply")
    public ResponseEntity<CategorizeResultDTO> applyCategoryRules(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.bankCategoryRuleService.applyRules(userId));
    }
}
