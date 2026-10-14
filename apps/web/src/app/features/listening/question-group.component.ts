import { Component, Input, Output, EventEmitter } from '@angular/core';
import { MatRadioModule } from '@angular/material/radio';
import { ReadingContent } from '../../core/models';
@Component({
  selector: 'app-listening-question-group', imports: [MatRadioModule],
  template: `@for(question of content.questions ?? []; track question.id) {<section class="listening-question"><h3 [id]="'prompt-' + question.id">{{question.prompt}}</h3><mat-radio-group [attr.aria-labelledby]="'prompt-' + question.id" [value]="answers[question.id] || ''" (change)="change(question.id,$event.value)" [disabled]="disabled">@for(option of question.options; track option.id) {<mat-radio-button [value]="option.id" class="choice">{{option.text}}</mat-radio-button>}</mat-radio-group></section>}`
})
export class ListeningQuestionGroupComponent {
  @Input({ required: true }) content!: ReadingContent; @Input({ required: true }) answers!: Record<string,string>; @Input() disabled = false;
  @Output() answersChange = new EventEmitter<Record<string,string>>();
  change(id: string,value: string) { this.answersChange.emit({ ...this.answers, [id]: value }); }
}
