import { Component, Input, Output, EventEmitter } from '@angular/core';
import { ReadingContent } from '../../core/models';
import { ChoiceQuestionComponent } from './choice-question.component';
@Component({ selector: 'app-daily-life', imports: [ChoiceQuestionComponent], template: `<app-choice-question [content]="content" [answers]="answers" [disabled]="disabled" (answersChange)="answersChange.emit($event)" />` })
export class DailyLifeComponent {
  @Input({ required: true }) content!: ReadingContent; @Input({ required: true }) answers!: Record<string,string>; @Input() disabled = false;
  @Output() answersChange = new EventEmitter<Record<string,string>>();
}
