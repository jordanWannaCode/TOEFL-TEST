import { Component, inject, signal } from '@angular/core';
import { DatePipe, PercentPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { Api, errorMessage } from '../../core/api';
import { Blueprint, Dashboard } from '../../core/models';
@Component({
  imports: [DatePipe, PercentPipe, RouterLink, MatButtonModule, MatCardModule],
  template: `<p class="eyebrow">READING / PRACTICE</p><h1>Your progress, one practice at a time</h1><p class="subtitle">Work with original passages. Save your answers, then review the reasoning.</p>@if(error()) {<p role="alert" class="error">{{error()}}</p><button mat-button (click)="load()">Refresh dashboard</button>}@if(data(); as dashboard) {<div class="grid gap-5 md:grid-cols-2 mb-8"><mat-card><mat-card-content><p class="muted">Completed practices</p><p class="metric" data-testid="completed-count">{{dashboard.completedSessions}}</p></mat-card-content></mat-card><mat-card><mat-card-content><p class="muted">Reading accuracy</p><p class="metric">{{(readingAccuracy(dashboard) | percent:'1.0-0') || '—'}}</p><p class="muted">Based on saved, closed practices.</p></mat-card-content></mat-card></div><mat-card class="mb-8"><mat-card-content><h2>Reading practice</h2><p>Complete the Words · Read in Daily Life · Read an Academic Passage</p><p class="muted">{{blueprint()?.durationSeconds ? 'Timed custom practice' : 'Untimed custom practice'}} · Original starter exercises · No official score conversion</p><button mat-flat-button (click)="start()" [disabled]="busy() || !blueprint()">{{busy() ? 'Starting…' : 'Start Reading practice'}}</button></mat-card-content></mat-card><mat-card class="mb-8"><mat-card-content><h2>Listening</h2><p>Four task types, original audio, practice and custom timed sections.</p><p class="muted">Listening accuracy: {{(listeningAccuracy(dashboard) | percent:'1.0-0') || '—'}}</p><a mat-flat-button routerLink="/listening">Open Listening practice</a></mat-card-content></mat-card><h2>Attempt history</h2>@if(!dashboard.recentAttempts.length) {<p class="muted">No attempts yet. Your first practice will appear here.</p>}@else {<ul class="history">@for(attempt of dashboard.recentAttempts; track attempt.sessionId) {<li><div><strong>{{attempt.section === 'LISTENING' ? 'Listening' : 'Reading'}} {{attempt.mode === 'SECTION_TEST' ? 'section test' : 'practice'}}</strong><p class="muted">{{attempt.startedAt | date:'medium'}} · {{attempt.state}}</p></div><div>@if(attempt.objectiveScore) {<span class="score">{{attempt.objectiveScore.correct}} / {{attempt.objectiveScore.total}}</span>}<a mat-stroked-button [routerLink]="[attempt.state === 'ACTIVE' ? '/practice' : '/results', attempt.sessionId]">{{attempt.state === 'ACTIVE' ? 'Resume practice' : 'View results'}}</a></div></li>}</ul>}}@else if(!error()) {<p role="status">Loading your progress…</p>}`
})
export class DashboardComponent {
  private api = inject(Api); private router = inject(Router);
  readonly data = signal<Dashboard | null>(null); readonly blueprint = signal<Blueprint | null>(null); readonly busy = signal(false); readonly error = signal('');
  private startKey: string | null = null;
  constructor() { void this.load(); }
  readingAccuracy(data: Dashboard) { return data.sectionMetrics.find(metric => metric.section === 'READING')?.objectiveAccuracy ?? null; }
  listeningAccuracy(data: Dashboard) { return data.sectionMetrics.find(metric => metric.section === 'LISTENING')?.objectiveAccuracy ?? null; }
  async load() { this.error.set(''); try { const [data, blueprints] = await Promise.all([this.api.dashboard(), this.api.blueprints()]); this.data.set(data); this.blueprint.set(blueprints[0] ?? null); } catch(error) { this.error.set(errorMessage(error)); } }
  async start() {
    const blueprint = this.blueprint(); if(!blueprint || this.busy()) return;
    this.busy.set(true); this.error.set('');
    // Retain the creation key across retries and refreshes after a lost acknowledgement.
    const storage = `toefl:${this.api.user()?.id}:start:${blueprint.id}`;
    this.startKey = sessionStorage.getItem(storage) ?? crypto.randomUUID(); sessionStorage.setItem(storage, this.startKey);
    try { const session = await this.api.start(blueprint.id, this.startKey); sessionStorage.removeItem(storage); this.startKey = null; await this.router.navigate(['/practice', session.id]); }
    catch(error) { this.api.resetCsrf(); this.error.set(errorMessage(error)); }
    finally { this.busy.set(false); }
  }
}
