package eye.on.the.money.controller;

import eye.on.the.money.dto.in.STARGrantEditDTO;
import eye.on.the.money.dto.out.STARGrantDTO;
import eye.on.the.money.dto.out.STARGrantReportDTO;
import eye.on.the.money.security.CurrentUserId;
import eye.on.the.money.service.stock.STARGrantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/equity/star")
@Validated
@RequiredArgsConstructor
public class STARGrantController {

    private final STARGrantService starGrantService;

    @GetMapping()
    public ResponseEntity<STARGrantReportDTO> getGrants(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.starGrantService.getGrants(userId));
    }

    @PostMapping()
    public ResponseEntity<STARGrantDTO> createGrant(@CurrentUserId Long userId,
                                                    @RequestBody @Valid STARGrantEditDTO editDTO) {
        return ResponseEntity.ok(this.starGrantService.createGrant(userId, editDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<STARGrantDTO> updateGrant(@CurrentUserId Long userId, @PathVariable Long id,
                                                    @RequestBody @Valid STARGrantEditDTO editDTO) {
        return ResponseEntity.ok(this.starGrantService.updateGrant(userId, id, editDTO));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserId Long userId, @RequestParam List<Long> ids) {
        this.starGrantService.deleteGrantsByIds(userId, ids);
        return ResponseEntity.ok().build();
    }
}
