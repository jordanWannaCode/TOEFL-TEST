# Modèle de domaine initial
Ce modèle guide Sprint 1 ; Sprint 0 ne crée que la migration technique de base.
| Agrégat / entité | Champs principaux | Invariants |
|---|---|---|
| User | id, externalSubject, createdAt | identité vérifiée côté serveur |
| Question / QuestionRevision | id, revision, section, taskType, content, mediaRefs, status | publication immuable, contenu original |
| AnswerKey | questionRevisionId, acceptedAnswers, rubricVersion | stockage séparé ; jamais envoyé pendant une session active |
| ExamBlueprint / BlueprintVersion | id, version, rules, provenance, verificationStatus | publication réservée aux règles validées |
| ExamSection | blueprintVersionId, section, position, timingRules | règles versionnées ; aucune durée ETS non vérifiée |
| RubricVersion | id, version, criteria, scoringMethod | publié immuable ; provenance et calibration explicites |
| ExamSession | id, ownerId, blueprintVersionId, mode, state, startedAt, expiresAt, version | propriétaire obligatoire, transition atomique |
| SessionItem | id, sessionId, questionRevisionId, position, contentSnapshot | ordre et contenu figés |
| SectionAttempt | id, sessionId, sectionId, state, startedAt, expiresAt | deadline persistée et clôture atomique |
| Attempt | id, sessionItemId, response, revision, savedAt | dernière révision durable ; historique selon rétention |
| Evaluation | id, attemptId, kind, status, score, rubricVersion, modelVersion | résultat objectif ou estimation IA explicitement distincts |
| MediaAsset | id, ownerId, objectKey, mediaType, size, checksum | objets privés, accès autorisé et temporaire |
| PerformanceSummary | ownerId, section, taskType, period, metrics | projection recalculable, pas de score ETS officiel |
Relations : User 1—N ExamSession ; BlueprintVersion 1—N ExamSession ; ExamSession 1—N SessionItem ; QuestionRevision 1—N SessionItem et 1—1 AnswerKey ; SessionItem 1—N Attempt ; Attempt 1—N Evaluation.
États session : CREATED → ACTIVE → SUBMITTED ou EXPIRED ; CANCELLED est terminal. Clôture et expiration sont atomiques ; l'heure serveur tranche les écritures tardives. La pratique non chronométrée accepte expiresAt=null. Après fermeture aucune réponse ne change ; une nouvelle pratique crée une nouvelle session.
Évaluation : PENDING → RUNNING → SUCCEEDED ou FAILED ; retries identifiés et traçables. Aucun résultat absent ne devient zéro implicitement.
Isolation : owner dérivé du principal, requêtes filtrées par owner, références vérifiées transitivement, URLs média signées après autorisation. Les banques publiques sont partagées en lecture ; banques privées avec owner obligatoire. Les clés restent dans le module scoring.
Adaptativité future : stratégie de sélection versionnée dans le blueprint, décisions enregistrées dans la session ; pas d'algorithme inventé en Sprint 0.

Les futurs AIJob (evaluationId, retryCount, status, provider, modelVersion, latencyMs, estimatedCost) seront séparés des réponses persistées ; aucune table de job, abonnement ou paiement créée en Sprint 0. Le cycle d’examen et le cycle d’évaluation restent deux machines d’état indépendantes : une panne IA ne rouvre pas un examen.
