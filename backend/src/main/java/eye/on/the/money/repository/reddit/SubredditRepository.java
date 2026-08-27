package eye.on.the.money.repository.reddit;

import eye.on.the.money.model.reddit.Subreddit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubredditRepository extends JpaRepository<Subreddit, Long> {
    List<Subreddit> findByUserIdOrderBySubredditAsc(Long userId);

    Optional<Subreddit> findBySubredditAndUserId(String subreddit, Long userId);

    int deleteByIdAndUserId(Long id, Long userId);
}
