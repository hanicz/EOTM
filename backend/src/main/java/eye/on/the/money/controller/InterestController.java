package eye.on.the.money.controller;

import eye.on.the.money.dto.out.InterestDTO;
import eye.on.the.money.service.security.InterestService;
import eye.on.the.money.util.CsvResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import eye.on.the.money.security.CurrentUserId;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/v1/security/interest")
@RequiredArgsConstructor
public class InterestController {

    private final InterestService interestService;

    @GetMapping()
    public ResponseEntity<List<InterestDTO>> getAllInterest(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.interestService.getInterest(userId));
    }

    @PostMapping
    public ResponseEntity<InterestDTO> createInterest(@CurrentUserId Long userId, @RequestBody InterestDTO interestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.interestService.createInterest(interestDTO, userId));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserId Long userId, @RequestParam List<Long> ids) {
        this.interestService.deleteInterestById(ids, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/csv")
    public void getCSV(@CurrentUserId Long userId, HttpServletResponse servletResponse) throws IOException {
        this.interestService.getCSV(userId, CsvResponseUtil.prepare(servletResponse, "interest.csv"));
    }

    @PutMapping
    public ResponseEntity<InterestDTO> updateInterest(@CurrentUserId Long userId, @RequestBody InterestDTO interestDTO) {
        return ResponseEntity.ok(this.interestService.updateInterest(interestDTO, userId));
    }

    @PostMapping("/process/csv")
    public ResponseEntity<Void> processCSV(@CurrentUserId Long userId, @RequestParam("file") MultipartFile file) throws IOException {
        this.interestService.processCSV(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
