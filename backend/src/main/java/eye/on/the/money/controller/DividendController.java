package eye.on.the.money.controller;

import eye.on.the.money.dto.out.DividendDTO;
import eye.on.the.money.service.stock.DividendService;
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
@RequestMapping("api/v1/dividend")
@Slf4j
@RequiredArgsConstructor
public class DividendController {

    private final DividendService dividendService;

    @GetMapping()
    public ResponseEntity<List<DividendDTO>> getAllDividends(@CurrentUserId Long userId) {
        log.trace("Enter");
        return ResponseEntity.ok(this.dividendService.getDividends(userId));
    }

    @PostMapping
    public ResponseEntity<DividendDTO> createDividend(@CurrentUserId Long userId, @RequestBody DividendDTO dividendDTO) {
        log.trace("Enter");
        return ResponseEntity.status(HttpStatus.CREATED).body(this.dividendService.createDividend(dividendDTO, userId));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteByIds(@CurrentUserId Long userId, @RequestParam List<Long> ids) {
        log.trace("Enter");
        this.dividendService.deleteDividendById(ids, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/csv")
    public void getCSV(@CurrentUserId Long userId, HttpServletResponse servletResponse) throws IOException {
        log.trace("Enter");
        this.dividendService.getCSV(userId, CsvResponseUtil.prepare(servletResponse, "dividends.csv"));
    }

    @PutMapping
    public ResponseEntity<DividendDTO> updateDividend(@CurrentUserId Long userId, @RequestBody DividendDTO dividendDTO) {
        log.trace("Enter");
        return ResponseEntity.ok(this.dividendService.updateDividend(dividendDTO, userId));
    }

    @PostMapping("/process/csv")
    public ResponseEntity<Void> processCSV(@CurrentUserId Long userId, @RequestParam("file") MultipartFile file) throws IOException {
        log.trace("Enter");
        this.dividendService.processCSV(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
