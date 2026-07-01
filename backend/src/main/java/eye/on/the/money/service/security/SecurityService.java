package eye.on.the.money.service.security;

import eye.on.the.money.model.security.Security;
import eye.on.the.money.repository.security.SecurityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityService {

    private final SecurityRepository securityRepository;

    public List<Security> getAllSecurities() {
        log.trace("Enter getAllSecurities");
        return this.securityRepository.findAllByOrderByNameAsc();
    }

    public Security getOrCreateSecurity(String id, String name) {
        return this.securityRepository.findById(id).orElseGet(() -> {
            Security newSecurity = Security.builder()
                    .id(id)
                    .name(name)
                    .build();
            return this.securityRepository.save(newSecurity);
        });
    }
}
