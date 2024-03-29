package eye.on.the.money.controller;

import eye.on.the.money.service.shared.TaxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("api/v1/tax")
@Slf4j
@RequiredArgsConstructor
public class TaxController {
    private final TaxService taxService;

    @PostMapping("/process")
    public ResponseEntity<HttpStatus> processMNBExcel(@RequestParam("file") MultipartFile file) throws IOException {
        log.trace("Enter");
        this.taxService.loadRatesFromXLS(file);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

}
