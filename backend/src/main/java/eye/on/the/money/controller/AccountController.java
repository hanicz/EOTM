package eye.on.the.money.controller;

import eye.on.the.money.model.stock.Account;
import eye.on.the.money.service.stock.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<Account>> getAccounts(@AuthenticationPrincipal UserDetails user) {
        return new ResponseEntity<>(this.accountService.getAccountsByUserEmail(user.getUsername()), HttpStatus.OK);
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        var isDeleted = this.accountService.deleteById(user.getUsername(), id);
        return new ResponseEntity<>(isDeleted ? HttpStatus.OK : HttpStatus.NOT_FOUND);
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody Account account, @AuthenticationPrincipal UserDetails user) {
        return new ResponseEntity<>(this.accountService.createAccount(account, user.getUsername()), HttpStatus.OK);
    }
}
