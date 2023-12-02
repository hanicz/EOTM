package eye.on.the.money.controller;

import eye.on.the.money.model.news.News;
import eye.on.the.money.service.api.NewsAPIService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class NewsControllerTest {

    @Mock
    private NewsAPIService newsAPIService;

    @InjectMocks
    private NewsController newsController;

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

    private List<News> createNewsList() {
        List<News> news = new ArrayList<>();
        news.add(News.builder().datetime(123123L).headline("HL1").id(1L).summary("s1").image("i1").source("s1").category("c1").build());
        news.add(News.builder().datetime(4123123L).headline("HL2").id(2L).summary("s2").image("i2").source("s2").category("c2").build());
        news.add(News.builder().datetime(1230330L).headline("HL3").id(3L).summary("s3").image("i3").source("s3").category("c3").build());
        return news;
    }
}