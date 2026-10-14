import { Component, Input, Output, EventEmitter } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ReadingContent } from '../../core/models';
@Component({
  selector: 'app-complete-words', imports: [FormsModule],
  template: `<div class="passage word-passage">@for(segment of content.segments ?? []; track $index) {@if(segment.gapId) {<span class="word-gap">{{segment.prefix}}<input class="gap-input" [attr.aria-label]="'Missing letters ' + segment.gapId" [attr.maxlength]="segment.missingLength" [attr.size]="segment.missingLength" [ngModel]="answers[segment.gapId] || ''" (ngModelChange)="change(segment.gapId, $event)" [disabled]="disabled" autocomplete="off" spellcheck="false">{{segment.suffix}}</span>}@else {<span>{{segment.text}}</span>}}</div>`
})
export class CompleteWordsComponent {
  @Input({ required: true }) content!: ReadingContent; @Input({ required: true }) answers!: Record<string,string>; @Input() disabled = false;
  @Output() answersChange = new EventEmitter<Record<string,string>>();
  change(id: string, value: string) { this.answersChange.emit({ ...this.answers, [id]: value }); }
}
