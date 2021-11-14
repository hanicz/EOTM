package eye.on.the.money.repository;

import eye.on.the.money.model.Credential;
import org.springframework.data.repository.CrudRepository;

public interface CredentialRepository extends CrudRepository<Credential, String> {
}
