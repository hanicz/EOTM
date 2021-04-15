package eye.on.the.money.controller;

import eye.on.the.money.model.User;
import eye.on.the.money.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("user")
public class UserController {

    @Autowired
    private UserServiceImpl userService;

    @PostMapping("/signup")
    public ResponseEntity<HttpStatus> createNewPlan(@RequestBody User user) {
        this.userService.signUp(user);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
