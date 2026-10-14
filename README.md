# TOEFL AI Simulator
Parcours Reading et Listening opérationnels : connexion personnelle → pratique des trois types → sauvegarde → soumission → correction objective et historique. Listening ajoute quatre types, filtres, groupes audio et section chronométrée personnalisée. Produit indépendant d'ETS, contenu original, aucune notation IA ou conversion officielle.

## Démarrer
Prérequis : Docker Engine/Compose v2.24.4+, Python 3.12, uv 0.9.5 ou compatible, Node 22.12+ (22 LTS recommandé ; 24 supporté), npm. Java 21/Maven 3.9.11 sont fournis par les images.
```bash
./scripts/install.sh
make up
make smoke
```
Ports locaux : Angular 4200, Spring 8080, FastAPI 8000. PostgreSQL reste sur le réseau privé Docker. L'interface est accessible sur le port 4200 de votre machine ; aucun aperçu web cloud n'est fourni ici.
`init-local.sh` génère `.env` avec secrets aléatoires sans les afficher. Il conserve les valeurs existantes et ajoute seulement les variables manquantes. Consulter ce fichier localement pour les identifiants LOCAL_USER_EMAIL / LOCAL_USER_PASSWORD ; ne pas copier ses valeurs dans Git ou un compte rendu. Le compte est créé au premier démarrage du profil local ; changer le fichier ne réinitialise pas un compte existant.

## Utiliser Reading
Se connecter, cliquer « Start Reading practice », remplir les lettres manquantes (uniquement la partie manquante), puis les deux QCM. « Save answer » confirme la persistance ; changer de question conserve les brouillons. « Submit practice » verrouille les réponses et révèle les explications. Le dashboard affiche uniquement vos tentatives réelles.
Une actualisation recharge les réponses serveur. Une sauvegarde sans acquittement reste en attente et peut être réessayée avec la même clé. Une réponse non sauvegardée n'est pas garantie hors de l'onglet courant. Les résultats d'une session close restent persistés.
READING_PRACTICE_DURATION_SECONDS=0 donne une pratique sans limite ; définir une durée personnalisée dans `.env` puis recréer le backend publie une nouvelle version du blueprint. Les sessions déjà démarrées gardent leur échéance. Aucun temps officiel TOEFL non vérifié n'est utilisé.

## Vérifier
```bash
# Installer le navigateur Playwright si aucun Chromium local n'est fourni :
(cd apps/web && npm ci && npx playwright install chromium)
# Linux : npx playwright install-deps chromium si bibliothèques système absentes.
./scripts/check.sh
./scripts/smoke.sh --reading
```
Le cloud utilise automatiquement le Chromium système s'il est disponible ; ailleurs, `PLAYWRIGHT_EXECUTABLE_PATH` permet de le choisir explicitement. Le navigateur Playwright téléchargé est le choix CI par défaut.
JUnit migre la base dédiée `toefl_test` et nettoie ses comptes de test. Pytest vérifie FastAPI. Playwright démarre un projet Compose isolé sur le port 4210, vérifie le parcours réel et un acquittement perdu, puis supprime uniquement ses propres volumes. Les données personnelles restent dans le projet principal. La CI existe dans `.github/workflows/ci.yaml` ; son exécution distante n'est pas prétendue vérifiée.

## Développer
```bash
# Frontend en développement, proxy /api et /system/health vers Spring
./scripts/compose.sh stop frontend
(cd apps/web && npm start)
# Python, port alternatif
(cd apps/ai-service && uv sync --frozen && uv run uvicorn app.main:app --reload --port 8001)
# Logs et redémarrage
make logs
./scripts/compose.sh restart backend
./scripts/smoke.sh --reading
make down  # volumes conservés
```
Variables backend : DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD ; identité de bootstrap local : LOCAL_USER_EMAIL / LOCAL_USER_PASSWORD ; timing : READING_PRACTICE_DURATION_SECONDS ; cookie Secure actif par défaut, SESSION_COOKIE_SECURE=false uniquement en Compose local.
Sur l'hôte, utiliser Java 21 et Maven 3.9.11 avec ces variables. Le runner `backend-check` fournit l'environnement et le réseau privé. Ne jamais diriger les tests vers une base de production. Flyway : ne pas modifier les migrations déjà appliquées ; ajouter une migration et une nouvelle révision de contenu.

