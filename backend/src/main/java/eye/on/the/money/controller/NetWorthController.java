package eye.on.the.money.controller;

import eye.on.the.money.dto.out.NetWorthDTO;
import eye.on.the.money.dto.out.NetWorthHistoryDTO;
import eye.on.the.money.security.CurrentUserId;
import eye.on.the.money.service.shared.NetWorthService;
import eye.on.the.money.service.shared.NetWorthSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/networth")
@RequiredArgsConstructor
public class NetWorthController {

    private final NetWorthService netWorthService;
    private final NetWorthSnapshotService netWorthSnapshotService;

    @GetMapping
    public ResponseEntity<NetWorthDTO> getNetWorth(@CurrentUserId Long userId,
                                                   @RequestParam(required = false) String currency,
                                                   @RequestParam(defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(this.netWorthService.getNetWorth(userId, currency, refresh));
    }

    @GetMapping("/history")
    public ResponseEntity<NetWorthHistoryDTO> getHistory(@CurrentUserId Long userId,
                                                         @RequestParam(defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(this.netWorthSnapshotService.getHistory(userId, refresh));
    }
}
