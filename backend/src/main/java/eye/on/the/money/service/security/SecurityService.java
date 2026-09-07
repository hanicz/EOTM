package eye.on.the.money.service.security;

import eye.on.the.money.exception.CSVException;
import eye.on.the.money.model.security.Security;
import eye.on.the.money.repository.security.SecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecurityService {

    private static final Map<String, String> ID_PREFIXES = Map.of(
            "Prémium Magyar Állampapír ", "PMÁP ",
            "Bónusz Magyar Állampapír ", "BMÁP ",
            "Euró Magyar Állampapír ", "EMÁP ",
            "Fix Magyar Állampapír ", "FMÁP ",
            "Diszkont Kincstárjegy ", "DKJ ");

    private final SecurityRepository securityRepository;

    public List<Security> getAllSecurities() {
        return this.securityRepository.findAllByOrderByNameAsc();
    }

    public Map<String, String> getIsinBySecurityId() {
        return this.securityRepository.findByIsinIsNotNull().stream()
                .collect(Collectors.toMap(Security::getId, Security::getIsin));
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

    public Security getOrCreateByName(String name) {
        return this.securityRepository.findByName(name)
                .orElseGet(() -> this.getOrCreateSecurity(this.toSecurityId(name), name));
    }

    private String toSecurityId(String name) {
        return ID_PREFIXES.entrySet().stream()
                .filter(prefix -> name.startsWith(prefix.getKey()))
                .map(prefix -> prefix.getValue() + name.substring(prefix.getKey().length()))
                .findFirst()
                .orElseThrow(() -> new CSVException("Unknown instrument: " + name));
    }
}
