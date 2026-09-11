package eye.on.the.money.controller;

import eye.on.the.money.dto.in.SubredditDTO;
import eye.on.the.money.model.news.News;
import eye.on.the.money.model.reddit.Subreddit;
import eye.on.the.money.service.news.NewsService;
import eye.on.the.money.service.reddit.RedditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import eye.on.the.money.security.CurrentUserId;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;
    private final RedditService redditService;

    @GetMapping("category/reddit")
    public ResponseEntity<List<News>> getHotPosts(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.redditService.getHotNewsFromSubreddits(userId));
    }

    @GetMapping("portfolio")
    public ResponseEntity<List<News>> getPortfolioNews(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.newsService.getPortfolioNews(userId));
    }

    @GetMapping("category/{category}")
    public ResponseEntity<List<News>> getGeneralNews(@PathVariable String category) {
        return ResponseEntity.ok(this.newsService.getCategoryNews(category));
    }

    @GetMapping("company/{symbol}")
    public ResponseEntity<List<News>> getCompanyNews(@PathVariable String symbol) {
        return ResponseEntity.ok(this.newsService.getCompanyNews(symbol));
    }

    @GetMapping("reddit")
    public ResponseEntity<List<Subreddit>> getSubreddits(@CurrentUserId Long userId) {
        return ResponseEntity.ok(this.redditService.getSubredditsByUser(userId));
    }

    @DeleteMapping("reddit/{id}")
    public ResponseEntity<Void> deleteSubreddit(@PathVariable Long id, @CurrentUserId Long userId) {
        var isDeleted = this.redditService.deleteSubreddit(id, userId);
        return ResponseEntity.status(isDeleted ? HttpStatus.OK : HttpStatus.NOT_FOUND).build();
    }

    @PostMapping("reddit")
    public ResponseEntity<Subreddit> addSubreddit(@RequestBody SubredditDTO subreddit, @CurrentUserId Long userId) {
        return ResponseEntity.ok(this.redditService.addSubreddit(subreddit, userId));
    }
}
