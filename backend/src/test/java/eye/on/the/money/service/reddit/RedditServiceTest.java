package eye.on.the.money.service.reddit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.EotmApplication;
import eye.on.the.money.dto.in.SubredditDTO;
import eye.on.the.money.model.news.News;
import eye.on.the.money.model.reddit.Subreddit;
import eye.on.the.money.repository.reddit.SubredditRepository;
import eye.on.the.money.service.api.RedditAPIService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = EotmApplication.class)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class RedditServiceTest {


    @Autowired
    private RedditService redditService;

    @MockBean
    private RedditAPIService redditAPIService;

    @Autowired
    private SubredditRepository subredditRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${reddit.logo}")
    private String redditLogo;

    @Test
    public void deleteSubreddit() {
        boolean result = this.redditService.deleteSubreddit(1L, "test@test.test");
        Assertions.assertTrue(result);
    }

    @Test
    public void deleteSubredditDoesntExist() {
        boolean result = this.redditService.deleteSubreddit(100000L, "test@test.test");
        Assertions.assertFalse(result);
    }

    @Test
    public void addSubReddit() {
        SubredditDTO subreddit = new SubredditDTO("test", "test");
        this.redditService.addSubreddit(subreddit, "test@test.test");
        var dbResult = this.subredditRepository.findBySubredditAndUserEmail("test", "test@test.test");
        Assertions.assertAll("Assert response",
                () -> Assertions.assertTrue(dbResult.isPresent()),
                () -> Assertions.assertEquals(subreddit.subReddit(), dbResult.get().getSubreddit()),
                () -> Assertions.assertEquals(subreddit.description(), dbResult.get().getDescription())
        );
    }

    @Test
    public void getSubredditsByUser() {
        List<Subreddit> actual = this.subredditRepository.findByUserEmailOrderBySubredditAsc("test@test.test");
        List<Subreddit> result = this.redditService.getSubredditsByUser("test@test.test");
        Assertions.assertEquals(actual.size(), result.size());
    }

    @Test
    public void getHotNewsFromSubreddits() throws JsonProcessingException {
        JsonNode token = this.objectMapper.readTree("{ \"access_token\": \"token\" }");
        when(this.redditAPIService.getToken()).thenReturn(token);
        when(this.redditAPIService.getHotRedditNews(any(), anyString())).thenReturn(Flux.just(this.createSubRedditJson()));

        List<News> result = this.redditService.getHotNewsFromSubreddits("test@test.test");

        Assertions.assertAll("Assert response",
                () -> Assertions.assertEquals(3, result.size()),
                () -> Assertions.assertEquals("Title2", result.get(0).getHeadline()),
                () -> Assertions.assertEquals("Title3", result.get(1).getHeadline()),
                () -> Assertions.assertEquals("Title4", result.get(2).getHeadline()),
                () -> Assertions.assertEquals("notBlank", result.get(1).getImage()),
                () -> Assertions.assertEquals(this.redditLogo, result.get(0).getImage()),
                () -> Assertions.assertEquals(this.redditLogo, result.get(2).getImage())
        );
    }

    private JsonNode createSubRedditJson() throws JsonProcessingException {
        return this.objectMapper.readTree("""
                    {
                        "children": [
                            {
                                "data": {
                                    "selftext": "SelfText",
                                    "title": "Title",
                                    "thumbnail": "",
                                    "subreddit": "investing",
                                    "created": 1716886868.0,
                                    "permalink": "/r/investing/test/test",
                                    "stickied": true
                                }
                            },
                            {
                                "data": {
                                    "selftext": "SelfText2",
                                    "title": "Title2",
                                    "thumbnail": "",
                                    "subreddit": "investing",
                                    "created": 1716886868.0,
                                    "permalink": "/r/investing/test/test2",
                                    "stickied": false
                                }
                            },
                            {
                                "data": {
                                    "selftext": "SelfText3",
                                    "title": "Title3",
                                    "thumbnail": "notBlank",
                                    "subreddit": "investing",
                                    "created": 1716886868.0,
                                    "permalink": "/r/investing/test/test3",
                                    "stickied": false
                                }
                            },
                            {
                                "data": {
                                    "selftext": "SelfText4",
                                    "title": "Title4",
                                    "thumbnail": "self",
                                    "subreddit": "investing",
                                    "created": 1716886868.0,
                                    "permalink": "/r/investing/test/test4",
                                    "stickied": false
                                }
                            }
                        ]
                    }
                """);
    }

    private JsonNode createBlankSubRedditJson() throws JsonProcessingException {
        return this.objectMapper.readTree("""
                    "children": [
                            {
                                "data": {
                                    "notNews": "notNews"
                                }
                            }
                        ]
                """);
    }
}