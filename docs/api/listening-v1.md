# Contrat Listening v1
Les routes Reading existantes restent compatibles. Le moteur partagé ExamSessions ajoute section, mode, playbackPolicy, navigationPolicy et currentPosition à la vue session. Authentification cookie et CSRF identiques à Reading ; owner toujours dérivé du principal.
| Route | Contrat |
|---|---|
| GET /api/v1/listening/exercises | Filtres optionnels taskType, difficulty et skill. Exercices APPROVED avec blueprintVersionId, titre, type, difficulté, compétences et metadata du stimulus ; aucun transcript, clé ou objectKey. |
| GET /api/v1/blueprints?section=LISTENING&mode=PRACTICE ou SECTION_TEST | Dernières versions publiées par nom ; durée, mode, section et politiques publiques. Défaut READING/PRACTICE conservé. |
| POST /api/v1/sessions | {blueprintVersionId,mode}, Idempotency-Key. Le mode doit correspondre au blueprint. Snapshot des politiques et du contenu. |
| PUT /api/v1/sessions/{id}/items/{item}/response | {answers:{q1:"option-id",q2:"option-id"}}, If-Match et Idempotency-Key. Audio terminé requis. Une sauvegarde porte sur tout le groupe, sans calcul de score visible. |
| GET /api/v1/sessions/{id}/items/{item}/playback | {plays,positionMs,completed,policy,durationMs,grantId?,audioUrl?}. Reprise sans nouveau play lorsque permise. |
| POST /api/v1/sessions/{id}/items/{item}/playback | Idempotency-Key ; {grantId,audioUrl}. Répéter la clé ne débite pas un nouveau play. Une nouvelle lecture invalide l'ancien grant. |
| PUT /api/v1/sessions/{id}/items/{item}/playback | {grantId,positionMs,completed}. Checkpoint ; position bornée, rewind/skip contrôlés en mode strict et fin comparée à l'horloge serveur. |
| GET /api/v1/sessions/{id}/items/{item}/audio/{grant} | WAV privé, cookie obligatoire, propriétaire/groupe/grant/phase/échéance vérifiés. Support Range 206, no-store et checksum SHA-256 vérifié. Le grant n'est pas une URL S3 publique. |
| POST /api/v1/sessions/{id}/navigation | {position}. FREE ou déplacement suivant seulement ; audio courant terminé requis en FORWARD_ONLY. Les contenus hors groupe courant sont masqués durant l'examen strict. |
| GET /api/v1/sessions/{id}/items/{item}/transcript | Tableau de {speakerId,text}, uniquement après SUBMITTED/EXPIRED ; 409 TRANSCRIPT_LOCKED sinon. |
| POST /api/v1/sessions/{id}/submit | Clôture et évaluation atomiques, clés et explications révélées seulement après clôture. |
| GET /api/v1/sessions/{id}/results | Champs Reading conservés ; ajouts section, skillsToImprove, item.content public, questionFeedback par qid avec correct, skill et explanation. Score brut, estimated=false ; aucun LLM ou score ETS. |
| GET /api/v1/dashboard | Historique Reading/Listening et précision objective agrégée par section depuis les évaluations persistées. |
Un groupe correspond à un stimulus et une révision de contenu ; il peut contenir plusieurs questions. AnswerKey/feedback et transcript restent dans des tables séparées du contenu public. Le scheduler finalise les sessions expirées même sans navigateur ouvert.
Codes spécifiques : 409 REPLAY_LIMIT, NAVIGATION_RESTRICTED, AUDIO_REQUIRED, SKIP_RESTRICTED, REWIND_RESTRICTED, PLAYBACK_NOT_FINISHED ; 410 AUDIO_GRANT_EXPIRED ; 503 AUDIO_UNAVAILABLE/AUDIO_INTEGRITY. Les erreurs standard d'identité, CSRF, version et propriété restent identiques.
Les grants durent au maximum une heure, bornée par l'échéance active de session. La revue après clôture utilise de nouveaux grants et une politique de replay libre. Ils ne sont pas une protection contre un enregistrement externe. Voir les limites détaillées dans docs/ets-references/listening-2026.md.
