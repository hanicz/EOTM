package eye.on.the.money.controller;

import eye.on.the.money.dto.out.DividendDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.service.DividendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("dividend")
public class DividendController {

    private static final Logger log = LoggerFactory.getLogger(DividendController.class);

    @Autowired
    private DividendService dividendService;

    @GetMapping()
    public ResponseEntity<List<DividendDTO>> getAllDividends(@AuthenticationPrincipal User user) {
        log.trace("Enter getAllDividends");
        return new ResponseEntity<List<DividendDTO>>(this.dividendService.getDividends(user.getId()), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<DividendDTO> createDividend(@AuthenticationPrincipal User user, @RequestBody DividendDTO dividendDTO) {
        log.trace("Enter createDividend");
        return new ResponseEntity<DividendDTO>(this.dividendService.createDividend(dividendDTO, user), HttpStatus.CREATED);
    }

    @DeleteMapping()
    public ResponseEntity<HttpStatus> deleteByIds(@AuthenticationPrincipal User user, @RequestParam String ids) {
        log.trace("Enter deleteByIds");
        List<Long> idList = Stream.of(ids.split(",")).map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
        this.dividendService.deleteDividendById(idList);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/csv")
    public void getCSV(@AuthenticationPrincipal User user, HttpServletResponse servletResponse) throws IOException {
        log.trace("Enter getCSV");
        servletResponse.setContentType("text/csv");
        servletResponse.addHeader("Content-Disposition", "attachment; filename=\"dividends.csv\"");
        this.dividendService.getCSV(user.getId(), servletResponse.getWriter());
    }

    @PutMapping
    public ResponseEntity<DividendDTO> updateDividend(@AuthenticationPrincipal User user, @RequestBody DividendDTO dividendDTO) {
        log.trace("Enter updateDividend");
        return new ResponseEntity<DividendDTO>(this.dividendService.updateDividend(dividendDTO, user), HttpStatus.CREATED);
    }

    @PostMapping("/process/csv")
    public ResponseEntity<HttpStatus> processCSV(@AuthenticationPrincipal User user, @RequestParam("file") MultipartFile file) throws IOException {
        log.trace("Enter processCSV");
        this.dividendService.processCSV(user, file);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
