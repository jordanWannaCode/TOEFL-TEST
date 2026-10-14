# TOEFL AI Simulator
Première tranche Reading opérationnelle : connexion personnelle → pratique des trois types → sauvegarde → soumission → correction objective et historique. Produit indépendant d'ETS, contenu original, aucune notation IA ou conversion officielle.

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
make media                 # MinIO, inutile pour Reading
./scripts/smoke.sh          # contrôle complet, incluant MinIO
```
MinIO expose localement 9000/9001 et conserve ses objets dans un volume. Sa compilation utilise une révision officielle et un SHA-256 fixé ; les images Docker Hub et Quay testées ont été refusées. Licence AGPL-3.0 amont ; version et obligations à revoir avant commercialisation.
Dans ce cloud, le build MinIO est bloqué par une redirection Go vers storage.googleapis.com. Le brouillon réseau ajoute ce domaine, www.ets.org et les deux domaines de téléchargement Playwright. Revoir/enregistrer les changements dans les paramètres et publier l'environnement pour les activer ; le socle Reading fonctionne indépendamment.
`scripts/compose.sh` génère hors Git un override de proxy cloud, avec certificat monté comme secret BuildKit et configuration Maven locale. TLS et contrôles d'intégrité restent actifs. Les caches sont conservés hors dépôt sous `/workspace/.cache` ; en cas de saturation, supprimer seulement les caches créés par cette tâche, pas les volumes de données.

## Documents
[PRD](docs/PRD.md) · [ADR initial](docs/ADR-0001.md) · [ADR Reading](docs/ADR-0002.md) · [Modèle](docs/domain-model.md) · [Structure](docs/repository.md) · [Taxonomie provisoire](docs/toefl-2026.md) · [Contrats](docs/api/reading-v1.md) · [Relecture](docs/content-review.md) · [Validation](docs/sprint-0-validation.md) · [Backlog](docs/backlog/roadmap.md).
Banque initiale : trois exercices, quatre unités, relus en interne. Pas de Listening/Writing/Speaking, de Full Mock, de facture ou de fournisseur IA. Les règles ETS 2026 restent à vérifier ; voir la taxonomie. Authentification publique, récupération de compte, rétention et calibrage pédagogique restent hors de cette livraison.
