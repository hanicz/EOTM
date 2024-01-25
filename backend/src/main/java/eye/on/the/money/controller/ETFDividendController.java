package eye.on.the.money.controller;

import eye.on.the.money.dto.out.ETFDividendDTO;
import eye.on.the.money.service.etf.ETFDividendService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("etfdividend")
@Slf4j
public class ETFDividendController {

    private final ETFDividendService etfDividendService;

    @Autowired
    public ETFDividendController(ETFDividendService etfDividendService) {
        this.etfDividendService = etfDividendService;
    }

    @GetMapping()
    public ResponseEntity<List<ETFDividendDTO>> getAllETFDividends(@AuthenticationPrincipal UserDetails user) {
        log.trace("Enter");
        return new ResponseEntity<>(this.etfDividendService.getDividends(user.getUsername()), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ETFDividendDTO> createDividend(@AuthenticationPrincipal UserDetails user, @RequestBody ETFDividendDTO dividendDTO) {
        log.trace("Enter");
        return new ResponseEntity<>(this.etfDividendService.createETFDividend(dividendDTO, user.getUsername()), HttpStatus.CREATED);
    }

    @DeleteMapping()
    public ResponseEntity<HttpStatus> deleteByIds(@AuthenticationPrincipal UserDetails user, @RequestParam String ids) {
        log.trace("Enter");
        List<Long> idList = Stream.of(ids.split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        this.etfDividendService.deleteETFDividendById(idList, user.getUsername());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/csv")
    public void getCSV(@AuthenticationPrincipal UserDetails user, HttpServletResponse servletResponse) throws IOException {
        log.trace("Enter");
        servletResponse.setContentType("text/csv");
        servletResponse.addHeader("Content-Disposition", "attachment; filename=\"dividends.csv\"");
        this.etfDividendService.getCSV(user.getUsername(), servletResponse.getWriter());
    }

    @PutMapping
    public ResponseEntity<ETFDividendDTO> updateETFDividend(@AuthenticationPrincipal UserDetails user, @RequestBody ETFDividendDTO dividendDTO) {
        log.trace("Enter");
        return new ResponseEntity<>(this.etfDividendService.updateETFDividend(dividendDTO, user.getUsername()), HttpStatus.CREATED);
    }

    @PostMapping("/process/csv")
    public ResponseEntity<HttpStatus> processCSV(@AuthenticationPrincipal UserDetails user, @RequestParam("file") MultipartFile file) throws IOException {
        log.trace("Enter");
        this.etfDividendService.processCSV(user.getUsername(), file);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

}
