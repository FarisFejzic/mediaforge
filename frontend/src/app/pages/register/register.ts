import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {
  private auth = inject(AuthService);
  private router = inject(Router);

  protected email = signal('');
  protected password = signal('');
  protected error = signal<string | null>(null);
  protected loading = signal(false);

  submit() {
    this.error.set(null);
    this.loading.set(true);
    const email = this.email();
    const password = this.password();

    this.auth.register(email, password).subscribe({
      next: () => {
        // register succeeded (user created, no token) → log in to get the token
        this.auth.login(email, password).subscribe({
          next: () => {
            this.loading.set(false);
            this.router.navigate(['/dashboard']);
          },
          error: () => {
            this.loading.set(false);
            this.error.set('Registered, but auto-login failed. Try logging in.');
          }
        });
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.status === 409
          ? 'That email is already registered.'
          : 'Registration failed. Try again.');
      }
    });
  }
}
