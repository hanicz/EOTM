package eye.on.the.money.controller;

import eye.on.the.money.model.User;
import eye.on.the.money.model.news.News;
import eye.on.the.money.service.api.NewsAPIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("news")
public class NewsController {

    @Autowired
    private NewsAPIService newsAPIService;

    private static final Logger log = LoggerFactory.getLogger(NewsController.class);

    @GetMapping("category/{category}")
    public ResponseEntity<List<News>> getGeneralNews(@AuthenticationPrincipal User user, @PathVariable String category) {
        log.trace("Enter getNews");
        return new ResponseEntity<List<News>>(this.newsAPIService.getNews(category), HttpStatus.OK);
    }

    @GetMapping("company/{symbol}")
    public ResponseEntity<List<News>> getCompanyNews(@AuthenticationPrincipal User user, @PathVariable String symbol) {
        log.trace("Enter getCompanyNews");
        return new ResponseEntity<List<News>>(this.newsAPIService.getCompanyNews(symbol), HttpStatus.OK);
    }
}
