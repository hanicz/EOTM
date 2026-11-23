import { ChangeDetectorRef, Component, HostListener } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
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
import { environment } from '../../environments/environment';

@Component({
    selector: 'login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.css'],
    imports: [Bind, Toast, FormsModule, InputText, ReactiveFormsModule, ButtonDirective, Ripple]
})
export class LoginComponent {

  user = new User();
  assetUrl: string = environment.assets_url;

  emailControl = new UntypedFormControl('', [Validators.required, Validators.email]);
  passwordControl = new UntypedFormControl('', [Validators.required]);
  codeControl = new UntypedFormControl('', [Validators.required, Validators.pattern(/^\d{6}$/)]);

  mfaRequired = false;
  verifying = false;
  loggingIn = false;

  private mfaToken = '';

  constructor(private userService: UserService,
    private router: Router,
    private messageService: MessageService,
    private cdr: ChangeDetectorRef) { }

  @HostListener('window:keydown', ['$event'])
  keyboardInput(event: KeyboardEvent) {
    if (event.key !== 'Enter') {
      return;
    }
    if (this.mfaRequired) {
      this.verifyCode();
    } else {
      this.login();
    }
  }

  login() {
    if (!this.emailControl.valid || !this.passwordControl.valid || this.loggingIn) {
      return;
    }

    this.loggingIn = true;
    this.userService.loginUser(this.user).subscribe({
      next: (result) => {
        this.loggingIn = false;
        if (result.mfaRequired) {
          this.mfaToken = result.mfaToken ?? '';
          this.mfaRequired = true;
          this.codeControl.setValue('');
          this.cdr.markForCheck();
          return;
        }
        this.router.navigate(['./dashboard']);
      },
      error: (err: HttpErrorResponse) => {
        this.loggingIn = false;
        this.showError(err);
        this.cdr.markForCheck();
      }
    });
  }

  verifyCode() {
    if (!this.codeControl.valid || this.verifying) {
      return;
    }

    this.verifying = true;
    this.userService.verifyTotp(this.mfaToken, this.codeControl.value).subscribe({
      next: () => {
        this.verifying = false;
        this.router.navigate(['./dashboard']);
      },
      error: (err: HttpErrorResponse) => {
        this.verifying = false;
        this.codeControl.setValue('');
        this.showCodeError(err);
        this.cdr.markForCheck();
      }
    });
  }

  backToPassword() {
    this.mfaRequired = false;
    this.mfaToken = '';
    this.codeControl.setValue('');
    this.user.password = '';
    this.passwordControl.setValue('');
    this.cdr.markForCheck();
  }

  private showError(err: HttpErrorResponse) {
    if (err.status === 429) {
      this.messageService.add({ severity: 'warn', detail: 'Too many login attempts. Please wait a minute and try again.' });
      return;
    }
    this.messageService.add({ severity: 'error', detail: 'Login failed!' });
  }

  private showCodeError(err: HttpErrorResponse) {
    if (err.status === 429) {
      this.messageService.add({ severity: 'warn', detail: 'Too many attempts. Please wait a minute and try again.' });
      return;
    }
    this.messageService.add({ severity: 'error', detail: 'That code is not right, or it has expired.' });
  }
}
