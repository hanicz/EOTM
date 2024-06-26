package eye.on.the.money.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.dto.in.ChangePasswordDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.user.UserServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/v1/user")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserServiceImpl userService;
    private final ObjectMapper objectMapper;

    @PostMapping("/signup")
    public ResponseEntity<HttpStatus> createNewUser(@RequestBody User user) {
        this.userService.signUp(user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping()
    public ResponseEntity<HttpStatus> validatingToken(@AuthenticationPrincipal UserDetails user) {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/me")
    public ResponseEntity<String> getUserEmail(@AuthenticationPrincipal UserDetails user) throws JsonProcessingException {
        Map<String, String> map = new HashMap<>();
        map.put("email", user.getUsername());
        return new ResponseEntity<>(this.objectMapper.writeValueAsString(map), HttpStatus.OK);
    }

    @PutMapping("/password")
    public ResponseEntity<HttpStatus> changePassword(@RequestBody @Valid ChangePasswordDTO passwordDTO, @AuthenticationPrincipal UserDetails user) {
        this.userService.changePassword(user.getUsername(), passwordDTO);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
