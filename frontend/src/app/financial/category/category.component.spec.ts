import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { FinancialCategoryComponent } from './category.component';

describe('FinancialCategoryComponent', () => {
  let component: FinancialCategoryComponent;
  let fixture: ComponentFixture<FinancialCategoryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FinancialCategoryComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), MessageService]
    }).compileComponents();
    fixture = TestBed.createComponent(FinancialCategoryComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
