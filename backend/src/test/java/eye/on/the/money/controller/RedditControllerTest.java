package eye.on.the.money.controller;

import eye.on.the.money.dto.in.RedditPostDTO;
import eye.on.the.money.service.reddit.RedditService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class RedditControllerTest {

    @Mock
    private RedditService redditService;

    @InjectMocks
    private RedditController redditController;

    @Test
    public void getToken() {
        List<RedditPostDTO> postList = this.createPostList();
        when(this.redditService.getHotNewsFromSubreddits()).thenReturn(postList);

        ResponseEntity<List<RedditPostDTO>> result = this.redditController.getHotPosts();
        Assertions.assertEquals(postList, result.getBody());
    }

    private List<RedditPostDTO> createPostList() {
        List<RedditPostDTO> postList = new ArrayList<>();
        postList.add(RedditPostDTO.builder().stickied(false).permalink("link").selftext("text").thumbnail("tn").title("title").build());
        postList.add(RedditPostDTO.builder().stickied(true).permalink("link2").selftext("text2").thumbnail("tn2").title("title2").build());
        postList.add(RedditPostDTO.builder().stickied(false).permalink("link3").selftext("text3").thumbnail("tn3").title("title3").build());

        return postList;
    }
}