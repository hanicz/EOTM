package eye.on.the.money.controller;

import eye.on.the.money.model.security.Security;
import eye.on.the.money.service.security.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/security")
@Slf4j
@RequiredArgsConstructor
public class SecurityController {

    private final SecurityService securityService;

    @GetMapping()
    public ResponseEntity<List<Security>> getAllSecurities() {
        log.trace("Enter");
        return new ResponseEntity<>(this.securityService.getAllSecurities(), HttpStatus.OK);
    }
}
