package eye.on.the.money.controller;

import eye.on.the.money.dto.in.ChangePasswordDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.user.UserServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import eye.on.the.money.security.CurrentUserEmail;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/v1/user")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserServiceImpl userService;

    @PostMapping("/signup")
    public ResponseEntity<Void> createNewUser(@RequestBody User user) {
        this.userService.signUp(user);
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
}
