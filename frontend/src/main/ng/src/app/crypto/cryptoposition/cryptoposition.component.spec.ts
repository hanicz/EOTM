import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CryptopositionComponent } from './cryptoposition.component';

describe('CryptopositionComponent', () => {
  let component: CryptopositionComponent;
  let fixture: ComponentFixture<CryptopositionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CryptopositionComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CryptopositionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
