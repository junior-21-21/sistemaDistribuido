import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from './estudiante.service';

export interface Notificacion {id:number;eventoId:string;tipo:string;canal:string;estudianteId:number;asunto:string;destinatario:string|null;estado:string;intentos:number;maxIntentos:number;proximoIntento:string|null;ultimoError:string|null;correlationId:string|null;detalleProveedor:string|null;creadaEn:string;enviadaEn:string|null;}
@Injectable({providedIn:'root'}) export class NotificacionService{
  private readonly http=inject(HttpClient);
  buscar(f:Record<string,string|number>,page=0):Observable<PageResponse<Notificacion>>{let p=new HttpParams().set('page',page).set('size',25);Object.entries(f).forEach(([k,v])=>{if(v!=='')p=p.set(k,v)});return this.http.get<PageResponse<Notificacion>>('/api/notificaciones',{params:p});}
  reintentar(id:number):Observable<Notificacion>{return this.http.post<Notificacion>(`/api/notificaciones/${id}/reintentar`,{});}
}
