ALTER TABLE simulator.question_versions DROP CONSTRAINT question_versions_section_check;
ALTER TABLE simulator.question_versions DROP CONSTRAINT question_versions_task_type_check;
ALTER TABLE simulator.question_versions ADD CONSTRAINT question_section CHECK(section IN ('READING','LISTENING'));
ALTER TABLE simulator.question_versions ADD CONSTRAINT question_task CHECK(
 (section='READING' AND task_type IN ('COMPLETE_THE_WORDS','READ_IN_DAILY_LIFE','READ_AN_ACADEMIC_PASSAGE')) OR
 (section='LISTENING' AND task_type IN ('LISTEN_AND_CHOOSE_A_RESPONSE','LISTEN_TO_A_CONVERSATION','LISTEN_TO_AN_ANNOUNCEMENT','LISTEN_TO_AN_ACADEMIC_TALK')));
ALTER TABLE simulator.question_versions ADD COLUMN content_fingerprint text;
ALTER TABLE simulator.answer_keys ADD COLUMN feedback jsonb NOT NULL DEFAULT '{}';
CREATE TABLE simulator.audio_assets (
 id uuid PRIMARY KEY, object_key text NOT NULL UNIQUE, sha256 char(64) NOT NULL,
 media_type text NOT NULL CHECK(media_type='audio/wav'), size_bytes bigint NOT NULL CHECK(size_bytes BETWEEN 44 AND 10485760),
 duration_ms integer NOT NULL CHECK(duration_ms>0), synthetic boolean NOT NULL, speakers jsonb NOT NULL
);
CREATE TABLE simulator.listening_stimuli (
 question_version_id uuid PRIMARY KEY REFERENCES simulator.question_versions(id),
 audio_asset_id uuid NOT NULL REFERENCES simulator.audio_assets(id), transcript jsonb NOT NULL
);
ALTER TABLE simulator.blueprint_versions ADD COLUMN section text NOT NULL DEFAULT 'READING' CHECK(section IN ('READING','LISTENING'));
ALTER TABLE simulator.blueprint_versions ADD COLUMN mode text NOT NULL DEFAULT 'PRACTICE' CHECK(mode IN ('PRACTICE','SECTION_TEST'));
ALTER TABLE simulator.blueprint_versions ADD COLUMN playback_policy jsonb NOT NULL DEFAULT '{"maxPlays":0,"allowPause":true,"allowSeek":true,"resumeAllowed":true}';
ALTER TABLE simulator.blueprint_versions ADD COLUMN navigation_policy text NOT NULL DEFAULT 'FREE' CHECK(navigation_policy IN ('FREE','FORWARD_ONLY'));
ALTER TABLE simulator.blueprint_versions ADD COLUMN adaptive_strategy text NOT NULL DEFAULT 'NONE';
ALTER TABLE simulator.exam_sessions DROP CONSTRAINT exam_sessions_mode_check;
ALTER TABLE simulator.exam_sessions ADD CONSTRAINT session_mode CHECK(mode IN ('PRACTICE','SECTION_TEST'));
ALTER TABLE simulator.exam_sessions ADD COLUMN section text NOT NULL DEFAULT 'READING' CHECK(section IN ('READING','LISTENING'));
ALTER TABLE simulator.exam_sessions ADD COLUMN playback_policy jsonb NOT NULL DEFAULT '{"maxPlays":0,"allowPause":true,"allowSeek":true,"resumeAllowed":true}';
ALTER TABLE simulator.exam_sessions ADD COLUMN navigation_policy text NOT NULL DEFAULT 'FREE' CHECK(navigation_policy IN ('FREE','FORWARD_ONLY'));
ALTER TABLE simulator.exam_sessions ADD COLUMN current_position integer NOT NULL DEFAULT 0;
CREATE TABLE simulator.playback_state (
 item_id uuid PRIMARY KEY REFERENCES simulator.session_items(id) ON DELETE CASCADE,
 plays integer NOT NULL DEFAULT 0, position_ms integer NOT NULL DEFAULT 0, completed boolean NOT NULL DEFAULT false
);
CREATE TABLE simulator.playback_grants (
 id uuid PRIMARY KEY, item_id uuid NOT NULL REFERENCES simulator.session_items(id) ON DELETE CASCADE,
 request_key varchar(128) NOT NULL, created_at timestamptz NOT NULL, expires_at timestamptz NOT NULL,
 start_position_ms integer NOT NULL DEFAULT 0, review boolean NOT NULL DEFAULT false,
 UNIQUE(item_id,request_key)
);
CREATE INDEX session_expiration ON simulator.exam_sessions(expires_at) WHERE state='ACTIVE';
