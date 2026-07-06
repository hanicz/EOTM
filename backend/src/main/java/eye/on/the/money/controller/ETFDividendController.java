package eye.on.the.money.controller;

import eye.on.the.money.dto.out.ETFDividendDTO;
import eye.on.the.money.service.etf.ETFDividendService;
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
@RequestMapping("api/v1/etfdividend")
@Slf4j
@RequiredArgsConstructor
public class ETFDividendController {

    private final ETFDividendService etfDividendService;

    @GetMapping()
    public ResponseEntity<List<ETFDividendDTO>> getAllETFDividends(@CurrentUserEmail String userEmail) {
        log.trace("Enter");
        return ResponseEntity.ok(this.etfDividendService.getDividends(userEmail));
    }

    @PostMapping
    public ResponseEntity<ETFDividendDTO> createDividend(@CurrentUserEmail String userEmail, @RequestBody ETFDividendDTO dividendDTO) {
        log.trace("Enter");
        return ResponseEntity.status(HttpStatus.CREATED).body(this.etfDividendService.createETFDividend(dividendDTO, userEmail));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserEmail String userEmail, @RequestParam List<Long> ids) {
        log.trace("Enter");
        this.etfDividendService.deleteETFDividendById(ids, userEmail);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/csv")
    public void getCSV(@CurrentUserEmail String userEmail, HttpServletResponse servletResponse) throws IOException {
        log.trace("Enter");
        this.etfDividendService.getCSV(userEmail, CsvResponseUtil.prepare(servletResponse, "etf_dividends.csv"));
    }

    @PutMapping
    public ResponseEntity<ETFDividendDTO> updateETFDividend(@CurrentUserEmail String userEmail, @RequestBody ETFDividendDTO dividendDTO) {
        log.trace("Enter");
        return ResponseEntity.ok(this.etfDividendService.updateETFDividend(dividendDTO, userEmail));
    }

    @PostMapping("/process/csv")
    public ResponseEntity<Void> processCSV(@CurrentUserEmail String userEmail, @RequestParam("file") MultipartFile file) throws IOException {
        log.trace("Enter");
        this.etfDividendService.processCSV(userEmail, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
