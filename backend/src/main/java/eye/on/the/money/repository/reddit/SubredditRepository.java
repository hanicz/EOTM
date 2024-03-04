package eye.on.the.money.repository.reddit;

import eye.on.the.money.model.reddit.Subreddit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubredditRepository extends JpaRepository<Subreddit, Long> {
    List<Subreddit> findByUserEmailOrderByIdAsc(String userEmail);

    void deleteByIdAndUserEmail(Long id, String userEmail);
}
