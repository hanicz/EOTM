import { TestBed } from '@angular/core/testing';

import { EtfdividendService } from './etfdividend.service';

describe('EtfdividendService', () => {
  let service: EtfdividendService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(EtfdividendService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
