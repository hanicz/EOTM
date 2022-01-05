package eye.on.the.money.controller;

import eye.on.the.money.model.User;
import eye.on.the.money.service.impl.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("user")
public class UserController {

    @Autowired
    private UserServiceImpl userService;

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/signup")
    public ResponseEntity<HttpStatus> createNewUser(@RequestBody User user) {
        log.trace("Enter createNewUser");
        this.userService.signUp(user);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping()
    public ResponseEntity<HttpStatus> validatingToken(@AuthenticationPrincipal User user) {
        log.trace("Enter validatingToken");
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
