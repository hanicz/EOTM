package eye.on.the.money.controller;

import eye.on.the.money.model.news.News;
import eye.on.the.money.service.api.NewsAPIService;
import eye.on.the.money.service.reddit.RedditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/news")
@Slf4j
@RequiredArgsConstructor
public class NewsController {

    private final NewsAPIService newsAPIService;
    private final RedditService redditService;

    @GetMapping("category/reddit")
    public ResponseEntity<List<News>> getHotPosts() {
        return new ResponseEntity<>(this.redditService.getHotNewsFromSubreddits(), HttpStatus.OK);
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
}
