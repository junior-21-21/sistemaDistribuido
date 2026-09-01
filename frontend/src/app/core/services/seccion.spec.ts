import { TestBed } from '@angular/core/testing';

import { Seccion } from './seccion';

describe('Seccion', () => {
  let service: Seccion;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Seccion);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
