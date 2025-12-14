package eye.on.the.money.controller;

import eye.on.the.money.dto.in.SubredditDTO;
import eye.on.the.money.model.User;
import eye.on.the.money.model.news.News;
import eye.on.the.money.model.reddit.Subreddit;
import eye.on.the.money.service.api.NewsAPIService;
import eye.on.the.money.service.reddit.RedditService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class NewsControllerTest {

    @Mock
    private NewsAPIService newsAPIService;
    @Mock
    private RedditService redditService;

    @InjectMocks
    private NewsController newsController;

    private final User user = User.builder().id(1L).email("email").build();

    @Test
    public void getGeneralNews() {
        List<News> news = this.createNewsList();
        when(this.newsAPIService.getNews(anyString())).thenReturn(news);

        ResponseEntity<List<News>> result = this.newsController.getGeneralNews("category");

        Assertions.assertEquals(news, result.getBody());
    }

    @Test
    public void getCompanyNews() {
        List<News> news = this.createNewsList();
        when(this.newsAPIService.getCompanyNews(anyString())).thenReturn(news);

        ResponseEntity<List<News>> result = this.newsController.getCompanyNews("symbol");

        Assertions.assertEquals(news, result.getBody());
    }

    @Test
    public void getRedditNews() {
        List<News> newsList = this.createNewsList();
        when(this.redditService.getHotNewsFromSubreddits(this.user.getEmail())).thenReturn(newsList);

        ResponseEntity<List<News>> result = this.newsController.getHotPosts(this.user);
        Assertions.assertEquals(newsList, result.getBody());
    }

    @Test
    public void getSubreddits() {
        List<Subreddit> subReddits = new ArrayList<>();

        subReddits.add(Subreddit.builder().id(1L).description("desc1").subreddit("subreddit1").build());
        subReddits.add(Subreddit.builder().id(2L).description("desc2").subreddit("subreddit2").build());
        subReddits.add(Subreddit.builder().id(3L).description("desc3").subreddit("subreddit3").build());

        when(this.redditService.getSubredditsByUser(this.user.getEmail())).thenReturn(subReddits);

        ResponseEntity<List<Subreddit>> result = this.newsController.getSubreddits(this.user);
        Assertions.assertIterableEquals(subReddits, result.getBody());
    }

    @Test
    public void deleteSubreddit() {
        when(this.redditService.deleteSubreddit(1L, this.user.getEmail())).thenReturn(true);
        ResponseEntity<Void> result = this.newsController.deleteSubreddit(1L, this.user);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    public void deleteSubreddit404() {
        when(this.redditService.deleteSubreddit(1L, this.user.getEmail())).thenReturn(false);
        ResponseEntity<Void> result = this.newsController.deleteSubreddit(1L, this.user);
        Assertions.assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
    }

    @Test
    public void addSubreddit() {
        SubredditDTO sDTO = new SubredditDTO("subreddit", "description");
        Subreddit subreddit = new Subreddit(1L, "description", "subreddit", null);
        when(this.redditService.addSubreddit(sDTO, this.user.getEmail())).thenReturn(subreddit);
        ResponseEntity<Subreddit> result = this.newsController.addSubreddit(sDTO, this.user);
        Assertions.assertEquals(subreddit, result.getBody());
    }

    private List<News> createNewsList() {
        List<News> news = new ArrayList<>();
        news.add(News.builder().datetime(123123L).headline("HL1").id(1L).summary("s1").image("i1").source("s1").category("c1").build());
        news.add(News.builder().datetime(4123123L).headline("HL2").id(2L).summary("s2").image("i2").source("s2").category("c2").build());
        news.add(News.builder().datetime(1230330L).headline("HL3").id(3L).summary("s3").image("i3").source("s3").category("c3").build());
        return news;
    }
}