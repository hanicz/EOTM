import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { FinancialCategoryRuleComponent } from './categoryrule.component';

describe('FinancialCategoryRuleComponent', () => {
  let component: FinancialCategoryRuleComponent;
  let fixture: ComponentFixture<FinancialCategoryRuleComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FinancialCategoryRuleComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), MessageService]
    }).compileComponents();
    fixture = TestBed.createComponent(FinancialCategoryRuleComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
