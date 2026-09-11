import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MessageService } from 'primeng/api';

import { Globals } from '../util/global';

import { EtfComponent } from './etf.component';

describe('EtfComponent', () => {
  let component: EtfComponent;
  let fixture: ComponentFixture<EtfComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [EtfComponent],
    providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations(), MessageService, Globals]
})
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(EtfComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
