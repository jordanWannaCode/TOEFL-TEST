# Sprint 4 — livraison et validation
Vérifications effectuées le 14 octobre 2026 dans l'environnement cloud actuel.

## Implémenté
- Trois tâches Writing originales APPROVED, difficultés indicatives A2/B1/B2 ; variantes de phrases relues et privées.
- Assemblage accessible à la souris et au clavier, ordre modifiable, deux variantes déterministes acceptées, explications après clôture.
- Éditeur réutilisable, word count, sauvegarde automatique backend, révisions et reçus idempotents ; récupération de brouillon après actualisation et perte d'acquittement.
- Practice sans limite et tests personnalisés par exercice, échéance serveur immuable/configurable, finalisation des brouillons sauvegardés à expiration sans navigateur.
- File PostgreSQL, timeout, bail récupérable, retry automatique borné et relance manuelle. Un job/résultat par item.
- FastAPI protégé, interface fournisseur, adaptateurs OpenAI/Anthropic, modèle configurable, prompt/rubrics versionnés, JSON structuré, validation des preuves et scores. Usage et provenance persistés ; coût inconnu explicitement null.
- Rapports persistants : original, critères/preuves/actions, corrections annotées, version améliorée, recommandations, incertitudes, date/modèle. Dashboard par type, score objectif des phrases, moyennes IA estimées, catégories d'erreurs, tendance et prochains exercices approuvés.
- Flyway V5/V6, contrats/API et commandes documentés. V1–V4 conservées.

## Résultats vérifiés
| Contrôle | Résultat |
|---|---|
| `./scripts/install.sh` | Installation verrouillée et images construites |
| Build Java / Angular | Réussis |
| `make up` puis `make smoke` | Cinq services sains, proxy frontend et DB inclus |
| `./scripts/check.sh` — JUnit | 27 réussis, 0 échec, 0 ignoré |
| `./scripts/check.sh` — Pytest | 18 réussis, 0 échec, 0 ignoré |
| `./scripts/check.sh` — Playwright | 13 réussis, 0 échec, 0 ignoré |
| FastAPI réel, fournisseur disabled | 503 EVALUATION_UNAVAILABLE, aucune fausse évaluation |
| OpenAPI interne réel anonyme | 401 |
| `git diff --check`, syntaxe scripts | Réussis |
| Base personnelle | Reading 3, Listening 4, Writing 3 APPROVED ; 0 session de test |

Java couvre phrase objective/variante/éléments invalides, brouillons, création idempotente de jobs, reprise/retry après échec fournisseur, persistance et historique, propriétaire étranger, limite d'entrée, expiration et récupération de bail, plus régressions Reading/Listening. Les workers des contextes JUnit sont déclenchés explicitement pour éviter la concurrence involontaire entre contextes ; le traitement automatique est exercé réellement en E2E.
Python couvre auth, schéma privé, statuts indisponibles, fournisseur test interdit en production, spans inventés rejetés, critères/scores invalides, taille HTTP, timeout, sélection Anthropic et deux références originales de grammaire. Le benchmark est un contrôle de contrat et d'ancrage, pas une calibration psychométrique.
Playwright couvre Email complet jusqu'au feedback/historique, assemblage au clavier, Discussion, échéance conservée et perte d'acquittement Writing, plus Reading/Listening. Les mocks IA sont exclusivement dans les tests et le projet E2E identifié TEST_ONLY ; Spring/PostgreSQL/FastAPI et le navigateur communiquent réellement.

## Limites et suite
Aucun fournisseur distant appelé : clé, modèle et budget non configurés. OpenAI/Anthropic intégrés mais validation réelle et qualité/calibration à effectuer séparément. Aucune équivalence ETS/CEFR revendiquée. Durées exactes et autres contraintes non confirmées restent personnalisées ; le catalogue ne simule pas une section officielle complète. Trois exercices seulement. Les spans vérifiés ne garantissent pas que la correction soit linguistiquement juste ni que la reformulation préserve parfaitement le sens. Relecture humaine et corpus de références étendu restent nécessaires.
Les recommandations actuelles agrègent les résultats persistés et catégories ; personnalisation avancée et parcours adaptatif à venir. Avant SaaS : politique de rétention, API de suppression/export, gestion de contenu administrative, durcissement déploiement et TLS interne hors réseau Compose. Aucun Writing fictif dans la base personnelle, ni Speaking/Full Mock ajouté.

## Configuration et Git
`.env`, secrets et caches sont ignorés. Le brouillon cloud confirme `start_skill` mis à jour et les suggestions `WRITING_SECTION_DURATION_SECONDS=300`, `WRITING_AI_PROVIDER=disabled`. Revoir/enregistrer puis publier dans les paramètres pour activer ; ni publication/restauration en nouvelle tâche ni CI GitHub exécutée ne sont revendiquées. Install_script existant conservé.
Publication des modifications Sprint 4 sur GitHub autorisée par l’utilisateur après validation.
