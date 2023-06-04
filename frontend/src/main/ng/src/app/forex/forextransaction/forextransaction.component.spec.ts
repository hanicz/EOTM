import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ForextransactionComponent } from './forextransaction.component';

describe('ForextransactionComponent', () => {
  let component: ForextransactionComponent;
  let fixture: ComponentFixture<ForextransactionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ForextransactionComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ForextransactionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
