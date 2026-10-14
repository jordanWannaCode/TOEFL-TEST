import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { Api, errorMessage } from '../../core/api';
import { Blueprint, ListeningExercise } from '../../core/models';
@Component({
 imports: [FormsModule, MatButtonModule, MatCardModule],
 template: `<p class="eyebrow">LISTENING / ORIGINAL EXERCISES</p><h1>Listening practice</h1><p class="subtitle">Listen, answer and review. Original scripts with clearly labelled synthetic audio.</p><div class="filters"><label>Task type <select aria-label="Task type" [(ngModel)]="task" (ngModelChange)="load()"><option value="">All task types</option>@for(type of types; track type.id) {<option [value]="type.id">{{type.label}}</option>}</select></label><label>Difficulty <select aria-label="Difficulty" [(ngModel)]="difficulty" (ngModelChange)="load()"><option value="">All levels</option>@for(level of ['A2','B1','B2','C1']; track level) {<option [value]="level">{{level}}</option>}</select></label><label>Skill <select aria-label="Skill" [(ngModel)]="skill" (ngModelChange)="load()"><option value="">All skills</option>@for(s of ['INTENT','DETAIL','MAIN_IDEA','INFERENCE']; track s) {<option [value]="s">{{s}}</option>}</select></label></div>@if(error()) {<p role="alert" class="error">{{error()}}</p>}@if(loading()) {<p role="status">Loading exercises…</p>}@else {@for(exercise of exercises(); track exercise.id) {<mat-card class="mb-5"><mat-card-content><h2>{{exercise.title}}</h2><p class="muted">{{label(exercise.taskType)}} · {{exercise.difficulty}} · {{exercise.skills.join(', ')}}</p><p>{{exercise.stimulus.synthetic ? 'Synthetic audio' : 'Recorded audio'}} · {{(exercise.stimulus.durationMs / 1000).toFixed(1)}} seconds</p><button mat-flat-button (click)="start(exercise.blueprintVersionId,'PRACTICE')" [disabled]="busy()">Practice {{exercise.title}}</button></mat-card-content></mat-card>}@empty {<p class="muted">No approved exercises match these filters.</p>}}@if(section(); as blueprint) {<mat-card><mat-card-content><h2>Custom timed Listening section</h2><p>{{blueprint.durationSeconds}} seconds · Questions grouped by audio stimulus</p><p class="muted">{{blueprint.playbackPolicy?.maxPlays === 0 ? 'Replay allowed' : ('Play limit: ' + blueprint.playbackPolicy?.maxPlays)}} · {{blueprint.navigationPolicy}}. This is a custom section, not a calibrated adaptive ETS examination.</p><button mat-stroked-button (click)="start(blueprint.id,'SECTION_TEST')" [disabled]="busy()">Start custom Listening section</button></mat-card-content></mat-card>}`
})
export class ListeningCatalogComponent {
 private api = inject(Api); private router = inject(Router); task = ''; difficulty = ''; skill = '';
 readonly exercises = signal<ListeningExercise[]>([]); readonly section = signal<Blueprint | null>(null); readonly error = signal(''); readonly loading = signal(true); readonly busy = signal(false); private loadId = 0;
 readonly types = [{ id:'LISTEN_AND_CHOOSE_A_RESPONSE', label:'Listen and Choose a Response' },{ id:'LISTEN_TO_A_CONVERSATION', label:'Listen to a Conversation' },{ id:'LISTEN_TO_AN_ANNOUNCEMENT', label:'Listen to an Announcement' },{ id:'LISTEN_TO_AN_ACADEMIC_TALK', label:'Listen to an Academic Talk' }];
 constructor() { void this.load(); }
 label(id: string) { return this.types.find(type => type.id === id)?.label ?? id; }
 async load() { const id = ++this.loadId; this.loading.set(true); this.error.set(''); try { const [exercises,blueprints] = await Promise.all([this.api.exercises(this.task,this.difficulty,this.skill),this.api.blueprints('LISTENING','SECTION_TEST')]); if(id !== this.loadId) return; this.exercises.set(exercises); this.section.set(blueprints[0] ?? null); } catch(error) { if(id === this.loadId) this.error.set(errorMessage(error)); } finally { if(id === this.loadId) this.loading.set(false); } }
 async start(id: string,mode: string) {
   if(this.busy()) return; this.busy.set(true); this.error.set(''); const storage = `toefl:${this.api.user()?.id}:start:${id}`; const key = sessionStorage.getItem(storage) ?? crypto.randomUUID(); sessionStorage.setItem(storage,key);
   try { const session = await this.api.start(id,key,mode); sessionStorage.removeItem(storage); await this.router.navigate(['/practice',session.id]); } catch(error) { this.api.resetCsrf(); this.error.set(errorMessage(error)); } finally { this.busy.set(false); }
 }
}
