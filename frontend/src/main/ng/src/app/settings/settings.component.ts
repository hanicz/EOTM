import { Component, OnInit } from '@angular/core';
import { NewsService } from '../service/news.service';
import { Subreddit } from '../model/subreddit';
import { UserService } from '../service/user.service';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css']
})
export class SettingsComponent implements OnInit {

  subReddits: Subreddit[] = [];
  oldPassword: string = '';
  newPassword: string = '';

  constructor(private newsService: NewsService, private userService: UserService) {

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

  changePassword() {
    this.userService.changePassword(this.oldPassword, this.newPassword).subscribe(() => {
      this.oldPassword = '';
      this.newPassword = '';
    });
  }
}
