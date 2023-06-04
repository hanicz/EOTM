package eye.on.the.money.controller;

import eye.on.the.money.dto.out.ETFDividendDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.etf.ETFDividendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("etfdividend")
@Slf4j
public class ETFDividendController {

    @Autowired
    private ETFDividendService etfDividendService;

    @GetMapping()
    public ResponseEntity<List<ETFDividendDTO>> getAllETFDividends(@AuthenticationPrincipal User user) {
        log.trace("Enter getAllETFDividends");
        return new ResponseEntity<List<ETFDividendDTO>>(this.etfDividendService.getDividends(user.getId()), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<ETFDividendDTO> createDividend(@AuthenticationPrincipal User user, @RequestBody ETFDividendDTO dividendDTO) {
        log.trace("Enter createDividend");
        return new ResponseEntity<ETFDividendDTO>(this.etfDividendService.createETFDividend(dividendDTO, user), HttpStatus.CREATED);
    }

    @DeleteMapping()
    public ResponseEntity<HttpStatus> deleteByIds(@AuthenticationPrincipal User user, @RequestParam String ids) {
        log.trace("Enter deleteByIds");
        List<Long> idList = Stream.of(ids.split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        this.etfDividendService.deleteETFDividendById(idList, user);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/csv")
    public void getCSV(@AuthenticationPrincipal User user, HttpServletResponse servletResponse) throws IOException {
        log.trace("Enter getCSV");
        servletResponse.setContentType("text/csv");
        servletResponse.addHeader("Content-Disposition", "attachment; filename=\"dividends.csv\"");
        this.etfDividendService.getCSV(user.getId(), servletResponse.getWriter());
    }

    @PutMapping
    public ResponseEntity<ETFDividendDTO> updateETFDividend(@AuthenticationPrincipal User user, @RequestBody ETFDividendDTO dividendDTO) {
        log.trace("Enter updateETFDividend");
        return new ResponseEntity<ETFDividendDTO>(this.etfDividendService.updateETFDividend(dividendDTO, user), HttpStatus.CREATED);
    }

    @PostMapping("/process/csv")
    public ResponseEntity<HttpStatus> processCSV(@AuthenticationPrincipal User user, @RequestParam("file") MultipartFile file) throws IOException {
        log.trace("Enter processCSV");
        this.etfDividendService.processCSV(user, file);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

}
