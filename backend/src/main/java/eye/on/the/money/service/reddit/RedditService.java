package eye.on.the.money.service.reddit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.dto.in.RedditPostDTO;
import eye.on.the.money.dto.in.SubredditDTO;
import eye.on.the.money.exception.JsonException;
import eye.on.the.money.model.User;
import eye.on.the.money.model.news.News;
import eye.on.the.money.model.reddit.Subreddit;
import eye.on.the.money.repository.reddit.SubredditRepository;
import eye.on.the.money.service.api.RedditAPIService;
import eye.on.the.money.service.user.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedditService {

    private final RedditAPIService redditAPIService;
    private final SubredditRepository subredditRepository;
    private final UserServiceImpl userService;
    private final ObjectMapper objectMapper;
    @Value("${reddit.url}")
    private String redditUrl;
    @Value("${reddit.logo}")
    private String redditLogo;


    public List<News> getHotNewsFromSubreddits(String userEmail) {
        List<Subreddit> subredditList = this.subredditRepository.findByUserEmailOrderBySubredditAsc(userEmail);
        JsonNode token = this.redditAPIService.getToken();
        Flux<JsonNode> subRedditsFlux = this.redditAPIService.getHotRedditNews(
                subredditList.stream().map(Subreddit::getSubreddit).collect(Collectors.toList()),
                token.findValue("access_token").textValue());

        return subRedditsFlux
                .map(jsonNode -> jsonNode.findValue("children").findValues("data"))
                .collectList().block()
                .stream()
                .flatMap(Collection::stream)
                .map(data -> this.convert(data, RedditPostDTO.class))
                .filter(post -> !post.isStickied())
                .map(this::convertPostToNews)
                .toList();
    }

    @Transactional
    public boolean deleteSubreddit(Long id, String userEmail) {
        return this.subredditRepository.deleteByIdAndUserEmail(id, userEmail) > 0;
    }

    @Transactional
    public Subreddit addSubreddit(SubredditDTO subreddit, String userEmail) {
        User user = this.userService.loadUserByEmail(userEmail);
        return this.subredditRepository.save(Subreddit.builder()
                .subreddit(subreddit.subReddit())
                .description(subreddit.description())
                .user(user)
                .build());
    }

    public List<Subreddit> getSubredditsByUser(String userEmail) {
        return this.subredditRepository.findByUserEmailOrderBySubredditAsc(userEmail);
    }

    private <T> T convert(JsonNode json, Class<T> cls) {
        try {
            return this.objectMapper.treeToValue(json, cls);
        } catch (JsonProcessingException e) {
            log.error("Unable to process reddit response. {}", e.getMessage(), e);
            throw new JsonException("Unable to process reddit response");
        }
    }

    private News convertPostToNews(RedditPostDTO post) {
        return News.builder()
                .id(post.getCreated())
                .image(post.getThumbnail().isBlank() || "self".equals(post.getThumbnail()) ? this.redditLogo : post.getThumbnail())
                .url(this.redditUrl + post.getPermalink())
                .category("Reddit")
                .datetime(post.getCreated())
                .headline(post.getTitle())
                .source(post.getSubreddit())
                .summary(post.getSelftext())
                .build();
    }
}
