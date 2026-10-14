import { Component, inject, signal, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { Api, errorMessage } from '../../core/api';
import { PendingSave, SessionItem, SessionView } from '../../core/models';
import { CompleteWordsComponent } from './complete-words.component';
import { DailyLifeComponent } from './daily-life.component';
import { AcademicPassageComponent } from './academic-passage.component';
@Component({
  imports: [RouterLink, MatButtonModule, MatCardModule, CompleteWordsComponent, DailyLifeComponent, AcademicPassageComponent],
  template: `<a routerLink="/" class="back-link">← Dashboard</a>@if(error()) {<p class="error" role="alert">{{error()}}</p><a routerLink="/login">Sign in again</a>}@if(session(); as view) {<div class="practice-heading"><div><p class="eyebrow">READING / PRACTICE</p><h1>{{current()?.content?.title}}</h1></div><p role="timer" class="timer">{{timerLabel()}}</p></div><nav aria-label="Practice items" class="item-nav">@for(item of view.items; track item.id; let i = $index) {<button mat-stroked-button (click)="select(i)" [attr.aria-current]="index() === i ? 'step' : null">{{i+1}}. {{taskLabel(item.content.taskType)}}</button>}</nav>@if(current(); as item) {<mat-card><mat-card-content><p class="muted">{{item.content.instructions}}</p>@switch(item.content.taskType) {@case('COMPLETE_THE_WORDS') {<app-complete-words [content]="item.content" [answers]="answers(item)" [disabled]="busy() || !!pending()" (answersChange)="change(item,$event)"/>}@case('READ_IN_DAILY_LIFE') {<app-daily-life [content]="item.content" [answers]="answers(item)" [disabled]="busy() || !!pending()" (answersChange)="change(item,$event)"/>}@case('READ_AN_ACADEMIC_PASSAGE') {<app-academic-passage [content]="item.content" [answers]="answers(item)" [disabled]="busy() || !!pending()" (answersChange)="change(item,$event)"/>}}<div class="answer-actions"><button mat-flat-button (click)="save(item)" [disabled]="busy() || !!pending() || !isDirty(item)">Save answer</button><span role="status">{{pending()?.itemId === item.id ? 'Save awaiting confirmation' : (isDirty(item) ? 'Not yet saved on the server' : (item.savedAt ? 'Saved on the server' : 'No answer saved'))}}</span></div></mat-card-content></mat-card>}@if(pending()) {<div class="notice"><p>Your save is awaiting confirmation. Retry sends the same request safely.</p><button mat-stroked-button (click)="retry()" [disabled]="busy()">Retry saving</button></div>}<div class="session-actions"><button mat-button (click)="reload()" [disabled]="busy()">Reload server answers</button><button mat-flat-button (click)="submit()" [disabled]="busy() || !!pending() || hasDirty()">Submit practice</button></div><p class="muted">Save changed answers before submitting. Unanswered items score zero. Corrections appear only after submission.</p><p class="muted">This is custom practice with original content, not an official ETS examination.</p>}@else if(!error()) {<p role="status">Loading your saved practice…</p>}`
})
export class SessionComponent implements OnDestroy {
  private api = inject(Api); private route = inject(ActivatedRoute); private router = inject(Router);
  private id = this.route.snapshot.paramMap.get('id')!;
  private storage = `toefl:${this.api.user()?.id}:session:${this.id}`;
  readonly session = signal<SessionView | null>(null); readonly index = signal(0); readonly error = signal(''); readonly busy = signal(false); readonly timerLabel = signal('Untimed practice'); readonly pending = signal<PendingSave | null>(null);
  private drafts = signal<Record<string,Record<string,string>>>({}); private serverAnchor = 0; private monotonicAnchor = 0;
  private submitKey: string | null = null; private timer: ReturnType<typeof setInterval>;
  constructor() {
    try { const saved = JSON.parse(sessionStorage.getItem(this.storage) ?? '{}'); this.drafts.set(saved.drafts ?? {}); this.pending.set(saved.pending ?? null); this.submitKey = saved.submitKey ?? null; } catch { sessionStorage.removeItem(this.storage); }
    void this.reload(); this.timer = setInterval(() => this.tick(), 1000);
  }
  ngOnDestroy() { clearInterval(this.timer); }
  current() { return this.session()?.items[this.index()]; }
  select(index: number) { this.index.set(index); }
  answers(item: SessionItem) { return this.drafts()[item.id] ?? item.response.answers; }
  change(item: SessionItem, answers: Record<string,string>) { this.drafts.update(values => ({ ...values, [item.id]: answers })); this.persist(); }
  isDirty(item: SessionItem) { const left = this.answers(item); const right = item.response.answers; return Object.keys({ ...left,...right }).some(key => (left[key] ?? '') !== (right[key] ?? '')); }
  hasDirty() { return this.session()?.items.some(item => this.isDirty(item)) ?? false; }
  private persist() { sessionStorage.setItem(this.storage, JSON.stringify({ drafts: this.drafts(), pending: this.pending(), submitKey: this.submitKey })); }
  async reload(clearError = true) {
    if(this.busy()) return;
    if(clearError) this.error.set('');
    try {
      const session = await this.api.session(this.id);
      if((this.session()?.version ?? 0) > session.version) return;
      this.session.set(session);
      this.serverAnchor = Date.parse(session.serverTime); this.monotonicAnchor = performance.now();
      if(session.state !== 'ACTIVE') { sessionStorage.removeItem(this.storage); await this.router.navigate(['/results',this.id]); return; }
      this.tick();
    } catch(error) { this.error.set(errorMessage(error)); }
  }
  async save(item: SessionItem) {
    if(this.busy() || this.pending()) return; const view = this.session(); if(!view) return;
    this.pending.set({ itemId: item.id, body: { answers: { ...this.answers(item) } }, version: view.version, key: crypto.randomUUID() }); this.persist(); await this.retry();
  }
  async retry() {
    const pending = this.pending(); if(!pending || this.busy()) return; this.busy.set(true); this.error.set(''); let failed = false;
    try {
      await this.api.save(this.id,pending.itemId,pending.body,pending.version,pending.key);
      this.pending.set(null); this.drafts.update(drafts => { const next = { ...drafts }; delete next[pending.itemId]; return next; }); this.persist();
    } catch(error) {
      failed = true; this.api.resetCsrf(); this.error.set(errorMessage(error));
      // A conflict proves the request was rejected, unlike a lost acknowledgement.
      if(error instanceof Object && 'status' in error && [409,410,422].includes(Number(error.status))) { this.pending.set(null); this.persist(); }
    } finally { this.busy.set(false); }
    await this.reload(!failed);
  }
  async submit() {
    const view = this.session(); if(!view || this.busy() || this.pending() || this.hasDirty()) return;
    if(!confirm('Submit this practice? Answers will be locked and unanswered items will score zero.')) return;
    this.busy.set(true); this.error.set(''); this.submitKey ??= crypto.randomUUID(); this.persist();
    try { await this.api.submit(this.id,view.version,this.submitKey); sessionStorage.removeItem(this.storage); await this.router.navigate(['/results',this.id]); }
    catch(error) { this.api.resetCsrf(); this.error.set(errorMessage(error)); }
    finally { this.busy.set(false); }
  }
  private tick() {
    const session = this.session(); if(!session?.expiresAt) { this.timerLabel.set('Untimed practice'); return; }
    const remaining = Math.max(0,Math.ceil((Date.parse(session.expiresAt) - this.serverAnchor - (performance.now()-this.monotonicAnchor))/1000));
    this.timerLabel.set(`${Math.floor(remaining/60)}:${String(remaining%60).padStart(2,'0')} remaining`);
    if(remaining === 0 && !this.busy()) { this.timerLabel.set('Checking server deadline…'); void this.reload(); }
  }
  taskLabel(type: string) { return ({ COMPLETE_THE_WORDS:'Complete the Words', READ_IN_DAILY_LIFE:'Daily Life', READ_AN_ACADEMIC_PASSAGE:'Academic Passage' } as Record<string,string>)[type] ?? type; }
}
