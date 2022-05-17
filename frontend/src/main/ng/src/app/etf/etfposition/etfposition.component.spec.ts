import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EtfpositionComponent } from './etfposition.component';

describe('EtfpositionComponent', () => {
  let component: EtfpositionComponent;
  let fixture: ComponentFixture<EtfpositionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ EtfpositionComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(EtfpositionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
