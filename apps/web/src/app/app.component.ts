import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { Api, errorMessage } from './core/api';
@Component({
  selector: 'app-root', imports: [RouterLink, RouterOutlet, MatButtonModule],
  template: `<header class="site-header"><a class="brand" routerLink="/">TOEFL <span>AI Simulator</span></a><nav aria-label="Main navigation">@if(api.user()) {<a mat-button routerLink="/">Dashboard</a><button mat-button (click)="logout()">Sign out</button>}</nav></header><main class="page-container">@if(error()) {<p role="alert" class="error">{{error()}}</p>}<router-outlet /></main><footer>Independent TOEFL preparation · Original content · Practice scores, never official ETS scores</footer>`
})
export class AppComponent {
  readonly api = inject(Api); private router = inject(Router); readonly error = signal('');
  async logout() { try { await this.api.logout(); await this.router.navigate(['/login']); } catch(error) { this.error.set(errorMessage(error)); } }
}
