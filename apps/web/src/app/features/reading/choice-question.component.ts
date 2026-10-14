import { Component, Input, Output, EventEmitter } from '@angular/core';
import { MatRadioModule } from '@angular/material/radio';
import { ReadingContent } from '../../core/models';
@Component({
  selector: 'app-choice-question', imports: [MatRadioModule],
  template: `<div class="passage">{{content.passage}}</div><h3 id="question-prompt">{{content.prompt}}</h3><mat-radio-group aria-labelledby="question-prompt" [value]="answers['choice'] || ''" (change)="answersChange.emit({ choice: $event.value })" [disabled]="disabled">@for(option of content.options ?? []; track option.id) {<mat-radio-button [value]="option.id" class="choice">{{option.text}}</mat-radio-button>}</mat-radio-group>`
})
export class ChoiceQuestionComponent {
  @Input({ required: true }) content!: ReadingContent; @Input({ required: true }) answers!: Record<string,string>; @Input() disabled = false;
  @Output() answersChange = new EventEmitter<Record<string,string>>();
}
