export type TaskType = 'COMPLETE_THE_WORDS' | 'READ_IN_DAILY_LIFE' | 'READ_AN_ACADEMIC_PASSAGE';
export interface Segment { text?: string; gapId?: string; prefix?: string; suffix?: string; missingLength?: number; }
export interface ReadingContent { taskType: TaskType; title: string; instructions: string; segments?: Segment[]; passage?: string; prompt?: string; options?: { id: string; text: string }[]; }
export interface ResponseBody { answers: Record<string, string>; }
export interface SessionItem { id: string; content: ReadingContent; response: ResponseBody; savedAt: string | null; }
export interface SessionView { id: string; state: 'ACTIVE' | 'SUBMITTED' | 'EXPIRED'; version: number; blueprintVersionId: string; startedAt: string; expiresAt: string | null; serverTime: string; items: SessionItem[]; }
export interface Blueprint { id: string; name: string; version: number; durationSeconds: number; verificationStatus: string; }
export interface Score { correct: number; total: number; }
export interface Results { sessionId: string; objectiveScore: Score; estimated: false; rubricVersion: string; items: { itemId: string; title: string; correct: number; total: number; response: ResponseBody; correctAnswers: Record<string,string>; explanation: string }[]; }
export interface Dashboard { completedSessions: number; sectionMetrics: { section: string; attemptCount: number; objectiveAccuracy: number | null }[]; recentAttempts: { sessionId: string; section: string; state: string; startedAt: string; completedAt: string | null; objectiveScore: Score | null }[]; }
export interface User { id: string; email: string; }
export interface PendingSave { itemId: string; body: ResponseBody; version: number; key: string; }
