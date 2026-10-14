# Validation — socle et première tranche Reading
Date : 13 octobre 2026. Résultats de commandes exécutées dans cette instance cloud ; publication et restauration dans une nouvelle tâche non vérifiées.

Le parcours Reading est opérationnel : identité personnelle, trois types depuis PostgreSQL, réponses durables, reprise, clôture/correction objective, résultats et progression. Les clés sont séparées du contenu et ne sortent qu'après clôture. Les exercices sont originaux et relus en interne. Aucun test n'a été ajouté à l'historique personnel : la base principale contient zéro session après les vérifications.

| Contrôle | Résultat effectif |
|---|---|
| scripts/install.sh | Réussi : npm ci, uv sync --frozen, builds séquentiels Spring/Angular/FastAPI |
| scripts/check.sh / JUnit | 13 exécutés, 13 réussis, aucun échec ou skip |
| scripts/check.sh / Pytest | 2 exécutés, 2 réussis ; un avertissement de dépréciation AnyIO/Starlette |
| Build Angular production | Réussi, contrôles TypeScript/templates et budgets de bundle satisfaits |
| Build/package Java 21 / Spring Boot 3.5.16 | Réussi avec Maven 3.9.11 ; tests séparés exécutés |
| Playwright, après correction finale Nginx | 3 exécutés, 3 réussis, aucun skip ; Chromium système |
| PostgreSQL / Flyway | 3 migrations appliquées sur base neuve E2E et validées sur base persistante après recréation du backend |
| make up / smoke Reading | Quatre services sains ; requêtes HTTP directes et via proxy Angular → Spring validées |
| Recréation backend / résolution Nginx | Smoke réussi avant et après ; résolution DNS dynamique Docker |
| npm audit --omit=dev | 0 vulnérabilité signalée dans les dépendances de production au moment du contrôle ; pas un audit de sécurité exhaustif |
| git diff --check / fichiers locaux | Réussi ; .env, dépendances, builds et résultats de test ignorés ; aucun secret ajouté aux fichiers versionnables |

Tests Java : normalisation et unités objectives, réponses absentes, workflow complet, refus des résultats actifs, isolation de deux comptes pour lectures/écritures, rejouabilité et conflits de version, concurrence, expiration persistée avec rejet de réponse tardive, timing configuré inchangé à la reprise malgré une échéance envoyée par le client, CSRF/déconnexion, refus de contenu non approuvé, santé et migrations.
Tests navigateur : connexion et redirection privée, rendu des trois tâches, sauvegarde puis actualisation, score 4/4 et explications après soumission, historique/dashboard réels, sauvegarde réellement reçue par le serveur mais acquittement perdu puis retry après actualisation. Les bases de test sont distinctes du profil personnel ; le projet E2E est supprimé à la fin.
Les corrections issues de ces contrôles incluent l'initialisation zoneless Angular, le dashboard vide sans métrique, les permissions des images et la résolution dynamique du backend par Nginx.

## Limites restantes
MinIO n'a pas pu être compilé/démarré. Les pulls d'images testés sont refusés ; le build depuis la source officielle vérifiée échoue sur des archives Go redirigées vers storage.googleapis.com, interdit dans cette instance. Le contrôle complet `scripts/smoke.sh` incluant MinIO n'est donc pas validé ; `--reading` exclut explicitement ce service inutile au parcours actuel. Reprendre `make media` et le contrôle complet après activation des paramètres réseau.
Règles ETS 2026 non vérifiées (site officiel HTTP 403), banque limitée à trois exercices/quatre unités, sans calibration externe. Blueprint CUSTOM_PRACTICE, aucun barème officiel. Pas d'autres sections, IA de notation, Full Mock, facturation, inscription publique, récupération de compte ou politique de rétention commerciale. Les tests de sécurité vérifient le parcours local ; ils ne constituent pas un audit de mise en production.
La CI minimale est enregistrée mais n'a pas été exécutée sur GitHub. Les avertissements Mockito/JDK et AnyIO restent sans échec de test.

## Configuration réutilisable enregistrée
Le serveur a confirmé l'enregistrement de `install_script` (scripts/install.sh), `start_skill` (démarrage, contrôles, restrictions et limites), ainsi que de la variable non secrète READING_PRACTICE_DURATION_SECONDS suggérée à 0.
Le brouillon réseau conserve les presets existants et ajoute storage.googleapis.com, www.ets.org, cdn.playwright.dev et playwright.download.prss.microsoft.com. L'enregistrement n'applique pas la configuration et ne publie pas l'environnement. Revoir et enregistrer dans les paramètres, puis publier ; l'activation réseau et la restauration dans une nouvelle tâche ne sont pas prétendues vérifiées.
Aucune clé externe ou valeur de secret n'a été enregistrée dans le brouillon ou la documentation. Les secrets locaux aléatoires restent dans .env ignoré.
