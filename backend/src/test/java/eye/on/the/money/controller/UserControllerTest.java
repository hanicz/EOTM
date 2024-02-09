package eye.on.the.money.controller;

import eye.on.the.money.model.User;
import eye.on.the.money.service.user.UserServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class UserControllerTest {

    @Mock
    private UserServiceImpl userService;

    @InjectMocks
    private UserController userController;

    private final User user = User.builder().id(1L).build();

    @Test
    void createNewUser() {
        doNothing().when(this.userService).signUp(this.user);

        Assertions.assertEquals(HttpStatus.OK, this.userController.createNewUser(this.user).getStatusCode());
        verify(this.userService, times(1)).signUp(this.user);
    }

    @Test
    void validatingToken() {
        Assertions.assertEquals(HttpStatus.OK, this.userController.validatingToken(this.user).getStatusCode());
    }
}