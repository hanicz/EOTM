package eye.on.the.money.repository.reddit;

import eye.on.the.money.model.reddit.Subreddit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubredditRepository extends JpaRepository<Subreddit, Long> {
    List<Subreddit> findByUserEmailOrderBySubredditAsc(String userEmail);

    Optional<Subreddit> findBySubredditAndUserEmail(String subreddit, String userEmail);

    int deleteByIdAndUserEmail(Long id, String userEmail);
}
