# API Reading v1 — implémentée
Accès via le proxy same-origin Angular `/api/v1`. Cookie HttpOnly ; CSRF via GET `/auth/csrf` et header renvoyé pour toutes les écritures. Connexion et déconnexion invalident le jeton ; le client le recharge. Les réponses métier ne contiennent jamais de hash de mot de passe.
## Identité
- GET `/auth/csrf` → `{token,headerName}` (anonyme permis).
- POST `/auth/login` avec `{email,password}` → `{user:{id,email}}`, cookie de session régénéré.
- POST `/auth/logout` → 204, session invalidée.
- GET `/me` → `{id,email}` ; 401 sans session.
Compte personnel configuré dans `.env`, créé uniquement avec le profil `local` et mot de passe BCrypt. Pas d'inscription publique ni de rotation automatique d'un mot de passe existant.
## Pratique
| Route | Contrat |
|---|---|
| GET `/blueprints` | Liste contenant la dernière version locale Reading : id, name, version, durationSeconds, verificationStatus. 0 = sans limite. |
| POST `/sessions` | `{blueprintVersionId,mode:"PRACTICE"}` et Idempotency-Key ; 201, Location et ETag. Répéter la même clé crée une seule session et renvoie son état courant ; changer le blueprint avec cette clé produit 409. |
| GET `/sessions/{id}` | `{id,state,version,blueprintVersionId,startedAt,expiresAt,serverTime,items}` et ETag. Item : `{id,content,response,savedAt}`. Content contient taskType, title, instructions et segments ou passage/prompt/options. Aucune clé ni explication de correction. |
| PUT `/sessions/{id}/items/{itemId}/response` | `{answers:{g1:"…",g2:"…"}}` pour les lettres manquantes, ou `{answers:{choice:"option-id"}}` pour un choix. If-Match = ETag entre guillemets, Idempotency-Key obligatoire. 200 `{version,savedAt,serverTime}` et nouvel ETag. Réponse partielle acceptée ; aucune notation pendant l'activité. |
| POST `/sessions/{id}/submit` | If-Match et Idempotency-Key. 200 `{id,state,version,submittedAt}` et ETag ; réponses verrouillées, évaluation persistée dans la même transaction. |
| GET `/sessions/{id}/results` | Accessible seulement après SUBMITTED ou EXPIRED. `{sessionId,objectiveScore:{correct,total},estimated:false,rubricVersion,items:[{itemId,title,correct,total,response,correctAnswers,explanation}]}`. |
| GET `/dashboard` | `{completedSessions,sectionMetrics,recentAttempts}` ; accuracy = unités correctes / unités totales des sessions closes, null/absence si pas de données. Historique : 20 sessions récentes, avec état et score uniquement si clôturées. |
Les clés d'idempotence contiennent 8–128 caractères alphanumériques, tirets ou underscores. UUID recommandé. Une répétition de sauvegarde/soumission avec la même clé et le même contenu renvoie le même résultat logique même si la version a depuis évolué ; ordre des propriétés JSON non contractuel. Même clé avec autre opération/contenu : 409. Les reçus vivent avec la session.
Le backend verrouille la session en PostgreSQL, vérifie propriétaire, échéance puis version. Le temps client n'intervient pas. Une échéance atteinte clôture et évalue atomiquement, même si la nouvelle écriture retourne 410. Les réponses absentes comptent zéro.
## Erreurs et reprise
401 identité absente, 403 CSRF invalide, 404 ressource non accessible (y compris autre propriétaire), 409 version/idempotence/état incompatible ou résultats d'une session active, 410 délai expiré, 422 format invalide. En-tête obligatoire absent : 400. Erreurs métier `application/problem+json`, code stable et detail non sensible.
Le client conserve dans sessionStorage, sous utilisateur + session, les brouillons et la requête en attente, sans secret ni clé de correction. Une sauvegarde n'est affichée confirmée qu'après acquittement serveur. Après perte d'acquittement, même clé, même réponse et même version sont renvoyées ; après conflit certain, le client recharge et propose une nouvelle sauvegarde. Le stockage local est vidé à la clôture et à la déconnexion. Sans sessionStorage, seules les sauvegardes confirmées sur le serveur sont garanties.
La soumission conserve également sa clé pour les retries. Recharger une session fermée conduit aux résultats ; recharger une session active ne réinitialise pas son échéance.
