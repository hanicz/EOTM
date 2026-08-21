package eye.on.the.money.controller;

import eye.on.the.money.dto.in.FireProjectionDTO;
import eye.on.the.money.dto.out.FireProjectionResultDTO;
import eye.on.the.money.security.CurrentUserEmail;
import eye.on.the.money.service.shared.FireService;
import eye.on.the.money.util.CsvResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("api/v1/fire")
@Slf4j
@Validated
@RequiredArgsConstructor
public class FireController {

    private final FireService fireService;

    /**
     * A POST rather than a GET because the projection is worked out from the supplied assumptions; only the
     * portfolio it starts from comes from the database.
     */
    @PostMapping("/projection")
    public ResponseEntity<FireProjectionResultDTO> project(@CurrentUserEmail String userEmail,
                                                           @RequestBody @Valid FireProjectionDTO projectionDTO) {
        log.trace("Enter");
        return ResponseEntity.ok(this.fireService.project(userEmail, projectionDTO));
    }

    @PostMapping("/projection/csv")
    public void getCSV(@CurrentUserEmail String userEmail, @RequestBody @Valid FireProjectionDTO projectionDTO,
                       HttpServletResponse servletResponse) throws IOException {
        log.trace("Enter");
        this.fireService.getCSV(userEmail, projectionDTO, CsvResponseUtil.prepare(servletResponse, "fire-projection.csv"));
    }
}
