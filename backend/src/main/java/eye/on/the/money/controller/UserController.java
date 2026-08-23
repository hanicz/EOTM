package eye.on.the.money.controller;

import eye.on.the.money.dto.in.ChangePasswordDTO;
import eye.on.the.money.dto.in.SignUpDTO;
import eye.on.the.money.dto.out.ExportDTO;
import eye.on.the.money.service.shared.ExportService;
import eye.on.the.money.service.user.UserService;
import eye.on.the.money.util.DateFormats;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import eye.on.the.money.security.CurrentUserEmail;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/v1/user")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ExportService exportService;

    @PostMapping("/signup")
    public ResponseEntity<Void> createNewUser(@RequestBody @Valid SignUpDTO signUpDTO) {
        this.userService.signUp(signUpDTO);
        return ResponseEntity.ok().build();
    }

    @GetMapping()
    public ResponseEntity<Void> validatingToken() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> getUserEmail(@CurrentUserEmail String userEmail) {
        Map<String, String> map = new HashMap<>();
        map.put("email", userEmail);
        return ResponseEntity.ok(map);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@RequestBody @Valid ChangePasswordDTO passwordDTO, @CurrentUserEmail String userEmail) {
        this.userService.changePassword(userEmail, passwordDTO);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/export")
    public ResponseEntity<ExportDTO> export(@CurrentUserEmail String userEmail) {
        log.trace("Enter");
        String filename = "eotm-export-" + LocalDate.now().format(DateFormats.YYYY_MM_DD) + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(this.exportService.export(userEmail));
    }
}
