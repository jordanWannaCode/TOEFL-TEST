import {Component,inject,signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Router,RouterLink} from '@angular/router';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {Api,errorMessage} from '../../core/api';
import {WritingExercise} from './models';
@Component({imports:[FormsModule,RouterLink,MatButtonModule,MatCardModule],template:`<a routerLink="/">← Dashboard</a><h1>Writing practice</h1><p>Original exercises. AI scores are internal estimates, never official ETS scores. When configured, an external AI provider receives your response after submission. Avoid personal information.</p><label for="writing-mode">Mode</label><select id="writing-mode" [ngModel]="mode" (ngModelChange)="mode=$event; load()"><option value="PRACTICE">Practice · flexible timing</option><option value="SECTION_TEST">Custom timed test</option></select>@if(error()){<p role="alert">{{error()}}</p>}@for(exercise of exercises();track exercise.id){<mat-card class="mb-5"><mat-card-content><h2>{{exercise.content.title}}</h2><p>{{exercise.content.taskType}} · {{exercise.content.difficulty}}</p><p>{{exercise.content.instructions}}</p><button mat-flat-button (click)="start(exercise)" [disabled]="busy()">Start {{exercise.content.title}}</button></mat-card-content></mat-card>}`})
export class WritingCatalogComponent {
 private api=inject(Api);private router=inject(Router);mode='PRACTICE';exercises=signal<WritingExercise[]>([]);busy=signal(false);error=signal('');
 constructor(){void this.load();}
 async load(){this.exercises.set([]);this.error.set('');try{this.exercises.set(await this.api.writingExercises(this.mode));}catch(e){this.error.set(errorMessage(e));}}
 async start(exercise:WritingExercise){if(this.busy())return;this.busy.set(true);this.error.set('');const storage=`toefl:${this.api.user()?.id}:start:${exercise.blueprintVersionId}`;const key=sessionStorage.getItem(storage)??crypto.randomUUID();sessionStorage.setItem(storage,key);try{const session=await this.api.start(exercise.blueprintVersionId,key,this.mode);sessionStorage.removeItem(storage);await this.router.navigate(['/writing/session',session.id]);}catch(e){this.api.resetCsrf();this.error.set(errorMessage(e));}finally{this.busy.set(false);}}
}
