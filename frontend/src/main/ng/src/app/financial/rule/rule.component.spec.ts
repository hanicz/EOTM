import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { MessageService } from 'primeng/api';
import { FinancialRuleComponent } from './rule.component';

describe('FinancialRuleComponent', () => {
  let component: FinancialRuleComponent;
  let fixture: ComponentFixture<FinancialRuleComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FinancialRuleComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), MessageService]
    }).compileComponents();

    fixture = TestBed.createComponent(FinancialRuleComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
