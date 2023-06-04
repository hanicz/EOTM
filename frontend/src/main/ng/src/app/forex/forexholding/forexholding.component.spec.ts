import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ForexholdingComponent } from './forexholding.component';

describe('ForexholdingComponent', () => {
  let component: ForexholdingComponent;
  let fixture: ComponentFixture<ForexholdingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ForexholdingComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ForexholdingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
