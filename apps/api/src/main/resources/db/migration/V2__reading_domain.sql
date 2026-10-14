CREATE TABLE simulator.users (
 id uuid PRIMARY KEY, email varchar(254) NOT NULL UNIQUE, password_hash text NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE simulator.question_versions (
 id uuid PRIMARY KEY, question_id uuid NOT NULL, version integer NOT NULL CHECK(version>0),
 section text NOT NULL CHECK(section='READING'), task_type text NOT NULL CHECK(task_type IN ('COMPLETE_THE_WORDS','READ_IN_DAILY_LIFE','READ_AN_ACADEMIC_PASSAGE')),
 status text NOT NULL CHECK(status IN ('DRAFT','APPROVED')), content jsonb NOT NULL,
 UNIQUE(question_id,version)
);
CREATE TABLE simulator.answer_keys (
 question_version_id uuid PRIMARY KEY REFERENCES simulator.question_versions(id), answers jsonb NOT NULL, explanation text NOT NULL
);
CREATE TABLE simulator.blueprint_versions (
 id uuid PRIMARY KEY, name text NOT NULL, version integer NOT NULL CHECK(version>0),
 duration_seconds integer NOT NULL CHECK(duration_seconds BETWEEN 0 AND 86400),
 verification_status text NOT NULL CHECK(verification_status='CUSTOM_PRACTICE'),
 question_version_ids jsonb NOT NULL, UNIQUE(name,version)
);
CREATE TABLE simulator.exam_sessions (
 id uuid PRIMARY KEY, owner_id uuid NOT NULL REFERENCES simulator.users(id) ON DELETE CASCADE,
 blueprint_version_id uuid NOT NULL REFERENCES simulator.blueprint_versions(id),
 mode text NOT NULL CHECK(mode='PRACTICE'), state text NOT NULL CHECK(state IN ('ACTIVE','SUBMITTED','EXPIRED')),
 started_at timestamptz NOT NULL, expires_at timestamptz, submitted_at timestamptz,
 version integer NOT NULL DEFAULT 1 CHECK(version>0),
 creation_key varchar(128) NOT NULL, UNIQUE(owner_id,creation_key)
);
CREATE INDEX sessions_owner_time ON simulator.exam_sessions(owner_id,started_at DESC);
CREATE TABLE simulator.session_items (
 id uuid PRIMARY KEY, session_id uuid NOT NULL REFERENCES simulator.exam_sessions(id) ON DELETE CASCADE,
 question_version_id uuid NOT NULL REFERENCES simulator.question_versions(id),
 position integer NOT NULL, content_snapshot jsonb NOT NULL, UNIQUE(session_id,position)
);
CREATE TABLE simulator.responses (
 item_id uuid PRIMARY KEY REFERENCES simulator.session_items(id) ON DELETE CASCADE,
 response jsonb NOT NULL, revision integer NOT NULL, saved_at timestamptz NOT NULL
);
CREATE TABLE simulator.response_events (
 id uuid PRIMARY KEY, item_id uuid NOT NULL REFERENCES simulator.session_items(id) ON DELETE CASCADE,
 response jsonb NOT NULL, revision integer NOT NULL, saved_at timestamptz NOT NULL
);
CREATE TABLE simulator.evaluations (
 session_id uuid PRIMARY KEY REFERENCES simulator.exam_sessions(id) ON DELETE CASCADE,
 kind text NOT NULL CHECK(kind='OBJECTIVE'), result jsonb NOT NULL, evaluated_at timestamptz NOT NULL
);
CREATE TABLE simulator.request_receipts (
 session_id uuid NOT NULL REFERENCES simulator.exam_sessions(id) ON DELETE CASCADE,
 idempotency_key varchar(128) NOT NULL, operation text NOT NULL, payload jsonb NOT NULL,
 response jsonb NOT NULL, PRIMARY KEY(session_id,idempotency_key)
);
