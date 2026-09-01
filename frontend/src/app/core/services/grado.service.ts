import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';
import { Grado } from '../models/grado.model';

@Injectable({
  providedIn: 'root'
})
export class GradoService {
  private apiUrl = `${environment.apiUrl}/grados`;

  constructor(private http: HttpClient) { }

  getGrados(): Observable<Grado[]> {
    return this.http.get<Grado[]>(this.apiUrl);
  }
}