## Médias futurs et cloud
```bash
make media                 # MinIO, désormais requis pour Listening
./scripts/smoke.sh          # contrôle complet, incluant MinIO
```
MinIO expose localement 9000/9001 et conserve ses objets dans un volume. Sa compilation utilise une révision officielle et un SHA-256 fixé ; les images Docker Hub et Quay testées ont été refusées. Licence AGPL-3.0 amont ; version et obligations à revoir avant commercialisation.
MinIO a été compilé et démarré le 14 octobre 2026 : le réseau autorise maintenant les dépendances Go nécessaires.
`scripts/compose.sh` génère hors Git un override de proxy cloud, avec certificat monté comme secret BuildKit et configuration Maven locale. TLS et contrôles d'intégrité restent actifs. Les caches sont conservés hors dépôt sous `/workspace/.cache` ; en cas de saturation, supprimer seulement les caches créés par cette tâche, pas les volumes de données.

## Documents
[PRD](docs/PRD.md) · [ADR initial](docs/ADR-0001.md) · [ADR Reading](docs/ADR-0002.md) · [Modèle](docs/domain-model.md) · [Structure](docs/repository.md) · [Taxonomie provisoire](docs/toefl-2026.md) · [Contrats](docs/api/reading-v1.md) · [Relecture](docs/content-review.md) · [Validation](docs/sprint-0-validation.md) · [Backlog](docs/backlog/roadmap.md).
Banque initiale : trois exercices Reading et quatre groupes Listening, relus en interne. Pas de Writing/Speaking, Full Mock, facturation ou notation IA. Les types Listening et la lecture unique sont confirmés ; d’autres règles restent à vérifier, voir les références ETS. Authentification publique, récupération de compte, rétention et calibrage pédagogique restent hors de cette livraison.

## Listening — Sprint 3
Depuis le dashboard, ouvrir Listening, filtrer par type/difficulté/compétence, puis choisir un exercice. « Play audio » charge un WAV privé depuis MinIO ; les questions apparaissent après la fin confirmée. Sauvegarder, soumettre et consulter score brut, explications, compétences à revoir et transcript. Un nouveau démarrage crée une nouvelle tentative.
La section personnalisée regroupe les quatre stimuli : lecture unique, pas de pause/seek, navigation avant seulement et délai serveur. LISTENING_SECTION_DURATION_SECONDS=300 est une durée de démonstration configurable, jamais une durée officielle universelle. Recréer le backend publie une nouvelle version si la durée change ; les sessions actives gardent leur échéance.
Variables stockage transmises par Compose : OBJECT_STORAGE_ENDPOINT, OBJECT_STORAGE_ACCESS_KEY, OBJECT_STORAGE_SECRET_KEY, OBJECT_STORAGE_BUCKET. Les identifiants réutilisent MINIO_ROOT_USER / MINIO_ROOT_PASSWORD générés dans .env, sans clé externe. Le bucket est privé. LISTENING_IMPORT_DIRECTORY permet un import local contrôlé avec un volume read-only.
[API Listening](docs/api/listening-v1.md) · [Pipeline et relecture](docs/listening-content.md) · [Sources ETS et limites](docs/ets-references/listening-2026.md) · [ADR](docs/ADR-0003.md) · [Validation Sprint 3](docs/sprint-3-validation.md).
`make up` démarre désormais les cinq services ; `make smoke` inclut MinIO. `scripts/check.sh` vérifie Reading et Listening, avec stockage MinIO réel et bases isolées. La génération optionnelle nécessite eSpeak NG ; les WAV embarqués suffisent au démarrage.

## Sprint 4 — Writing
Trois tâches originales, brouillons persistés, tests personnalisés chronométrés et évaluation IA différée avec état indisponible explicite. Voir [développement Writing](docs/writing-development.md), [API](docs/api/writing-v1.md), [ADR 0004](docs/ADR-0004.md) et [sources ETS](docs/ets-references/writing-2026.md).
