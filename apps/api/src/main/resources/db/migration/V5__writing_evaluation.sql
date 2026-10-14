ALTER TABLE simulator.question_versions DROP CONSTRAINT question_section;
ALTER TABLE simulator.question_versions DROP CONSTRAINT question_task;
ALTER TABLE simulator.question_versions ADD CONSTRAINT question_section CHECK(section IN ('READING','LISTENING','WRITING'));
ALTER TABLE simulator.question_versions ADD CONSTRAINT question_task CHECK(
 (section='READING' AND task_type IN ('COMPLETE_THE_WORDS','READ_IN_DAILY_LIFE','READ_AN_ACADEMIC_PASSAGE')) OR
 (section='LISTENING' AND task_type IN ('LISTEN_AND_CHOOSE_A_RESPONSE','LISTEN_TO_A_CONVERSATION','LISTEN_TO_AN_ANNOUNCEMENT','LISTEN_TO_AN_ACADEMIC_TALK')) OR
 (section='WRITING' AND task_type IN ('BUILD_A_SENTENCE','WRITE_AN_EMAIL','WRITE_FOR_AN_ACADEMIC_DISCUSSION')));
ALTER TABLE simulator.blueprint_versions DROP CONSTRAINT blueprint_versions_section_check;
ALTER TABLE simulator.blueprint_versions ADD CONSTRAINT blueprint_section CHECK(section IN ('READING','LISTENING','WRITING'));
ALTER TABLE simulator.exam_sessions DROP CONSTRAINT exam_sessions_section_check;
ALTER TABLE simulator.exam_sessions ADD CONSTRAINT session_section CHECK(section IN ('READING','LISTENING','WRITING'));
CREATE TABLE simulator.evaluation_rubrics (
 id text PRIMARY KEY, task_type text NOT NULL, definition jsonb NOT NULL, prompt_version text NOT NULL
);
CREATE TABLE simulator.evaluation_jobs (
 id uuid PRIMARY KEY, item_id uuid NOT NULL UNIQUE REFERENCES simulator.session_items(id) ON DELETE CASCADE,
 rubric_id text NOT NULL REFERENCES simulator.evaluation_rubrics(id),
 status text NOT NULL CHECK(status IN ('PENDING','PROCESSING','COMPLETED','FAILED','UNAVAILABLE')),
 attempts integer NOT NULL DEFAULT 0, next_attempt_at timestamptz NOT NULL DEFAULT now(),
 lease_until timestamptz, last_error text, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX evaluation_job_queue ON simulator.evaluation_jobs(status,next_attempt_at);
CREATE TABLE simulator.writing_evaluations (
 job_id uuid PRIMARY KEY REFERENCES simulator.evaluation_jobs(id) ON DELETE CASCADE,
 result jsonb NOT NULL, provider text NOT NULL, model text NOT NULL, prompt_version text NOT NULL,
 evaluated_at timestamptz NOT NULL, usage jsonb NOT NULL
);
