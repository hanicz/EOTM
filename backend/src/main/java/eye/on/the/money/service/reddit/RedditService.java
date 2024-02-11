package eye.on.the.money.service.reddit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eye.on.the.money.dto.in.RedditPostDTO;
import eye.on.the.money.exception.JsonException;
import eye.on.the.money.model.news.News;
import eye.on.the.money.model.reddit.SubReddit;
import eye.on.the.money.repository.reddit.SubredditRepository;
import eye.on.the.money.service.api.RedditAPIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
    private final ObjectMapper objectMapper;
    @Value("${reddit.url}")
    private String redditUrl;
    @Value("${reddit.logo}")
    private String redditLogo;


    public List<News> getHotNewsFromSubreddits() {
        List<SubReddit> subRedditList = this.subredditRepository.findAll();
        JsonNode token = this.redditAPIService.getToken();
        Flux<JsonNode> subRedditsFlux = this.redditAPIService.getHotRedditNews(
                subRedditList.stream().map(SubReddit::getId).collect(Collectors.toList()),
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
                .image("self".equals(post.getThumbnail()) || post.getThumbnail().isBlank() ? this.redditLogo : post.getThumbnail())
                .url(this.redditUrl + post.getPermalink())
                .category("Reddit")
                .datetime(post.getCreated())
                .headline(post.getTitle())
                .source(post.getSubreddit())
                .summary(post.getSelftext())
                .build();
    }
}
