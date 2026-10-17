import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuditoriaService, RevisionAuditoria } from '../../services/auditoria.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RealtimeService } from '../../core/realtime/realtime.service';

@Component({selector:'app-auditoria',standalone:true,imports:[CommonModule,FormsModule],templateUrl:'./auditoria.html',styleUrl:'./auditoria.css'})
export class Auditoria implements OnInit {private readonly realtime=inject(RealtimeService);private readonly destroyRef=inject(DestroyRef);
  private readonly service=inject(AuditoriaService);
  filtros: Record<string,string|number>={entidad:'ESTUDIANTE',entidadId:'',usuario:'',desde:'',hasta:''};
  registros: RevisionAuditoria[]=[]; pagina=0; totalPaginas=0; cargando=false; error='';
  ngOnInit():void{this.buscar();this.realtime.alCambiar().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(()=>this.buscar(this.pagina));}
  buscar(page=0):void{this.cargando=true;this.error='';this.service.buscar(this.filtros,page).pipe(finalize(()=>this.cargando=false)).subscribe({next:r=>{this.registros=r.content;this.pagina=r.number;this.totalPaginas=r.totalPages;},error:()=>this.error='No se pudo consultar el historial de auditoría.'});}
  claves(item:RevisionAuditoria):string[]{return Object.keys(item.valores);}
}
