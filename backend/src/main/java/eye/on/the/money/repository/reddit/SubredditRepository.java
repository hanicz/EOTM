package eye.on.the.money.repository.reddit;

import eye.on.the.money.model.reddit.SubReddit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubredditRepository extends JpaRepository<SubReddit, String> {
}
