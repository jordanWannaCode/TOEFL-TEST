# Validation Sprint 3 — 14 octobre 2026
Listening est intégré au moteur Reading existant. Quatre types, catalogue filtrable, groupes de questions liés aux stimuli, WAV privés MinIO, autorisations et état de lecture persistants, réponses/scores objectifs, transcripts après clôture, historique et statistiques par section. SECTION_TEST personnalisé : délai serveur, lecture unique, navigation avant seulement, reprise sans rendre du temps et expiration automatique.

| Vérification exécutée | Résultat |
|---|---|
| scripts/check.sh / JUnit | 21 tests réussis, aucun échec/skip ; régressions Reading conservées |
| scripts/check.sh / Pytest | 5 tests réussis ; avertissement AnyIO/Starlette sans échec |
| Playwright | 8 tests réussis, aucun skip ; Chromium système, vraies API et WAV MinIO |
| Angular production / TypeScript/templates | Build réussi, budgets satisfaits |
| Java 21 / Maven / Spring | Build et package réussis |
| scripts/install.sh | Exécuté avec succès : dépendances gelées et builds séquentiels des quatre images |
| MinIO / PostgreSQL / application | Cinq services démarrés ; smoke complet HTTP réussi, migration V4 appliquée sans modification V1–V3 |
| Manifest/WAV | Quatre sources identiques à leur copie embarquée ; SHA-256, durée WAV, flags synthetic/reviewed cohérents |
| Données personnelles | Zéro session de test dans la base principale ; tests isolés et volumes E2E nettoyés |
| git diff --check / scripts shell | Réussis ; aucun secret ajouté aux fichiers du projet |

Java couvre retrieval audio Range 206, refus d'accès anonyme au bucket et d'un autre propriétaire, sélection approuvée/filtres, refus des transcripts actifs, clés privées, groupes multi-questions et compétences incorrectes, persistance, scoring objectif, replay/idempotence, skip/rewind/navigation interdits, reprise avec échéance inchangée, expiration sans navigateur, import DRAFT → APPROVED et immutabilité, checksum/associations invalides.
Playwright couvre le parcours Listening complet (sélection, lecture réelle, réponse, soumission, score, explication, transcript et historique), les quatre types dans le catalogue, les filtres vides, le retry après échec d'un vrai téléchargement audio, les contrôles et la reprise d'une section personnalisée, le non-redémarrage d'un clip strict écoulé après refresh, plus le parcours Reading et sa perte d'acquittement.
Les erreurs réseau injectées dans les tests sont attendues ; aucun flux terminé ne repose sur des données frontend simulées. Les tests unitaires d'import utilisent des doubles seulement pour vérifier qu'une entrée invalide n'écrit rien.

## Limites
Pas de calibration psychométrique, d'adaptativité effective, d'accommodations ou de conformité exacte certifiée ETS. Les types et lecture unique sont confirmés par sources officielles ; navigation/reprise et durée de démonstration restent des règles personnalisées. Sept questions Listening seulement. Relecture interne des textes/clés ; validation pédagogique et écoute humaine externe des voix encore nécessaires. Audio eSpeak NG explicitement synthétique, sans prétention de voix naturelles.
Le navigateur reçoit les octets : enregistrement externe et client modifié restent possibles. En mode strict, un incident de chargement peut faire perdre une portion de l'écoute ; le temps serveur n'est pas rendu. Les WAV manuels sont supportés ; autres codecs à ajouter. L'import est local contrôlé, pas un écran administratif public. Aucun fournisseur TTS distant, Writing/Speaking, Full Mock ou score officiel.
La CI est mise à jour mais son exécution GitHub n'est pas revendiquée. Cette livraison est vérifiée dans l'instance actuelle ; publication/restauration dans une nouvelle tâche non vérifiées.

## Configuration et état Git
Le brouillon cloud a confirmé install_script, start_skill actualisé et LISTENING_SECTION_DURATION_SECONDS suggérée à 300, sans secret. Il doit être revu/enregistré puis publié dans les paramètres pour devenir actif. Le réseau actuel permet désormais le build MinIO et la consultation ETS ; aucun nouvel élargissement réseau n'a été demandé pour Sprint 3.
La publication des modifications Sprint 3 sur GitHub a été autorisée par l’utilisateur après validation.
