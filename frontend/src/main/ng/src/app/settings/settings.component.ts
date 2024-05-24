import { Component, OnInit } from '@angular/core';
import { NewsService } from '../service/news.service';
import { Subreddit } from '../model/subreddit';
import { UserService } from '../service/user.service';
import { AccountService } from '../service/account.service';
import { Account } from '../model/account';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css']
})
export class SettingsComponent implements OnInit {

  subReddits: Subreddit[] = [];
  accounts: Account[] = [];
  oldPassword: string = '';
  newPassword: string = '';
  addSubRedditDialog: boolean = false;
  subReddit: string = '';
  description: string = '';

  constructor(private newsService: NewsService, private userService: UserService,
    private accountService: AccountService) { }

  ngOnInit(): void {
    this.loadSubReddits();
    this.loadAccounts();
  }

  loadSubReddits() {
    this.newsService.getSubreddits().subscribe(data => {
      this.subReddits = data;
    });
  }

  loadAccounts() {
    this.accountService.getAccounts().subscribe(data => {
      this.accounts = data;
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

  openDialog() {
    this.subReddit = '';
    this.description = '';
    this.addSubRedditDialog = true;
  }

  hideDialog() {
    this.addSubRedditDialog = false;
  }

  saveSubReddit() {
    this.newsService.addSubreddit(this.subReddit, this.description).subscribe(() => {
      this.loadSubReddits();
      this.addSubRedditDialog = false;
      this.subReddit = '';
      this.description = '';
    });
  }
}
