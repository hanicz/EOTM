import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { FinancialCategoryReportComponent } from './category.component';

describe('FinancialCategoryReportComponent', () => {
  let component: FinancialCategoryReportComponent;
  let fixture: ComponentFixture<FinancialCategoryReportComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FinancialCategoryReportComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    fixture = TestBed.createComponent(FinancialCategoryReportComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
