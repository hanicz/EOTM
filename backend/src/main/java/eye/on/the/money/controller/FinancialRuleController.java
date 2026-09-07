package eye.on.the.money.controller;

import eye.on.the.money.dto.in.BankExclusionRuleEditDTO;
import eye.on.the.money.dto.out.BankExclusionRuleDTO;
import eye.on.the.money.security.CurrentUserId;
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
}
