# Contrats Sprint 1 — conception initiale ; voir reading-v1.md pour les contrats livrés
Base /api/v1. Identité personnelle par cookie de session HttpOnly, SameSite=Lax, Secure hors développement local ; mots de passe hachés avec algorithme adaptatif, aucun compte/mot de passe initial dans Git. Provisionnement local par commande administrative à développer. Pas d'inscription publique. Protection CSRF sur les écritures, session régénérée à la connexion, expiration configurable ; pas de token dans localStorage.
| Méthode et route | Entrée | Résultat |
|---|---|---|
| GET /auth/csrf | aucune | 200 {token,headerName} avec session anonyme ; token envoyé au header des écritures |
| POST /auth/login | {email,password} et CSRF | 200 {user:{id,email}} et cookie ; 401 identifiants invalides ; throttling prévu avant commercialisation |
| POST /auth/logout | cookie et CSRF | 204, session invalidée |
| GET /me | cookie | 200 {id,email,targetScore:null ou number} ; 401 sans session |
| PATCH /me/preferences | {targetScore:null ou number}, CSRF, If-Match | 200 profil, ETag ; cible pédagogique interne, échelle à définir avant implémentation |
| GET /dashboard | cookie | 200 {completedSessions:0,sectionMetrics:[],recentAttempts:[]} pour un compte sans activité ; valeurs calculées uniquement depuis ses données |
Erreurs application/problem+json : type, title, status, detail, instance, code. Aucun secret ou trace interne. 401 sans authentification, 403 pour CSRF invalide, 404 pour ressource privée inaccessible, 409 pour version périmée, 422 pour champ invalide. IDs UUID et dates ISO-8601 UTC ; owner toujours dérivé du principal.
Dashboard : completedSessions integer ; sectionMetrics tableau de {section,attemptCount,objectiveAccuracy:null ou number entre 0 et 1} ; recentAttempts tableau de {sessionId,section,state,completedAt}. Absence de tentative = tableau vide, aucune estimation inventée. Lecture seule ; pagination future de l'historique distincte.
Les contrats Reading, idempotence et session figurent dans sprint-2-proposal.md. Les contrôles de santé Sprint 0 restent publics en local uniquement.

État de livraison : login, logout, CSRF, me (id/email seulement) et dashboard Reading implémentés. Objectif de score et PATCH préférences restent non implémentés. Le contrat exécuté est reading-v1.md.
