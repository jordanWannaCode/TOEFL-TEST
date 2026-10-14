import {Component,input,output} from '@angular/core';
import {FormsModule} from '@angular/forms';
@Component({selector:'app-writing-editor',imports:[FormsModule],template:`<label for="writing-response">Your response</label><textarea id="writing-response" rows="14" maxlength="12000" [ngModel]="text()" (ngModelChange)="changed.emit($event)" [disabled]="disabled()" spellcheck="false" aria-describedby="word-count"></textarea><p id="word-count">{{wordCount()}} words · {{text().length}} / 12000 characters</p>`})
export class WritingEditorComponent {text=input.required<string>();disabled=input(false);changed=output<string>();wordCount(){return this.text().trim()?this.text().trim().split(/\s+/u).length:0;}}
