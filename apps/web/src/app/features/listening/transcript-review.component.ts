import { Component, Input, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { Api, errorMessage } from '../../core/api';
import { TranscriptTurn } from '../../core/models';
@Component({ selector: 'app-transcript-review', imports: [MatButtonModule], template: `<button mat-stroked-button (click)="load()">Review transcript</button>@if(error()) {<p role="alert">{{error()}}</p>}@if(turns(); as text) {<section class="passage" aria-label="Transcript">@for(turn of text; track $index) {<p><strong>{{turn.speakerId}}:</strong> {{turn.text}}</p>}</section>}` })
export class TranscriptReviewComponent {
 @Input({ required:true }) sessionId!: string; @Input({ required:true }) itemId!: string;
 private api = inject(Api); readonly turns = signal<TranscriptTurn[] | null>(null); readonly error = signal('');
 async load() { this.error.set(''); try { this.turns.set(await this.api.transcript(this.sessionId,this.itemId)); } catch(error) { this.error.set(errorMessage(error)); } }
}
