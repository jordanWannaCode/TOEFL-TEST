import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { Api, errorMessage } from '../../core/api';
import { Results } from '../../core/models';
@Component({
  imports: [RouterLink, MatButtonModule, MatCardModule],
  template: `<a routerLink="/" class="back-link">← Dashboard</a><p class="eyebrow">READING / REVIEW</p><h1>Your practice results</h1>@if(error()) {<p class="error" role="alert">{{error()}}</p><button mat-button (click)="load()">Retry loading results</button>}@if(results(); as result) {<mat-card class="mb-8"><mat-card-content><p class="metric" data-testid="objective-score">{{result.objectiveScore.correct}} / {{result.objectiveScore.total}}</p><p>Correct answers · Objective practice score</p><p class="muted">No AI scoring or official ETS score conversion is used.</p></mat-card-content></mat-card>@for(item of result.items; track item.itemId) {<mat-card class="mb-5"><mat-card-content><h2>{{item.title}}</h2><p class="score">{{item.correct}} / {{item.total}}</p>@for(key of keys(item.correctAnswers); track key) {<p>Your answer: <strong>{{item.response.answers[key] || 'Unanswered'}}</strong> · Correct answer: <strong>{{item.correctAnswers[key]}}</strong></p>}<p>{{item.explanation}}</p></mat-card-content></mat-card>}<a mat-flat-button routerLink="/">Back to dashboard</a>}@else if(!error()) {<p role="status">Loading your results…</p>}`
})
export class ResultsComponent {
  private api = inject(Api); private route = inject(ActivatedRoute);
  readonly results = signal<Results | null>(null); readonly error = signal('');
  constructor() { void this.load(); }
  keys(value: Record<string,string>) { return Object.keys(value); }
  async load() { this.error.set(''); try { this.results.set(await this.api.results(this.route.snapshot.paramMap.get('id')!)); } catch(error) { this.error.set(errorMessage(error)); } }
}
