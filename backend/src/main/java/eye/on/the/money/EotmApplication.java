package eye.on.the.money;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class EotmApplication {
    public static void main(String[] args) {
        SpringApplication.run(EotmApplication.class, args);
    }
}
