package eye.on.the.money.service.reddit;

import eye.on.the.money.service.api.NewsAPIService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedditService {

    private final NewsAPIService newsAPIService;

    public void getHotNewsFromSubreddits() {

    }
}
