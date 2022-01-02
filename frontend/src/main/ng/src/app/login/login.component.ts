import { Component, OnInit, HostListener } from '@angular/core';
import { User } from '../model/user';
import { UserService } from '../service/user.service';
import { Router } from '@angular/router';
import { FormControl, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';

@Component({
  selector: 'login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {

  user = new User();

  emailControl = new FormControl('', [Validators.required, Validators.email]);
  passwordControl = new FormControl('', [Validators.required]);


  constructor(private userService: UserService,
    private router: Router,
    private messageService: MessageService) { }

  @HostListener('window:keydown', ['$event'])
  keyboardInput(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      this.login();
    }
  }

  login() {
    if (this.emailControl.valid && this.passwordControl.valid) {
      this.userService.loginUser(this.user).subscribe({
        next: (data) => {
          this.router.navigate(['./home'])
        },
        error: () => {
          this.showError();
        }
      });
    }
  }

  private showError() {
    this.messageService.add({ severity: 'error', detail: 'Login failed!' });
  }
}
