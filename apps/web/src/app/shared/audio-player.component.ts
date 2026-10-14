import { Component, Input, Output, EventEmitter, ViewChild, ElementRef, OnChanges, OnDestroy, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { Api, errorMessage } from '../core/api';
import { AudioStimulus, PlaybackState } from '../core/models';
@Component({
  selector: 'app-audio-player', imports: [MatButtonModule],
  template: `<section aria-label="Audio stimulus" class="audio-panel"><p class="muted">{{stimulus.synthetic ? 'Synthetic audio' : 'Recorded audio'}} · {{speakerLabels()}}</p><audio #audio preload="metadata" (ended)="ended()" (error)="loadingError()" (timeupdate)="track()" (seeking)="checkSeek()"></audio><div class="answer-actions"><button mat-flat-button (click)="play()" [disabled]="loading() || playing() || replayLocked()">{{review ? 'Play review audio' : (state()?.audioUrl && !state()?.completed ? 'Resume audio' : (state()?.completed ? 'Replay audio' : 'Play audio'))}}</button>@if(state()?.policy?.allowPause && playing()) {<button mat-stroked-button (click)="pause()">Pause audio</button>}<span data-testid="audio-status" role="status">{{status()}}</span></div>@if(state()?.policy?.allowSeek) {<label class="muted">Playback position <input aria-label="Playback position" type="range" [min]="0" [max]="stimulus.durationMs / 1000" [value]="position()" (input)="seek($any($event.target).value)" [disabled]="!sourceLoaded()"></label>}@else {<p class="muted">No seeking or pause · {{state()?.policy?.maxPlays}} authorized play · Resume: {{state()?.policy?.resumeAllowed ? 'allowed' : 'disabled'}}</p>}@if(error()) {<p role="alert" class="error">{{error()}}</p><button mat-stroked-button (click)="retryLoad()" [disabled]="loading()">Retry audio loading</button>@if(pendingCompletion) {<button mat-stroked-button (click)="report(true)">Retry playback confirmation</button>}}</section>`
})
export class AudioPlayerComponent implements OnChanges, OnDestroy {
  @Input({ required: true }) sessionId!: string; @Input({ required: true }) itemId!: string; @Input({ required: true }) stimulus!: AudioStimulus; @Input() review = false;
  @Output() completedChange = new EventEmitter<boolean>(); @ViewChild('audio', { static: true }) audio!: ElementRef<HTMLAudioElement>;
  private api = inject(Api); private key = ''; private interval: ReturnType<typeof setInterval> | null = null; private requestKey: string | null = null; private safePosition = 0; private initialSeek = false; private reporting = false;
  readonly state = signal<PlaybackState | null>(null); readonly error = signal(''); readonly status = signal('Loading playback policy…'); readonly loading = signal(false); readonly playing = signal(false); readonly position = signal(0); readonly sourceLoaded = signal(false);
  pendingCompletion = false;
  ngOnChanges() {
    const key = `${this.sessionId}:${this.itemId}:${this.review}`; if(key === this.key) return;
    this.key = key; this.audio?.nativeElement.pause(); this.playing.set(false); this.sourceLoaded.set(false); this.state.set(null); this.error.set(''); this.pendingCompletion = false;
    this.completedChange.emit(false); void this.load();
  }
  ngOnDestroy() { if(this.interval) clearInterval(this.interval); this.audio.nativeElement.pause(); }
  speakerLabels() { return this.stimulus.speakers.map(s => s.label).join(' / '); }
  replayLocked() { const s = this.state(); return !!s && s.policy.maxPlays > 0 && s.plays >= s.policy.maxPlays && (s.completed || !s.audioUrl || !s.policy.resumeAllowed); }
  private storageKey() { return `toefl:${this.api.user()?.id}:audio:${this.key}`; }
  private async load() {
    const key = this.key;
    try {
      const state = await this.api.playback(this.sessionId,this.itemId); if(key !== this.key) return;
      this.state.set(state); this.position.set(state.positionMs/1000); this.status.set(!this.review && state.completed ? 'Audio finished' : 'Ready to listen');
      this.completedChange.emit(!this.review && state.completed);
      this.requestKey = sessionStorage.getItem(this.storageKey());
    } catch(error) { this.error.set(errorMessage(error)); }
  }
  async play() {
    if(this.loading() || this.playing() || this.replayLocked()) return;
    this.loading.set(true); this.error.set(''); this.status.set('Loading audio…');
    try {
      let state = this.state(); if(!state) { await this.load(); state = this.state(); } if(!state) return;
      let url = state.audioUrl; let grantId = state.grantId; const replay = state.completed || this.review;
      if(!url || replay) {
        this.requestKey ??= crypto.randomUUID(); sessionStorage.setItem(this.storageKey(),this.requestKey);
        const grant = await this.api.play(this.sessionId,this.itemId,this.requestKey); url = grant.audioUrl; grantId = grant.grantId;
        sessionStorage.removeItem(this.storageKey()); this.requestKey = null;
        state = { ...state, ...grant, positionMs: 0, completed: false, plays: state.plays + (this.review ? 0 : 1) }; this.state.set(state); this.completedChange.emit(false);
      }
      const audio = this.audio.nativeElement;
      if(audio.getAttribute('src') !== url || audio.readyState === 0 || audio.error) {
        audio.src = url!; this.initialSeek = true;
        await new Promise<void>((resolve,reject) => {
          const ready = () => { cleanup(); audio.currentTime = Math.min(state!.positionMs/1000, Number.isFinite(audio.duration) ? audio.duration : 0); this.safePosition = audio.currentTime; resolve(); };
          const fail = () => { cleanup(); reject(new Error('Audio could not be decoded.')); };
          const timeout = setTimeout(fail,15000); const cleanup = () => { clearTimeout(timeout); audio.removeEventListener('loadedmetadata',ready); audio.removeEventListener('error',fail); };
          audio.addEventListener('loadedmetadata',ready,{ once: true }); audio.addEventListener('error',fail,{ once: true }); audio.load();
        });
      }
      this.sourceLoaded.set(true);
      if(!state.policy.allowPause && state.positionMs >= state.durationMs) { this.pendingCompletion = true; await this.report(true); return; }
      await audio.play(); this.initialSeek = false; this.playing.set(true); this.status.set('Playing audio');
      if(!this.interval) this.interval = setInterval(() => { if(this.playing()) void this.report(false); },2000);
    } catch(error) { this.error.set('Audio loading or playback failed. Retry uses the existing authorization.'); this.playing.set(false); this.status.set('Audio unavailable'); }
    finally { this.loading.set(false); }
  }
  pause() { if(this.state()?.policy.allowPause) { this.audio.nativeElement.pause(); this.playing.set(false); this.status.set('Paused'); void this.report(false); } }
  seek(value: string) { if(this.state()?.policy.allowSeek && this.sourceLoaded()) { this.audio.nativeElement.currentTime = Number(value); this.safePosition = Number(value); } }
  checkSeek() { const audio = this.audio.nativeElement; if(!this.initialSeek && !this.state()?.policy.allowSeek && Math.abs(audio.currentTime-this.safePosition)>0.5) { this.initialSeek = true; audio.currentTime = this.safePosition; queueMicrotask(() => this.initialSeek = false); } }
  track() { this.position.set(this.audio.nativeElement.currentTime); if(!this.audio.nativeElement.seeking) this.safePosition = this.audio.nativeElement.currentTime; }
  ended() { this.playing.set(false); this.pendingCompletion = true; this.status.set('Confirming audio completion…'); void this.report(true); }
  async report(completed: boolean) {
    const state = this.state(); if(!state?.grantId || this.reporting) return; this.reporting = true; const key = this.key;
    try {
      const result = await this.api.audioProgress(this.sessionId,this.itemId,state.grantId,Math.round(this.audio.nativeElement.currentTime*1000),completed);
      if(key !== this.key) return; this.state.set(result); this.completedChange.emit(!this.review && result.completed);
      if(completed) { this.pendingCompletion = false; this.status.set('Audio finished'); this.error.set(''); }
    } catch(error) { if(key === this.key) this.error.set(errorMessage(error)); }
    finally { this.reporting = false; if(this.pendingCompletion && !completed) void this.report(true); }
  }
  loadingError() { this.error.set('Audio unavailable. Retry loading the authorized audio.'); this.status.set('Audio unavailable'); this.playing.set(false); }
  async retryLoad() { if(this.pendingCompletion) { await this.report(true); return; } await this.load(); await this.play(); }
}
