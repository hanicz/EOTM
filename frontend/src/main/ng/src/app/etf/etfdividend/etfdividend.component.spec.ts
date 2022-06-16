import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EtfdividendComponent } from './etfdividend.component';

describe('EtfdividendComponent', () => {
  let component: EtfdividendComponent;
  let fixture: ComponentFixture<EtfdividendComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ EtfdividendComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(EtfdividendComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
