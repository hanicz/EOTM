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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    public ResponseEntity<List<News>> getHotPosts(@AuthenticationPrincipal UserDetails user) {
        return new ResponseEntity<>(this.redditService.getHotNewsFromSubreddits(user.getUsername()), HttpStatus.OK);
    }

    @GetMapping("category/{category}")
    public ResponseEntity<List<News>> getGeneralNews(@PathVariable String category) {
        log.trace("Enter");
        return new ResponseEntity<>(this.newsAPIService.getNews(category), HttpStatus.OK);
    }

    @GetMapping("company/{symbol}")
    public ResponseEntity<List<News>> getCompanyNews(@PathVariable String symbol) {
        log.trace("Enter");
        return new ResponseEntity<>(this.newsAPIService.getCompanyNews(symbol), HttpStatus.OK);
    }

    @GetMapping("reddit")
    public ResponseEntity<List<Subreddit>> getSubreddits(@AuthenticationPrincipal UserDetails user) {
        return new ResponseEntity<>(this.redditService.getSubredditsByUser(user.getUsername()), HttpStatus.OK);
    }

    @DeleteMapping("reddit/{id}")
    public ResponseEntity<Void> deleteSubreddit(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
        this.redditService.deleteSubreddit(id, user.getUsername());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("reddit")
    public ResponseEntity<Subreddit> addSubreddit(@RequestBody SubredditDTO subreddit, @AuthenticationPrincipal UserDetails user) {
        return new ResponseEntity<>(this.redditService.addSubreddit(subreddit, user.getUsername()), HttpStatus.OK);
    }
}
