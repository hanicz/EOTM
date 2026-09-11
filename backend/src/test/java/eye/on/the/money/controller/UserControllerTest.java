package eye.on.the.money.controller;

import eye.on.the.money.dto.in.ChangePasswordDTO;
import eye.on.the.money.dto.in.PreferencesUpdateDTO;
import eye.on.the.money.dto.in.SignUpDTO;
import eye.on.the.money.dto.out.UserDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.shared.ExportService;
import eye.on.the.money.service.user.UserService;
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
    private UserService userService;

    @Mock
    private ExportService exportService;

    private UserController userController;

    @BeforeEach
    public void setUp() {
        this.userController = new UserController(this.userService, this.exportService);
    }

    private final User user = User.builder().id(1L).email("email").build();

    @Test
    void createNewUser() {
        SignUpDTO signUpDTO = new SignUpDTO("new@mail.com", "password123");
        doNothing().when(this.userService).signUp(signUpDTO);

        Assertions.assertEquals(HttpStatus.OK, this.userController.createNewUser(signUpDTO).getStatusCode());
        verify(this.userService, times(1)).signUp(signUpDTO);
    }

    @Test
    void validatingToken() {
        Assertions.assertEquals(HttpStatus.OK, this.userController.validatingToken().getStatusCode());
    }

    @Test
    void changePassword() {
        doNothing().when(this.userService).changePassword(anyLong(), any(ChangePasswordDTO.class));

        Assertions.assertEquals(HttpStatus.OK, this.userController.changePassword(new ChangePasswordDTO("old", "new"), 1L).getStatusCode());
        verify(this.userService, times(1)).changePassword(anyLong(), any(ChangePasswordDTO.class));
    }

    @Test
    void getCurrentUser() {
        UserDTO userDTO = UserDTO.builder().email(this.user.getUsername()).preferredCurrency("HUF").build();
        when(this.userService.getUser(1L)).thenReturn(userDTO);

        Assertions.assertEquals(userDTO, this.userController.getCurrentUser(1L).getBody());
    }

    @Test
    void updatePreferences() {
        UserDTO userDTO = UserDTO.builder().email(this.user.getUsername()).preferredCurrency("EUR").build();
        when(this.userService.updatePreferredCurrency(1L, "EUR")).thenReturn(userDTO);

        Assertions.assertEquals(userDTO, this.userController.updatePreferences(new PreferencesUpdateDTO("EUR"), 1L).getBody());
        verify(this.userService, times(1)).updatePreferredCurrency(1L, "EUR");
    }
}