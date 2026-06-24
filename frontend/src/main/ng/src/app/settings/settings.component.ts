import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { NewsService } from '../service/news.service';
import { Subreddit } from '../model/subreddit';
import { UserService } from '../service/user.service';
import { AccountService } from '../service/account.service';
import { Account } from '../model/account';
import { MenuComponent } from '../menu/menu.component';
import { Bind } from 'primeng/bind';
import { Panel } from 'primeng/panel';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';
import { DataView } from 'primeng/dataview';
import { PrimeTemplate } from 'primeng/api';
import { Password } from 'primeng/password';
import { FormsModule } from '@angular/forms';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { DatePicker } from 'primeng/datepicker';
import { DatePipe } from '@angular/common';

@Component({
    selector: 'app-settings',
    templateUrl: './settings.component.html',
    styleUrls: ['./settings.component.css'],
    imports: [MenuComponent, Bind, Panel, ButtonDirective, Ripple, DataView, PrimeTemplate, Password, FormsModule, Dialog, InputText, DatePicker, DatePipe]
})
export class SettingsComponent implements OnInit {

  // Data
  subReddits: Subreddit[] = [];
  accounts: Account[] = [];

  // Dialog states
  addSubRedditDialog: boolean = false;
  createAccountDialog: boolean = false;

  // Form data
  subReddit: string = '';
  description: string = '';
  newAccount: Account = {} as Account;
  oldPassword: string = '';
  newPassword: string = '';

  // Loading states
  isLoading: boolean = false;

  constructor(
    private newsService: NewsService,
    private userService: UserService,
    private accountService: AccountService,
    private cdr: ChangeDetectorRef,
  ) { }

  ngOnInit(): void {
    this.loadSubReddits();
    this.loadAccounts();
  }

  // Subreddit Methods
  loadSubReddits(): void {
    this.isLoading = true;
    this.newsService.getSubreddits().subscribe({
      next: (data) => {
        this.subReddits = data;
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Error loading subreddits:', error);
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  openSubredditDialog(): void {
    this.subReddit = '';
    this.description = '';
    this.addSubRedditDialog = true;
  }

  hideSubredditDialog(): void {
    this.addSubRedditDialog = false;
    this.subReddit = '';
    this.description = '';
  }

  saveSubReddit(): void {
    if (!this.subReddit.trim()) {
      return;
    }

    this.isLoading = true;
    this.newsService.addSubreddit(this.subReddit.trim(), this.description.trim()).subscribe({
      next: () => {
        this.loadSubReddits();
        this.hideSubredditDialog();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error adding subreddit:', error);
        this.isLoading = false;
      }
    });
  }

  delete(subReddit: Subreddit): void {
    if (!subReddit.id) return;
    
    if (confirm(`Are you sure you want to remove /r/${subReddit.subreddit}?`)) {
      this.isLoading = true;
      this.newsService.deleteSubReddit(subReddit.id).subscribe({
        next: () => {
          this.loadSubReddits();
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error deleting subreddit:', error);
          this.isLoading = false;
        }
      });
    }
  }

  // Account Methods
  loadAccounts(): void {
    this.isLoading = true;
    this.accountService.getAccounts().subscribe({
      next: (data) => {
        this.accounts = data;
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Error loading accounts:', error);
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  openAccountDialog(): void {
    this.newAccount = { creationDate: new Date() } as Account;
    this.createAccountDialog = true;
  }

  hideAccountDialog(): void {
    this.createAccountDialog = false;
    this.newAccount = {} as Account;
  }

  createAccount(): void {
    if (!this.newAccount.accountName?.trim()) {
      return;
    }

    if (!this.newAccount.creationDate) {
      return;
    }

    this.isLoading = true;
    this.accountService.createAccount(this.newAccount).subscribe({
      next: () => {
        this.loadAccounts();
        this.hideAccountDialog();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error creating account:', error);
        this.isLoading = false;
      }
    });
  }

  deleteAccount(account: Account): void {
    if (!account.id) return;

    if (confirm(`Are you sure you want to delete ${account.accountName}?`)) {
      this.isLoading = true;
      this.accountService.deleteAccount(account.id).subscribe({
        next: () => {
          this.loadAccounts();
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error deleting account:', error);
          this.isLoading = false;
        }
      });
    }
  }

  // Security Methods
  changePassword(): void {
    if (!this.oldPassword || !this.newPassword) {
      return;
    }

    if (this.oldPassword === this.newPassword) {
      return;
    }

    this.isLoading = true;
    this.userService.changePassword(this.oldPassword, this.newPassword).subscribe({
      next: () => {
        this.oldPassword = '';
        this.newPassword = '';
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('Error changing password:', error);
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }
}