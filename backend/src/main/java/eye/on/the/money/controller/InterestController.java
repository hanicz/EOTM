package eye.on.the.money.controller;

import eye.on.the.money.dto.out.InterestDTO;
import eye.on.the.money.service.security.InterestService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/v1/security/interest")
@Slf4j
@RequiredArgsConstructor
public class InterestController {

    private final InterestService interestService;

    @GetMapping()
    public ResponseEntity<List<InterestDTO>> getAllInterest(@AuthenticationPrincipal UserDetails user) {
        log.trace("Enter");
        return new ResponseEntity<>(this.interestService.getInterest(user.getUsername()), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<InterestDTO> createInterest(@AuthenticationPrincipal UserDetails user, @RequestBody InterestDTO interestDTO) {
        log.trace("Enter");
        return new ResponseEntity<>(this.interestService.createInterest(interestDTO, user.getUsername()), HttpStatus.CREATED);
    }

    @DeleteMapping()
    public ResponseEntity<HttpStatus> deleteByIds(@AuthenticationPrincipal UserDetails user, @RequestParam List<Long> ids) {
        log.trace("Enter");
        this.interestService.deleteInterestById(ids, user.getUsername());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/csv")
    public void getCSV(@AuthenticationPrincipal UserDetails user, HttpServletResponse servletResponse) throws IOException {
        log.trace("Enter");
        servletResponse.setContentType("text/csv");
        servletResponse.addHeader("Content-Disposition", "attachment; filename=\"interest.csv\"");
        this.interestService.getCSV(user.getUsername(), servletResponse.getWriter());
    }

    @PutMapping
    public ResponseEntity<InterestDTO> updateInterest(@AuthenticationPrincipal UserDetails user, @RequestBody InterestDTO interestDTO) {
        log.trace("Enter");
        return new ResponseEntity<>(this.interestService.updateInterest(interestDTO, user.getUsername()), HttpStatus.CREATED);
    }

    @PostMapping("/process/csv")
    public ResponseEntity<HttpStatus> processCSV(@AuthenticationPrincipal UserDetails user, @RequestParam("file") MultipartFile file) throws IOException {
        log.trace("Enter");
        this.interestService.processCSV(user.getUsername(), file);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
