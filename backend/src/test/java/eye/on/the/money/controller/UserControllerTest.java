package eye.on.the.money.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserServiceImpl userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserController userController;

    @BeforeEach
    public void setUp() {
        this.userController = new UserController(this.userService, this.objectMapper);
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
        Assertions.assertEquals(HttpStatus.OK, this.userController.validatingToken(this.user).getStatusCode());
    }

    @Test
    void changePassword() {
        doNothing().when(this.userService).changePassword(anyString(), any(ChangePasswordDTO.class));

        Assertions.assertEquals(HttpStatus.OK, this.userController.changePassword(new ChangePasswordDTO("old", "new"), this.user).getStatusCode());
        verify(this.userService, times(1)).changePassword(anyString(), any(ChangePasswordDTO.class));
    }

    @Test
    void getUserEmail() throws JsonProcessingException {
        Assertions.assertEquals("{\"email\":\"" + this.user.getUsername() + "\"}", this.userController.getUserEmail(this.user).getBody());
    }
}