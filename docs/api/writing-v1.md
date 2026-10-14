# Contrat Writing v1
Authentification étudiante, cookie sécurisé et CSRF existants. Aucun `ownerId` fourni par le client. Un autre propriétaire reçoit 404.

- `GET /api/v1/writing/exercises?mode=PRACTICE|SECTION_TEST` : contenu APPROVED, métadonnées et `blueprintVersionId`, jamais les variantes de correction.
- `POST /api/v1/sessions` : contrat existant, blueprint Writing, mode et `Idempotency-Key`.
- `GET /api/v1/sessions/{id}` : instantané, réponse sauvegardée, ETag/version, heure serveur, échéance immuable. Aucun feedback actif.
- `PUT /api/v1/sessions/{id}/items/{itemId}/response` : `If-Match` et `Idempotency-Key` ; phrase `{ "elementIds": ["a","c","b"] }` sans duplication/élément inconnu, texte `{ "text": "..." }` limité à 12 000 caractères. Sauvegarde vide/partielle permise pour les brouillons.
- `POST /api/v1/sessions/{id}/submit` : clôture idempotente. L'expiration sauvegarde l'état EXPIRED et crée les jobs à partir de la dernière réponse acquittée, même sans navigateur.
- `GET /api/v1/sessions/{id}/results` : 409 pendant ACTIVE. Writing renvoie `section`, `estimated`, `disclaimer`, `items`. Phrase : `objectiveScore`, `acceptableVariants`, explication. Texte : `jobId`, `status`, `errorCode`, `attempts`, puis `feedback`, fournisseur, modèle, date, prompt et usage.
- `POST /api/v1/sessions/{id}/evaluations/retry` : relance uniquement FAILED/UNAVAILABLE d'une session Writing fermée avec réponse non vide. Ne relance jamais COMPLETED.
- `GET /api/v1/dashboard` : ajoute `writingProgress`, tentatives par type, moyennes estimées basées sur les seuls résultats persistés, fréquence de catégories, tendance, recommandations provenant des évaluations et exercices approuvés non encore terminés.

Statuts : PENDING → PROCESSING → COMPLETED ; erreur transitoire → PENDING (trois essais) → FAILED ; fournisseur/secret/réponse indisponible → UNAVAILABLE. Codes d'erreur stables, aucun texte étudiant ni détail secret dans les erreurs.

## Spring → FastAPI
`POST /api/v1/ai/evaluations/writing`, `Authorization: Bearer <secret interne>`.

Entrée : `evaluationId`, `taskType`, `question` (instantané original), `response`, `rubric` (id/version/criteria/scale/source), `promptVersion`. Ni compte étudiant, email, cookie ni ownerId ne sont transmis.

Sortie : même `evaluationId`, `rubricVersion`, `promptVersion`, `provider`, `model`, `usage` (`inputTokens`, `outputTokens`, `estimatedCost`, nullable), `feedback`. Feedback : estimation 0–5, niveau indicatif, forces/faiblesses, explication, résultats par critère avec preuves et actions, erreurs avec span/correction/catégorie/sévérité, version améliorée, recommandations étayées et flags.

401 : authentification interne ; 422 : entrée invalide ; 413 : >128 Kio ; 503 : fournisseur indisponible ; 502 : fournisseur transitoire ou résultat invalide. OpenAPI exact généré et protégé : `GET /internal/openapi.json` avec le même secret. `/openapi.json` public désactivé. Aucun endpoint GET de job FastAPI : Spring possède le statut durable exposé dans les résultats, sans dépendance circulaire.
