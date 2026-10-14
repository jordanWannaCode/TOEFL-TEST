import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Blueprint, Dashboard, ResponseBody, Results, SessionView, User, PlaybackState, ListeningExercise, TranscriptTurn } from './models';
import {WritingExercise,WritingResponse,WritingReport} from '../features/writing/models';
@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);
  readonly user = signal<User | null>(null);
  private csrf: { token: string; headerName: string } | null = null;
  async me(): Promise<User> { const user = await firstValueFrom(this.http.get<User>('/api/v1/me')); this.user.set(user); return user; }
  private async headers(extra: Record<string,string> = {}): Promise<Record<string,string>> {
    if (!this.csrf) this.csrf = await firstValueFrom(this.http.get<{ token: string; headerName: string }>('/api/v1/auth/csrf'));
    return { ...extra, [this.csrf.headerName]: this.csrf.token };
  }
  async login(email: string, password: string): Promise<void> {
    await firstValueFrom(this.http.post('/api/v1/auth/login', { email, password }, { headers: await this.headers() }));
    this.csrf = null; await this.me();
  }
  async logout(): Promise<void> {
    await firstValueFrom(this.http.post('/api/v1/auth/logout', {}, { headers: await this.headers() }));
    this.csrf = null; this.user.set(null);
    Object.keys(sessionStorage).filter(key => key.startsWith('toefl:')).forEach(key => sessionStorage.removeItem(key));
  }
  blueprints(section = "READING", mode = "PRACTICE") { return firstValueFrom(this.http.get<Blueprint[]>('/api/v1/blueprints', { params: { section, mode } })); }
  dashboard() { return firstValueFrom(this.http.get<Dashboard>('/api/v1/dashboard')); }
  session(id: string) { return firstValueFrom(this.http.get<SessionView>(`/api/v1/sessions/${id}`)); }
  results(id: string) { return firstValueFrom(this.http.get<Results>(`/api/v1/sessions/${id}/results`)); }
  async start(blueprintVersionId: string, key: string, mode = "PRACTICE") {
    return firstValueFrom(this.http.post<SessionView>('/api/v1/sessions', { blueprintVersionId, mode }, { headers: await this.headers({ 'Idempotency-Key': key }) }));
  }
  async save(id: string, itemId: string, body: ResponseBody, version: number, key: string) {
    return firstValueFrom(this.http.put<{ version: number; savedAt: string; serverTime: string }>(`/api/v1/sessions/${id}/items/${itemId}/response`, body, { headers: await this.headers({ 'If-Match': `"${version}"`, 'Idempotency-Key': key }) }));
  }
  async submit(id: string, version: number, key: string) {
    return firstValueFrom(this.http.post(`/api/v1/sessions/${id}/submit`, {}, { headers: await this.headers({ 'If-Match': `"${version}"`, 'Idempotency-Key': key }) }));
  }
  exercises(taskType: string, difficulty: string, skill: string) { return firstValueFrom(this.http.get<ListeningExercise[]>('/api/v1/listening/exercises', { params: { taskType, difficulty, skill } })); }
  playback(session: string, item: string) { return firstValueFrom(this.http.get<PlaybackState>(`/api/v1/sessions/${session}/items/${item}/playback`)); }
  async play(session: string, item: string, key: string) {
    return firstValueFrom(this.http.post<{ grantId: string; audioUrl: string }>(`/api/v1/sessions/${session}/items/${item}/playback`, {}, { headers: await this.headers({ 'Idempotency-Key': key }) }));
  }
  async audioProgress(session: string, item: string, grantId: string, positionMs: number, completed: boolean) {
    return firstValueFrom(this.http.put<PlaybackState>(`/api/v1/sessions/${session}/items/${item}/playback`, { grantId, positionMs, completed }, { headers: await this.headers() }));
  }
  transcript(session: string, item: string) { return firstValueFrom(this.http.get<TranscriptTurn[]>(`/api/v1/sessions/${session}/items/${item}/transcript`)); }
  async navigate(session: string, position: number) { return firstValueFrom(this.http.post<SessionView>(`/api/v1/sessions/${session}/navigation`, { position }, { headers: await this.headers() })); }
  writingExercises(mode:string) {return firstValueFrom(this.http.get<WritingExercise[]>('/api/v1/writing/exercises',{params:{mode}}));}
  writingReport(id:string) {return firstValueFrom(this.http.get<WritingReport>(`/api/v1/sessions/${id}/results`));}
  async saveWriting(id:string,item:string,body:WritingResponse,version:number,key:string) {return firstValueFrom(this.http.put<{version:number;savedAt:string;serverTime:string}>(`/api/v1/sessions/${id}/items/${item}/response`,body,{headers:await this.headers({'If-Match':`"${version}"`,'Idempotency-Key':key})}));}
  async retryWriting(id:string) {return firstValueFrom(this.http.post(`/api/v1/sessions/${id}/evaluations/retry`,{},{headers:await this.headers()}));}
  resetCsrf() { this.csrf = null; }
}
export function errorMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) return 'The request failed. Please try again.';
  if (error.status === 0) return 'Connection interrupted. Your answer is not yet confirmed. Retry when connected.';
  if (error.status === 401) return 'Your login has expired. Sign in again to resume your practice.';
  if (error.status === 403) return 'The security token has expired. Refresh the page and try again.';
  if (error.status === 409) return error.error?.detail ?? 'The session changed. Reload server answers before continuing.';
  if (error.status === 410) return 'The server deadline has passed. Open your results.';
  return error.error?.detail ?? 'The request failed. Please try again.';
}
