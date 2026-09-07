package eye.on.the.money.controller;

import eye.on.the.money.dto.in.RSUGrantEditDTO;
import eye.on.the.money.dto.out.RSUGrantDTO;
import eye.on.the.money.dto.out.RSUGrantReportDTO;
import eye.on.the.money.security.CurrentUserId;
import eye.on.the.money.service.stock.RSUGrantService;
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
@RequestMapping("api/v1/equity/rsu")
@Validated
@RequiredArgsConstructor
public class RSUGrantController {

    private final RSUGrantService rsuGrantService;

    @GetMapping()
    public ResponseEntity<RSUGrantReportDTO> getGrants(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.rsuGrantService.getGrants(userId));
    }

    @PostMapping()
    public ResponseEntity<RSUGrantDTO> createGrant(@CurrentUserId Long userId,
                                                   @RequestBody @Valid RSUGrantEditDTO editDTO) {
        return ResponseEntity.ok(this.rsuGrantService.createGrant(userId, editDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RSUGrantDTO> updateGrant(@CurrentUserId Long userId, @PathVariable Long id,
                                                   @RequestBody @Valid RSUGrantEditDTO editDTO) {
        return ResponseEntity.ok(this.rsuGrantService.updateGrant(userId, id, editDTO));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserId Long userId, @RequestParam List<Long> ids) {
        this.rsuGrantService.deleteGrantsByIds(userId, ids);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/csv")
    public void getCSV(@CurrentUserId Long userId, HttpServletResponse servletResponse) throws IOException {
        this.rsuGrantService.getCSV(userId, CsvResponseUtil.prepare(servletResponse, "rsu-grants.csv"));
    }
}
