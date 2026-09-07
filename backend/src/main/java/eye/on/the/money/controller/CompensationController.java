package eye.on.the.money.controller;

import eye.on.the.money.dto.in.CompensationCompareDTO;
import eye.on.the.money.dto.in.CompensationEditDTO;
import eye.on.the.money.dto.out.CompensationItemDTO;
import eye.on.the.money.dto.out.CompensationPackageDTO;
import eye.on.the.money.security.CurrentUserId;
import eye.on.the.money.service.salary.CompensationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/history/compensation")
@Validated
@RequiredArgsConstructor
public class CompensationController {

    private final CompensationService compensationService;

    @GetMapping()
    public ResponseEntity<CompensationPackageDTO> getCurrentPackage(@CurrentUserId Long userId) {
        return this.compensationService.getCurrentPackage(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/compare")
    public ResponseEntity<CompensationPackageDTO> comparePackage(@RequestBody @Valid CompensationCompareDTO compareDTO) {
        return ResponseEntity.ok(this.compensationService.comparePackage(compareDTO));
    }

    @PostMapping()
    public ResponseEntity<CompensationItemDTO> createItem(@CurrentUserId Long userId,
                                                          @RequestBody @Valid CompensationEditDTO editDTO) {
        return ResponseEntity.ok(this.compensationService.createItem(userId, editDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompensationItemDTO> updateItem(@CurrentUserId Long userId, @PathVariable Long id,
                                                          @RequestBody @Valid CompensationEditDTO editDTO) {
        return ResponseEntity.ok(this.compensationService.updateItem(userId, id, editDTO));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserId Long userId, @RequestParam List<Long> ids) {
        this.compensationService.deleteItemsByIds(userId, ids);
        return ResponseEntity.ok().build();
    }
}
