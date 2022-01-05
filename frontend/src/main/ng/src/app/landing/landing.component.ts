import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UserService } from '../service/user.service';

@Component({
  selector: 'app-landing',
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.css']
})
export class LandingComponent implements OnInit {

  constructor(private router: Router, private userService: UserService) {
    if (localStorage.getItem('token') != null) {
      this.userService.validateToken().subscribe({
        next: () => this.navigateOn('./home'),
        error: () => { 
          localStorage.removeItem('token');
          this.navigateOn('./login') 
        },
      })
    } else {
      this.navigateOn('./login');
    }
  }

  ngOnInit(): void {
  }

  navigateOn(path: string) {
    this.router.navigate([path]);
  }
}
