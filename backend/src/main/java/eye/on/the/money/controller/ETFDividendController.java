package eye.on.the.money.controller;

import eye.on.the.money.dto.out.ETFDividendDTO;
import eye.on.the.money.service.etf.ETFDividendService;
import eye.on.the.money.util.CsvResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import eye.on.the.money.security.CurrentUserId;
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
    public ResponseEntity<List<ETFDividendDTO>> getAllETFDividends(@CurrentUserId Long userId) {
        log.trace("Enter");
        return ResponseEntity.ok(this.etfDividendService.getDividends(userId));
    }

    @PostMapping
    public ResponseEntity<ETFDividendDTO> createDividend(@CurrentUserId Long userId, @RequestBody ETFDividendDTO dividendDTO) {
        log.trace("Enter");
        return ResponseEntity.status(HttpStatus.CREATED).body(this.etfDividendService.createETFDividend(dividendDTO, userId));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserId Long userId, @RequestParam List<Long> ids) {
        log.trace("Enter");
        this.etfDividendService.deleteETFDividendById(ids, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/csv")
    public void getCSV(@CurrentUserId Long userId, HttpServletResponse servletResponse) throws IOException {
        log.trace("Enter");
        this.etfDividendService.getCSV(userId, CsvResponseUtil.prepare(servletResponse, "etf_dividends.csv"));
    }

    @PutMapping
    public ResponseEntity<ETFDividendDTO> updateETFDividend(@CurrentUserId Long userId, @RequestBody ETFDividendDTO dividendDTO) {
        log.trace("Enter");
        return ResponseEntity.ok(this.etfDividendService.updateETFDividend(dividendDTO, userId));
    }

    @PostMapping("/process/csv")
    public ResponseEntity<Void> processCSV(@CurrentUserId Long userId, @RequestParam("file") MultipartFile file) throws IOException {
        log.trace("Enter");
        this.etfDividendService.processCSV(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
