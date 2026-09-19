package eye.on.the.money.controller;

import eye.on.the.money.dto.in.PensionProjectionDTO;
import eye.on.the.money.dto.in.SalaryEditDTO;
import eye.on.the.money.dto.out.PensionProjectionResultDTO;
import eye.on.the.money.dto.out.SalaryDTO;
import eye.on.the.money.dto.out.SalaryRaiseDTO;
import eye.on.the.money.security.CurrentUserId;
import eye.on.the.money.service.salary.PensionProjectionService;
import eye.on.the.money.service.salary.SalaryService;
import eye.on.the.money.util.CsvResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/v1/history/salary")
@Validated
@RequiredArgsConstructor
public class SalaryController {

    private final SalaryService salaryService;
    private final PensionProjectionService pensionProjectionService;

    @GetMapping()
    public ResponseEntity<List<SalaryDTO>> getAllSalaries(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.salaryService.getSalaries(userId));
    }

    @GetMapping("/raise")
    public ResponseEntity<SalaryRaiseDTO> getRaiseScenarios(@CurrentUserId Long userId) {
        return this.salaryService.getRaiseScenarios(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping()
    public ResponseEntity<SalaryDTO> createSalary(@CurrentUserId Long userId,
                                                  @RequestBody @Valid SalaryEditDTO editDTO) {
        return ResponseEntity.ok(this.salaryService.createSalary(userId, editDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalaryDTO> updateSalary(@CurrentUserId Long userId, @PathVariable Long id,
                                                  @RequestBody @Valid SalaryEditDTO editDTO) {
        return ResponseEntity.ok(this.salaryService.updateSalary(userId, id, editDTO));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserId Long userId, @RequestParam List<Long> ids) {
        this.salaryService.deleteSalariesByIds(userId, ids);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pension")
    public ResponseEntity<PensionProjectionResultDTO> projectPension(
            @CurrentUserId Long userId, @RequestBody @Valid PensionProjectionDTO projectionDTO) {
        return ResponseEntity.ok(this.pensionProjectionService.project(userId, projectionDTO));
    }

    @PostMapping("/pension/csv")
    public void getPensionCSV(@CurrentUserId Long userId, @RequestBody @Valid PensionProjectionDTO projectionDTO,
                              HttpServletResponse servletResponse) throws IOException {
        this.pensionProjectionService.getCSV(userId, projectionDTO,
                CsvResponseUtil.prepare(servletResponse, "pension-projection.csv"));
    }
}
