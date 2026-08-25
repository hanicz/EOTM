package eye.on.the.money.controller;

import eye.on.the.money.model.security.Security;
import eye.on.the.money.service.security.SecurityRateService;
import eye.on.the.money.service.security.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/security")
@Slf4j
@RequiredArgsConstructor
public class SecurityController {

    private final SecurityService securityService;
    private final SecurityRateService securityRateService;

    @GetMapping()
    public ResponseEntity<List<Security>> getAllSecurities() {
        log.trace("Enter");
        return ResponseEntity.ok(this.securityService.getAllSecurities());
    }

    @PostMapping("/rate/refresh")
    public ResponseEntity<Void> refreshRates() {
        log.trace("Enter");
        this.securityRateService.refresh();
        return ResponseEntity.noContent().build();
    }
}
