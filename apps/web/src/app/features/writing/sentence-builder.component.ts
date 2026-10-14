import {Component,input,output,model} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
@Component({selector:'app-sentence-builder',imports:[MatButtonModule],template:`<p>Select phrases in order. Use Tab and Enter to select, remove or move a phrase.</p><div aria-label="Available phrases">@for(element of elements();track element.id){<button mat-stroked-button [disabled]="selected().includes(element.id) || disabled()" (click)="add(element.id)">Add {{element.text}}</button>}</div><ol aria-label="Constructed sentence">@for(id of selected();track id;let index=$index){<li>{{label(id)}} <button mat-button [disabled]="disabled() || index===0" (click)="move(index,-1)" [attr.aria-label]="'Move '+label(id)+' earlier'">↑</button><button mat-button [disabled]="disabled() || index===selected().length-1" (click)="move(index,1)" [attr.aria-label]="'Move '+label(id)+' later'">↓</button><button mat-button [disabled]="disabled()" (click)="remove(id)" [attr.aria-label]="'Remove '+label(id)">Remove</button></li>}</ol><p data-testid="constructed-sentence">{{sentence()}}</p>`})
export class SentenceBuilderComponent {
 elements=input.required<{id:string;text:string}[]>();selected=model.required<string[]>();disabled=input(false);changed=output<string[]>();
 private commit(values:string[]){this.selected.set(values);this.changed.emit(values);}
 add(id:string){this.commit([...this.selected(),id]);}
 label(id:string){return this.elements().find(e=>e.id===id)?.text??'';} sentence(){return this.selected().map(id=>this.label(id)).join(' ');}
 remove(id:string){this.commit(this.selected().filter(value=>value!==id));}
 move(index:number,delta:number){const values=[...this.selected()];[values[index],values[index+delta]]=[values[index+delta],values[index]];this.commit(values);}
}
