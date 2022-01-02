import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CryptoholdingComponent } from './cryptoholding.component';

describe('CryptoholdingComponent', () => {
  let component: CryptoholdingComponent;
  let fixture: ComponentFixture<CryptoholdingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CryptoholdingComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CryptoholdingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
