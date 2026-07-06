package eye.on.the.money.controller;

import eye.on.the.money.dto.in.SubredditDTO;
import eye.on.the.money.model.news.News;
import eye.on.the.money.model.reddit.Subreddit;
import eye.on.the.money.service.api.NewsAPIService;
import eye.on.the.money.service.reddit.RedditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import eye.on.the.money.security.CurrentUserEmail;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/news")
@Slf4j
@RequiredArgsConstructor
public class NewsController {

    private final NewsAPIService newsAPIService;
    private final RedditService redditService;

    @GetMapping("category/reddit")
    public ResponseEntity<List<News>> getHotPosts(@CurrentUserEmail String userEmail) {
        return ResponseEntity.ok(this.redditService.getHotNewsFromSubreddits(userEmail));
    }

    @GetMapping("category/{category}")
    public ResponseEntity<List<News>> getGeneralNews(@PathVariable String category) {
        log.trace("Enter");
        return ResponseEntity.ok(this.newsAPIService.getNews(category));
    }

    @GetMapping("company/{symbol}")
    public ResponseEntity<List<News>> getCompanyNews(@PathVariable String symbol) {
        log.trace("Enter");
        return ResponseEntity.ok(this.newsAPIService.getCompanyNews(symbol));
    }

    @GetMapping("reddit")
    public ResponseEntity<List<Subreddit>> getSubreddits(@CurrentUserEmail String userEmail) {
        return ResponseEntity.ok(this.redditService.getSubredditsByUser(userEmail));
    }

    @DeleteMapping("reddit/{id}")
    public ResponseEntity<Void> deleteSubreddit(@PathVariable Long id, @CurrentUserEmail String userEmail) {
        var isDeleted = this.redditService.deleteSubreddit(id, userEmail);
        return ResponseEntity.status(isDeleted ? HttpStatus.OK : HttpStatus.NOT_FOUND).build();
    }

    @PostMapping("reddit")
    public ResponseEntity<Subreddit> addSubreddit(@RequestBody SubredditDTO subreddit, @CurrentUserEmail String userEmail) {
        return ResponseEntity.ok(this.redditService.addSubreddit(subreddit, userEmail));
    }
}
