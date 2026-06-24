import { Component, OnInit, HostListener } from '@angular/core';
import { User } from '../model/user';
import { UserService } from '../service/user.service';
import { Router } from '@angular/router';
import { UntypedFormControl, Validators, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { Bind } from 'primeng/bind';
import { Toast } from 'primeng/toast';
import { InputText } from 'primeng/inputtext';
import { ButtonDirective } from 'primeng/button';
import { Ripple } from 'primeng/ripple';

@Component({
    selector: 'login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.css'],
    imports: [Bind, Toast, FormsModule, InputText, ReactiveFormsModule, ButtonDirective, Ripple]
})
export class LoginComponent {

  user = new User();

  emailControl = new UntypedFormControl('', [Validators.required, Validators.email]);
  passwordControl = new UntypedFormControl('', [Validators.required]);


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
