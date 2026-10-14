# Contrats Sprint 2 — proposition initiale
Le contrat livré et testé est reading-v1.md ; cette proposition initiale est conservée pour le cadrage des extensions.
Base /api/v1, JSON, authentification obligatoire. Temps ISO-8601 UTC. IDs UUID. Erreurs application/problem+json (type, title, status, detail, instance, code), sans fuite de contenu ni existence d'une ressource d'un autre utilisateur. 401 sans identité ; 404 pour une ressource inaccessible ; 409 pour version/idempotence incompatible ; 422 pour réponse invalide ; 410 pour session expirée.
| Méthode et route | Entrée | Résultat |
|---|---|---|
| GET /me | principal authentifié | {id} |
| GET /blueprints | aucune | versions publiées autorisées et règles de pratique |
| POST /sessions | {blueprintVersionId, mode: PRACTICE}, Idempotency-Key | 201, Location, {id,state,version,serverTime,expiresAt} |
| GET /sessions/{id} | propriétaire | session, règles, items au contenu public, réponses sauvegardées, serverTime ; aucune clé/évaluation active |
| PUT /sessions/{id}/items/{itemId}/response | {response: {selectedOptionIds: [...]}} ; If-Match version session ; Idempotency-Key | 200 {version,savedAt,serverTime} ; même retry renvoie même résultat même après évolution de version |
| POST /sessions/{id}/submit | If-Match et Idempotency-Key | 200 {id,state,submittedAt,version} ; correction uniquement après transition terminale |
| GET /sessions/{id}/results | propriétaire, état terminal | {objectiveScore:{correct,total},items:[{itemId,isCorrect,explanation}],estimated:false} ; 409 si active |
Sprint 2 porte sur une tâche Reading à choix objectif. Les payloads des autres tâches seront versionnés ultérieurement. Les réponses de GET incluent un ETag opaque correspondant à la version session ; toute écriture modifie la version dans une transaction. Un retry idempotent est résolu avant la comparaison de version ; même clé avec payload différent retourne 409. Clé scoped au propriétaire, méthode et ressource. Retention minimale jusqu'à la fin de la session plus fenêtre de retry documentée.
Le serveur contrôle toutes les échéances avant écriture. Reprise : GET puis reconciliation des réponses, nouvelle version pour toute modification. Une perte réseau ne garantit jamais une sauvegarde ; l'interface distingue confirmé et en attente.
Publication admin de questions et blueprints, export/suppression, upload audio et contrat d'évaluation IA restent hors Sprint 2. Le service Python n'est pas directement accessible aux étudiants.
