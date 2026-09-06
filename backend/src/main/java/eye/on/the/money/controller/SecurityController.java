package eye.on.the.money.controller;

import eye.on.the.money.dto.out.ImportResultDTO;
import eye.on.the.money.model.security.Security;
import eye.on.the.money.security.CurrentUserId;
import eye.on.the.money.service.security.SecurityRateService;
import eye.on.the.money.service.security.SecurityService;
import eye.on.the.money.service.security.WebkincstarImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("api/v1/security")
@Slf4j
@RequiredArgsConstructor
public class SecurityController {

    private final SecurityService securityService;
    private final SecurityRateService securityRateService;
    private final WebkincstarImportService webkincstarImportService;

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

    @PostMapping("/process/xls")
    public ResponseEntity<ImportResultDTO> processXls(@CurrentUserId Long userId,
                                                     @RequestParam("file") MultipartFile file) {
        log.trace("Enter");
        return ResponseEntity.ok(this.webkincstarImportService.processXls(userId, file));
    }
}
