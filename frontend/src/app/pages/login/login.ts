import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styles: [`
    .login-container {
      min-height: 100vh;
      background: linear-gradient(135deg, #0f2027 0%, #203a43 50%, #2c5364 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1rem;
    }
    
    .glass-card {
      background: rgba(255, 255, 255, 0.03);
      backdrop-filter: blur(16px);
      -webkit-backdrop-filter: blur(16px);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 24px;
      padding: 3rem;
      width: 100%;
      max-width: 440px;
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
      animation: float 6s ease-in-out infinite, fadeIn 0.8s ease-out forwards;
    }
    
    .logo-container {
      width: 64px;
      height: 64px;
      background: linear-gradient(135deg, #3b82f6, #1d4ed8);
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 1.5rem;
      color: white;
      font-weight: 800;
      font-size: 1.5rem;
      box-shadow: 0 10px 25px -5px rgba(37, 99, 235, 0.5);
      letter-spacing: 1px;
    }

    h2 {
      color: #f8fafc;
      font-weight: 700;
      letter-spacing: -0.025em;
    }
    
    .text-muted-light {
      color: #94a3b8;
    }

    .form-control-custom {
      background: rgba(15, 23, 42, 0.6);
      border: 1px solid rgba(255, 255, 255, 0.08);
      color: #f8fafc;
      border-radius: 12px;
      padding: 0.875rem 1rem;
      transition: all 0.3s ease;
    }
    
    .form-control-custom:focus {
      background: rgba(15, 23, 42, 0.8);
      border-color: #3b82f6;
      box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15);
      outline: none;
      color: white;
    }

    .form-control-custom::placeholder {
      color: #64748b;
    }

    .btn-login {
      background: linear-gradient(to right, #2563eb, #1d4ed8);
      color: white;
      border: none;
      border-radius: 12px;
      padding: 0.875rem;
      font-weight: 600;
      letter-spacing: 0.025em;
      transition: all 0.3s ease;
      box-shadow: 0 4px 14px 0 rgba(37, 99, 235, 0.39);
    }

    .btn-login:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(37, 99, 235, 0.23);
    }
    
    .btn-login:active {
      transform: translateY(0);
    }

    @keyframes float {
      0% { transform: translateY(0px); }
      50% { transform: translateY(-10px); }
      100% { transform: translateY(0px); }
    }
    
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(20px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class LoginComponent {
  credentials = { username: '', password: '' };
  isLoading = false;
  errorMessage = '';

  router = inject(Router);
  authService = inject(AuthService);

  /**
   * Login REAL: autentica contra el backend con JWT.
   */
  login() {
    this.isLoading = true;
    this.errorMessage = '';
    
    const { username, password } = this.credentials;

    // Intentar autenticación REAL contra el backend
    this.authService.login(username, password).subscribe({
      next: (response) => {
        if (response) {
          // Backend respondió OK con JWT
          console.log('Login exitoso con JWT para:', response.username);
          this.navigateToDashboard();
        } else {
          // Backend no retornó nada válido (o fue un error)
          this.errorMessage = 'Usuario o contraseña incorrectos.';
          this.isLoading = false;
        }
      },
      error: (err) => {
        if (err.status === 401) {
          this.errorMessage = 'Usuario o contraseña incorrectos.';
        } else if (err.status === 403) {
          this.errorMessage = 'Acceso denegado (403). Revisa los permisos o CORS.';
        } else {
          this.errorMessage = 'Error de conexión con el servidor. Por favor, intenta de nuevo.';
        }
        this.isLoading = false;
      }
    });
  }

  private navigateToDashboard() {
    this.router.navigate(['/dashboard']).then(success => {
      if (!success) {
        this.errorMessage = 'Error de redireccion. Contacte soporte.';
        this.isLoading = false;
      }
    }).catch(err => {
      this.errorMessage = 'Error interno del router: ' + err;
      this.isLoading = false;
    });
  }
}
