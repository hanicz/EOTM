import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {

  selectedType = 'category/reddit';
  options: any[];

  constructor() {
    this.options = [
      { label: 'Reddit', value: 'category/reddit' },
      { label: 'General', value: 'category/general' },
      { label: 'Forex', value: 'category/forex' },
      { label: 'Crypto', value: 'category/crypto' }
    ];
  }

  ngOnInit(): void {
  }
}
