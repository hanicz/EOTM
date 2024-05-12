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
    this.newsService.getSubreddits().subscribe(data => {
      this.subReddits = data
      console.log(this.subReddits);
    });
  }

}
