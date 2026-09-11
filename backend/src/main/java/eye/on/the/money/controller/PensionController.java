package eye.on.the.money.controller;

import eye.on.the.money.dto.out.PensionDTO;
import eye.on.the.money.security.CurrentUserId;
import eye.on.the.money.service.pension.PensionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/pension")
@Validated
@RequiredArgsConstructor
public class PensionController {

    private final PensionService pensionService;

    @GetMapping
    public ResponseEntity<PensionDTO> getPension(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.pensionService.getPension(userId));
    }

    @PutMapping
    public ResponseEntity<PensionDTO> updatePension(@CurrentUserId Long userId,
                                                    @RequestBody @Valid PensionDTO pensionDTO) {
        return ResponseEntity.ok(this.pensionService.updatePension(userId, pensionDTO));
    }
}
