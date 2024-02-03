import { Component, OnInit } from '@angular/core';
import { News } from '../model/news';
import { NewsService } from '../service/news.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {

  selectedType = 'category/general';
  options: any[];

  constructor(private newsService: NewsService) {
    this.options = [
      { label: 'Reddit', value: 'category/reddit' },
      { label: 'General', value: 'category/general' },
      { label: 'Forex', value: 'category/forex' },
      { label: 'Crypto', value: 'category/crypto' }
    ];
  }

  ngOnInit(): void {
  }

  typeChanged() {

  }
}
