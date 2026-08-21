package eye.on.the.money.controller;

import eye.on.the.money.dto.out.NetWorthDTO;
import eye.on.the.money.security.CurrentUserEmail;
import eye.on.the.money.service.shared.NetWorthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/networth")
@Slf4j
@RequiredArgsConstructor
public class NetWorthController {

    private final NetWorthService netWorthService;

    @GetMapping
    public ResponseEntity<NetWorthDTO> getNetWorth(@CurrentUserEmail String userEmail,
                                                   @RequestParam(required = false) String currency) {
        log.trace("Enter");
        return ResponseEntity.ok(this.netWorthService.getNetWorth(userEmail, currency));
    }
}
