package eye.on.the.money.controller;

import eye.on.the.money.dto.in.SpendingCategoryEditDTO;
import eye.on.the.money.dto.out.SpendingCategoryDTO;
import eye.on.the.money.security.CurrentUserId;
import eye.on.the.money.service.financial.SpendingCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/financial/category")
@Validated
@RequiredArgsConstructor
public class FinancialCategoryController {

    private final SpendingCategoryService spendingCategoryService;

    @GetMapping()
    public ResponseEntity<List<SpendingCategoryDTO>> getAllCategories(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.spendingCategoryService.getCategories(userId));
    }

    @PostMapping()
    public ResponseEntity<SpendingCategoryDTO> createCategory(@CurrentUserId Long userId,
                                                              @RequestBody @Valid SpendingCategoryEditDTO editDTO) {
        return ResponseEntity.ok(this.spendingCategoryService.createCategory(userId, editDTO));
    }

    @PostMapping("/starter")
    public ResponseEntity<List<SpendingCategoryDTO>> createStarterSet(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.spendingCategoryService.createStarterSet(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SpendingCategoryDTO> updateCategory(@CurrentUserId Long userId, @PathVariable Long id,
                                                              @RequestBody @Valid SpendingCategoryEditDTO editDTO) {
        return ResponseEntity.ok(this.spendingCategoryService.updateCategory(userId, id, editDTO));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserId Long userId, @RequestParam List<Long> ids) {
        this.spendingCategoryService.deleteCategoriesByIds(userId, ids);
        return ResponseEntity.ok().build();
    }
}
