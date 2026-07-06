package eye.on.the.money.controller;

import eye.on.the.money.dto.in.ChangePasswordDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.user.UserServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserServiceImpl userService;

    private UserController userController;

    @BeforeEach
    public void setUp() {
        this.userController = new UserController(this.userService);
    }

    private final User user = User.builder().id(1L).email("email").build();

    @Test
    void createNewUser() {
        doNothing().when(this.userService).signUp(this.user);

        Assertions.assertEquals(HttpStatus.OK, this.userController.createNewUser(this.user).getStatusCode());
        verify(this.userService, times(1)).signUp(this.user);
    }

    @Test
    void validatingToken() {
        Assertions.assertEquals(HttpStatus.OK, this.userController.validatingToken().getStatusCode());
    }

    @Test
    void changePassword() {
        doNothing().when(this.userService).changePassword(anyString(), any(ChangePasswordDTO.class));

        Assertions.assertEquals(HttpStatus.OK, this.userController.changePassword(new ChangePasswordDTO("old", "new"), "email").getStatusCode());
        verify(this.userService, times(1)).changePassword(anyString(), any(ChangePasswordDTO.class));
    }

    @Test
    void getUserEmail() {
        Assertions.assertEquals(Map.of("email", this.user.getUsername()), this.userController.getUserEmail("email").getBody());
    }
}