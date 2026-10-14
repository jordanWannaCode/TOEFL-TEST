export type TaskType = 'COMPLETE_THE_WORDS' | 'READ_IN_DAILY_LIFE' | 'READ_AN_ACADEMIC_PASSAGE' | 'LISTEN_AND_CHOOSE_A_RESPONSE' | 'LISTEN_TO_A_CONVERSATION' | 'LISTEN_TO_AN_ANNOUNCEMENT' | 'LISTEN_TO_AN_ACADEMIC_TALK';
export interface Segment { text?: string; gapId?: string; prefix?: string; suffix?: string; missingLength?: number; }
export interface ReadingContent { taskType: TaskType; title: string; instructions: string; segments?: Segment[]; passage?: string; prompt?: string; options?: { id: string; text: string }[]; stimulus?: AudioStimulus; questions?: ListeningQuestion[]; difficulty?: string; }
export interface ResponseBody { answers: Record<string, string>; }
export interface SessionItem { id: string; content: ReadingContent; response: ResponseBody; savedAt: string | null; available: boolean; }
export interface SessionView { id: string; state: 'ACTIVE' | 'SUBMITTED' | 'EXPIRED'; version: number; blueprintVersionId: string; startedAt: string; expiresAt: string | null; serverTime: string; items: SessionItem[]; section: 'READING' | 'LISTENING'; mode: 'PRACTICE' | 'SECTION_TEST'; playbackPolicy: PlaybackPolicy; navigationPolicy: 'FREE' | 'FORWARD_ONLY'; currentPosition: number; }
export interface Blueprint { id: string; name: string; version: number; durationSeconds: number; verificationStatus: string; section: string; mode: 'PRACTICE' | 'SECTION_TEST'; playbackPolicy?: PlaybackPolicy; navigationPolicy?: string; }
export interface Score { correct: number; total: number; }
export interface Results { sessionId: string; objectiveScore: Score; estimated: false; rubricVersion: string; items: { itemId: string; title: string; correct: number; total: number; response: ResponseBody; correctAnswers: Record<string,string>; explanation: string; content?: ReadingContent; questionFeedback?: QuestionFeedback[] }[]; section?: string; skillsToImprove?: string[]; }
export interface Dashboard { completedSessions: number; sectionMetrics: { section: string; attemptCount: number; objectiveAccuracy: number | null }[]; recentAttempts: { sessionId: string; section: string; mode: string; state: string; startedAt: string; completedAt: string | null; objectiveScore: Score | null }[]; }
export interface User { id: string; email: string; }
export interface PendingSave { itemId: string; body: ResponseBody; version: number; key: string; }

export interface PlaybackPolicy { maxPlays: number; allowPause: boolean; allowSeek: boolean; resumeAllowed: boolean; }
export interface AudioStimulus { id: string; durationMs: number; synthetic: boolean; speakers: { id: string; label: string; voice: string }[]; }
export interface ListeningQuestion { id: string; prompt: string; skill: string; options: { id: string; text: string }[]; }
export interface PlaybackState { plays: number; positionMs: number; completed: boolean; policy: PlaybackPolicy; durationMs: number; grantId?: string; audioUrl?: string; }
export interface ListeningExercise { id: string; blueprintVersionId: string; title: string; taskType: TaskType; difficulty: string; stimulus: AudioStimulus; skills: string[]; }
export interface QuestionFeedback { questionId: string; prompt: string; skill: string; correct: boolean; explanation: string; }
export interface TranscriptTurn { speakerId: string; text: string; }
