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
  createAccountDialog: boolean = false;
  subReddit: string = '';
  description: string = '';
  newAccount: Account = {} as Account;

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

  openSubredditDialog() {
    this.subReddit = '';
    this.description = '';
    this.addSubRedditDialog = true;
  }

  hideSubredditDialog() {
    this.addSubRedditDialog = false;
  }

  openAccountDialog() {
    this.newAccount = {} as Account;
    this.createAccountDialog = true;
  }

  hideAccountDialog() {
    this.createAccountDialog = false;
  }

  saveSubReddit() {
    this.newsService.addSubreddit(this.subReddit, this.description).subscribe(() => {
      this.loadSubReddits();
      this.addSubRedditDialog = false;
      this.subReddit = '';
      this.description = '';
    });
  }

  deleteAccount(account: Account) {
    this.accountService.deleteAccount(account.id).subscribe(() => {
      this.loadAccounts();
    });
  }

  createAccount() {
    this.accountService.createAccount(this.newAccount).subscribe(() => {
      this.loadAccounts();
      this.createAccountDialog = false;
    });
  }
}
