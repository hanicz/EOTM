package eye.on.the.money.controller;

import eye.on.the.money.model.stock.Account;
import eye.on.the.money.service.stock.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import eye.on.the.money.security.CurrentUserId;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<Account>> getAccounts(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.accountService.getAccountsByUserId(userId));
    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteAccount(@CurrentUserId Long userId, @PathVariable Long id) {
        var isDeleted = this.accountService.deleteById(userId, id);
        return ResponseEntity.status(isDeleted ? HttpStatus.OK : HttpStatus.NOT_FOUND).build();
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody Account account, @CurrentUserId Long userId) {
        return ResponseEntity.ok(this.accountService.createAccount(account, userId));
    }
}
