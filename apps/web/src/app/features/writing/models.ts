export interface WritingContent {
  taskType: 'BUILD_A_SENTENCE'|'WRITE_AN_EMAIL'|'WRITE_FOR_AN_ACADEMIC_DISCUSSION';
  title: string; instructions: string; prompt?: string; difficulty: string; rubricRef: string;
  elements?: {id:string;text:string}[]; contributions?: {name:string;text:string}[];
}
export interface WritingExercise { id:string; content:WritingContent; blueprintVersionId:string; }
export type WritingResponse = {text:string} | {elementIds:string[]};
export interface Feedback {
  estimated:true; internalScore:number; scoreMax:number; proficiencyLevel:string; strengths:string[]; weaknesses:string[]; explanation:string;
  criteria:{id:string;score:number;strengths:string[];weaknesses:string[];evidence:string[];improvements:string[]}[];
  findings:{originalSpan:string;explanation:string;correction:string;category:string;severity:string}[];
  improvedResponse:string; recommendations:{text:string;evidence:string}[]; flags:string[];
}
export interface WritingReport {sessionId:string; disclaimer:string; items:{itemId:string; content:WritingContent; response:WritingResponse; objectiveScore?:{correct:number;total:number}; acceptableVariants?:string[][]; explanation?:string; status?:string; errorCode?:string; feedback?:Feedback; provider?:string; model?:string; promptVersion?:string; evaluatedAt?:string;}[];}
export interface WritingProgress {attemptCount:number; byTaskType:{taskType:string;attemptCount:number;evaluatedCount:number;objectiveCorrect:number;objectiveTotal:number;averageEstimatedScore:number|null}[]; languageErrorFrequency:Record<string,number>; recommendations:string[]; proficiencyTrend:{taskType:string;internalScore:number;proficiencyLevel:string;evaluatedAt:string}[]; recommendedExercises:{id:string;title:string}[];}
