package eye.on.the.money.controller;

import eye.on.the.money.dto.in.RedditPostDTO;
import eye.on.the.money.service.reddit.RedditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/reddit")
@RequiredArgsConstructor
public class RedditController {

    private final RedditService redditService;

    @GetMapping
    public ResponseEntity<List<RedditPostDTO>> getHotPosts() {
        return new ResponseEntity<>(this.redditService.getHotNewsFromSubreddits(), HttpStatus.OK);
    }
}
