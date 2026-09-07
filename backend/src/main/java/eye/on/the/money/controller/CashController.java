package eye.on.the.money.controller;

import eye.on.the.money.dto.out.CashDTO;
import eye.on.the.money.security.CurrentUserId;
import eye.on.the.money.service.cash.CashService;
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
@RequestMapping("api/v1/cash")
@Validated
@RequiredArgsConstructor
public class CashController {

    private final CashService cashService;

    @GetMapping
    public ResponseEntity<CashDTO> getCash(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.cashService.getCash(userId));
    }

    @PutMapping
    public ResponseEntity<CashDTO> updateCash(@CurrentUserId Long userId, @RequestBody @Valid CashDTO cashDTO) {
        return ResponseEntity.ok(this.cashService.updateCash(userId, cashDTO));
    }
}
