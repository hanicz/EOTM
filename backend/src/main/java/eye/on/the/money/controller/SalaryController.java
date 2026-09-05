package eye.on.the.money.controller;

import eye.on.the.money.dto.in.SalaryEditDTO;
import eye.on.the.money.dto.out.SalaryDTO;
import eye.on.the.money.dto.out.SalaryRaiseDTO;
import eye.on.the.money.security.CurrentUserId;
import eye.on.the.money.service.salary.SalaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/history/salary")
@Slf4j
@Validated
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryService salaryService;

    @GetMapping()
    public ResponseEntity<List<SalaryDTO>> getAllSalaries(@CurrentUserId Long userId) {
        log.trace("Enter");
        return ResponseEntity.ok(this.salaryService.getSalaries(userId));
    }

    @GetMapping("/raise")
    public ResponseEntity<SalaryRaiseDTO> getRaiseScenarios(@CurrentUserId Long userId) {
        log.trace("Enter");
        return this.salaryService.getRaiseScenarios(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping()
    public ResponseEntity<SalaryDTO> createSalary(@CurrentUserId Long userId,
                                                  @RequestBody @Valid SalaryEditDTO editDTO) {
        log.trace("Enter");
        return ResponseEntity.ok(this.salaryService.createSalary(userId, editDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalaryDTO> updateSalary(@CurrentUserId Long userId, @PathVariable Long id,
                                                  @RequestBody @Valid SalaryEditDTO editDTO) {
        log.trace("Enter");
        return ResponseEntity.ok(this.salaryService.updateSalary(userId, id, editDTO));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserId Long userId, @RequestParam List<Long> ids) {
        log.trace("Enter");
        this.salaryService.deleteSalariesByIds(userId, ids);
        return ResponseEntity.ok().build();
    }
}
