package eye.on.the.money.controller;

import eye.on.the.money.dto.in.ChangePasswordDTO;
import eye.on.the.money.dto.in.PreferencesUpdateDTO;
import eye.on.the.money.dto.in.SignUpDTO;
import eye.on.the.money.dto.in.TotpCodeDTO;
import eye.on.the.money.dto.in.TotpDisableDTO;
import eye.on.the.money.dto.out.ExportDTO;
import eye.on.the.money.dto.out.TotpSetupDTO;
import eye.on.the.money.dto.out.TotpStatusDTO;
import eye.on.the.money.dto.out.UserDTO;
import eye.on.the.money.service.shared.ExportService;
import eye.on.the.money.service.user.TotpService;
import eye.on.the.money.service.user.UserService;
import eye.on.the.money.util.DateFormats;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import eye.on.the.money.security.CurrentUserId;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ExportService exportService;
    private final TotpService totpService;

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
    public ResponseEntity<UserDTO> getCurrentUser(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.userService.getUser(userId));
    }

    @PutMapping("/preferences")
    public ResponseEntity<UserDTO> updatePreferences(@RequestBody @Valid PreferencesUpdateDTO preferencesDTO,
                                                     @CurrentUserId Long userId) {
        return ResponseEntity.ok(this.userService.updatePreferredCurrency(userId, preferencesDTO.preferredCurrency()));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@RequestBody @Valid ChangePasswordDTO passwordDTO, @CurrentUserId Long userId) {
        this.userService.changePassword(userId, passwordDTO);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/2fa")
    public ResponseEntity<TotpStatusDTO> twoFactorStatus(@CurrentUserId Long userId) {
        return ResponseEntity.ok(new TotpStatusDTO(this.totpService.isEnabled(userId)));
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<TotpSetupDTO> startTwoFactorSetup(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.totpService.startEnrolment(userId));
    }

    @PostMapping("/2fa/confirm")
    public ResponseEntity<Void> confirmTwoFactor(@RequestBody @Valid TotpCodeDTO codeDTO, @CurrentUserId Long userId) {
        this.totpService.confirmEnrolment(userId, codeDTO.code());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/2fa")
    public ResponseEntity<Void> disableTwoFactor(@RequestBody @Valid TotpDisableDTO disableDTO, @CurrentUserId Long userId) {
        this.totpService.disable(userId, disableDTO.password());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    public ResponseEntity<ExportDTO> export(@CurrentUserId Long userId) {
        String filename = "eotm-export-" + LocalDate.now().format(DateFormats.YYYY_MM_DD) + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(this.exportService.export(userId));
    }
}
