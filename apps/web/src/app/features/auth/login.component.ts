import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Api, errorMessage } from '../../core/api';
@Component({
  imports: [FormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule],
  template: `<section class="mx-auto max-w-lg"><p class="eyebrow">YOUR PERSONAL STUDY SPACE</p><h1>Welcome back</h1><p class="subtitle">Sign in to continue your Reading practice.</p><mat-card><mat-card-content><form (ngSubmit)="login()"><mat-form-field class="w-full"><mat-label>Email</mat-label><input matInput type="email" name="email" [(ngModel)]="email" autocomplete="username" required></mat-form-field><mat-form-field class="w-full"><mat-label>Password</mat-label><input matInput type="password" name="password" [(ngModel)]="password" autocomplete="current-password" required></mat-form-field>@if (error()) {<p role="alert" class="error">{{ error() }}</p>}<button mat-flat-button type="submit" [disabled]="busy()">{{ busy() ? 'Signing in…' : 'Sign in' }}</button></form></mat-card-content></mat-card><p class="muted mt-6">Independent preparation tool. Practice results are not official ETS scores.</p></section>`
})
export class LoginComponent {
  private api = inject(Api); private router = inject(Router); private route = inject(ActivatedRoute);
  email = ''; password = ''; readonly busy = signal(false); readonly error = signal('');
  async login() {
    if (this.busy()) return; this.busy.set(true); this.error.set('');
    try { await this.api.login(this.email, this.password); this.password = ''; const url = this.route.snapshot.queryParamMap.get('returnUrl'); await this.router.navigateByUrl(url?.startsWith('/') && !url.startsWith('//') ? url : '/'); }
    catch (error) { this.api.resetCsrf(); this.error.set(errorMessage(error)); }
    finally { this.busy.set(false); }
  }
}
