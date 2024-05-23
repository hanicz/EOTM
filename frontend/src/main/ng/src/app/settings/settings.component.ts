import { Component, OnInit } from '@angular/core';
import { NewsService } from '../service/news.service';
import { Subreddit } from '../model/subreddit';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css']
})
export class SettingsComponent implements OnInit {

  subReddits: Subreddit[] = [];

  constructor(private newsService: NewsService) {

  }

  ngOnInit(): void {
    this.loadSubReddits();
  }

  loadSubReddits() {
    this.newsService.getSubreddits().subscribe(data => {
      this.subReddits = data;
    });
  }

  delete(subReddit: Subreddit) {
    this.newsService.deleteSubReddit(subReddit.id).subscribe(() => {
      this.loadSubReddits();
    });
  }
}
