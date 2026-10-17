import { Component, inject } from '@angular/core';
import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-toast-stack',
  standalone: true,
  templateUrl: './toast-stack.html',
  styleUrl: './toast-stack.css'
})
export class ToastStack {
  readonly toasts = inject(ToastService);
}
