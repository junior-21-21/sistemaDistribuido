import { TestBed } from '@angular/core/testing';

import { Grado } from './grado';

describe('Grado', () => {
  let service: Grado;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Grado);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
