package eye.on.the.money.controller;

import eye.on.the.money.dto.out.DividendDTO;
import eye.on.the.money.service.stock.DividendService;
import eye.on.the.money.util.CsvResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import eye.on.the.money.security.CurrentUserEmail;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/v1/dividend")
@Slf4j
@RequiredArgsConstructor
public class DividendController {

    private final DividendService dividendService;

    @GetMapping()
    public ResponseEntity<List<DividendDTO>> getAllDividends(@CurrentUserEmail String userEmail) {
        log.trace("Enter");
        return ResponseEntity.ok(this.dividendService.getDividends(userEmail));
    }

    @PostMapping
    public ResponseEntity<DividendDTO> createDividend(@CurrentUserEmail String userEmail, @RequestBody DividendDTO dividendDTO) {
        log.trace("Enter");
        return ResponseEntity.status(HttpStatus.CREATED).body(this.dividendService.createDividend(dividendDTO, userEmail));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserEmail String userEmail, @RequestParam List<Long> ids) {
        log.trace("Enter");
        this.dividendService.deleteDividendById(ids, userEmail);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/csv")
    public void getCSV(@CurrentUserEmail String userEmail, HttpServletResponse servletResponse) throws IOException {
        log.trace("Enter");
        this.dividendService.getCSV(userEmail, CsvResponseUtil.prepare(servletResponse, "dividends.csv"));
    }

    @PutMapping
    public ResponseEntity<DividendDTO> updateDividend(@CurrentUserEmail String userEmail, @RequestBody DividendDTO dividendDTO) {
        log.trace("Enter");
        return ResponseEntity.ok(this.dividendService.updateDividend(dividendDTO, userEmail));
    }

    @PostMapping("/process/csv")
    public ResponseEntity<Void> processCSV(@CurrentUserEmail String userEmail, @RequestParam("file") MultipartFile file) throws IOException {
        log.trace("Enter");
        this.dividendService.processCSV(userEmail, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
