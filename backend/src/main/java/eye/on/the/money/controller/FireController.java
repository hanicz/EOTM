package eye.on.the.money.controller;

import eye.on.the.money.dto.in.FireProjectionDTO;
import eye.on.the.money.dto.out.FireProjectionResultDTO;
import eye.on.the.money.dto.out.FireSnapshotDTO;
import eye.on.the.money.security.CurrentUserId;
import eye.on.the.money.service.shared.FireService;
import eye.on.the.money.service.shared.FireSnapshotService;
import eye.on.the.money.util.CsvResponseUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("api/v1/fire")
@Validated
@RequiredArgsConstructor
public class FireController {

    private final FireService fireService;
    private final FireSnapshotService fireSnapshotService;

    @GetMapping("/snapshot")
    public ResponseEntity<FireSnapshotDTO> snapshot(@CurrentUserId Long userId,
                                                    @RequestParam(required = false) String currency) {
        return ResponseEntity.ok(this.fireSnapshotService.snapshot(userId, currency));
    }

    /**
     * A POST rather than a GET because the projection is worked out from the supplied assumptions; only the
     * portfolio it starts from comes from the database.
     */
    @PostMapping("/projection")
    public ResponseEntity<FireProjectionResultDTO> project(@CurrentUserId Long userId,
                                                           @RequestBody @Valid FireProjectionDTO projectionDTO) {
        return ResponseEntity.ok(this.fireService.project(userId, projectionDTO));
    }

    @PostMapping("/projection/csv")
    public void getCSV(@CurrentUserId Long userId, @RequestBody @Valid FireProjectionDTO projectionDTO,
                       HttpServletResponse servletResponse) throws IOException {
        this.fireService.getCSV(userId, projectionDTO, CsvResponseUtil.prepare(servletResponse, "fire-projection.csv"));
    }
}
