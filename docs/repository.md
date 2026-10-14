# Structure du dépôt
- apps/web/ : Angular standalone, Material, Tailwind ; tests navigateur Playwright.
- apps/api/ : Spring Boot, migrations Flyway et tests JUnit ; futurs modules sous com.toeflsimulator.
- apps/ai-service/ : FastAPI et Pytest ; futurs adaptateurs providers derrière interfaces.
- docs/ : PRD, ADR, modèle, taxonomie et contrats.
- scripts/ : préparation locale, build/tests et smoke tests.
- compose.yaml : services et stockage local persistant.
Les fichiers .env, .venv, node_modules, target, dist et rapports sont locaux et ignorés. Pas de contenu TOEFL initial ni de données d'utilisateurs.

- infra/ : Dockerfile MinIO et override E2E isolé.
- .github/workflows/ : CI du socle Reading.
- Makefile : commandes usuelles.
- apps/web/src/app/features/ : auth, dashboard et reading, sans composants artificiels pour les autres sections.
- apps/api/src/main/java/com/toeflsimulator/ : identity, content, blueprint, examination, scoring et common ; interfaces publiques pour les frontières métier.

Sprint 3 : features/listening et shared/audio-player ; ExamSessions/ExamController remplacent les noms Reading pour le moteur partagé. media/ObjectStorage et adaptateur MinIO, contenu/import/blueprints Listening, interfaces speech dans ai-service et manifest original sous content/listening. Les transcripts et WAV embarqués restent hors des chemins de ressources web publiques.
