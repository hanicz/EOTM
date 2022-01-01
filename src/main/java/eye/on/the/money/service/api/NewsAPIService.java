package eye.on.the.money.service.api;

import eye.on.the.money.model.news.News;

import java.util.List;

public interface NewsAPIService {
    public List<News> getNews(String category);
}
