import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EtfinvestmentComponent } from './etfinvestment.component';

describe('EtfinvestmentComponent', () => {
  let component: EtfinvestmentComponent;
  let fixture: ComponentFixture<EtfinvestmentComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ EtfinvestmentComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(EtfinvestmentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
