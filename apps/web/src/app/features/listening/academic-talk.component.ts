import { Component, Input, Output, EventEmitter } from '@angular/core';
import { ReadingContent } from '../../core/models';
import { ListeningQuestionGroupComponent } from './question-group.component';
@Component({ selector: 'app-academic-talk', imports: [ListeningQuestionGroupComponent], template: `<app-listening-question-group [content]="content" [answers]="answers" [disabled]="disabled" (answersChange)="answersChange.emit($event)" />` })
export class AcademicTalkComponent {
  @Input({ required: true }) content!: ReadingContent; @Input({ required: true }) answers!: Record<string,string>; @Input() disabled = false;
  @Output() answersChange = new EventEmitter<Record<string,string>>();
}
