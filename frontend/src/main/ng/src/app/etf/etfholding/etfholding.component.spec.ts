import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EtfholdingComponent } from './etfholding.component';

describe('EtfholdingComponent', () => {
  let component: EtfholdingComponent;
  let fixture: ComponentFixture<EtfholdingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ EtfholdingComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(EtfholdingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
