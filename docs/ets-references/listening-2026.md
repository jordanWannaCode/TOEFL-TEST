# Listening 2026 — sources et limites
Sources officielles consultées le 14 octobre 2026, HTTP 200 :
- https://www.ets.org/toefl/test-takers/ibt/about/content.html
- https://www.ets.org/content/dam/ets-org/pdfs/toefl/toefl-ibt-test-specifications-2026.pdf
La page actuelle confirme les quatre types Listening, 47 items et 29 minutes comme valeurs de base approximatives ; elle précise que le nombre et le temps peuvent varier avec l'adaptation. Aucune durée universelle de 29 minutes n'est encodée.
Le document de spécification, pages imprimées 10–12, décrit les situations et compétences : échange bref, conversation, annonce, exposé académique ; stimuli avec représentation des locuteurs ; lecture unique ; questions généralement imprimées avec choix multiples, isolées ou regroupées ; au maximum un point par item Listening. Il indique un input bref allant jusqu'à six syllabes accentuées, des stimuli intermédiaires généralement de 35–100 mots et des monologues jusqu'à 250 mots.
Les scripts de ce dépôt sont originaux et ne reproduisent aucune question ETS. Les avatars textuels et noms de rôles représentent les locuteurs ; les quatre choix écrits et les groupes suivent le cadre décrit. La banque est volontairement réduite ; difficulté CEFR et authenticité des voix ne sont pas calibrées.

## Politiques implémentées
Practice : replay, pause et seek permis ; transcript et correction seulement après clôture. Ce sont des extensions pédagogiques de l'application.
SECTION_TEST : lecture unique (confirmée par la source), navigation avant seulement et reprise depuis une position serveur (politiques personnalisées, non confirmées comme règles ETS). Le compteur de temps continue même pendant une actualisation ou perte de réseau. La section de démonstration dure 300 secondes, paramètre configurable, sans prétention d'équivalence officielle.
L'adaptation est réservée par un champ de stratégie versionnée mais non implémentée. Routing, durées détaillées par parcours, contrôles exacts d'écran, navigation officielle, règles de reprise/incidents et accommodations nécessitent une validation complémentaire. Aucun contrôle inconnu n'est présenté comme une règle ETS vérifiée.

## Limites navigateur
Le serveur contrôle les autorisations, nombre de plays, navigation et délais ; les contrôles applicatifs interdisent pause/seek selon le blueprint. En mode strict sans pause, la reprise suit le temps serveur écoulé, sans rendre le temps perdu ni recommencer le clip. Un incident peut donc empêcher d'entendre une partie du stimulus ; une nouvelle pratique permet de réessayer.
Un navigateur doit recevoir les octets pour décoder l'audio. L'application ne peut pas empêcher l'enregistrement externe, l'inspection réseau ou un client modifié de conserver ces octets. Les grants autorisent les requêtes Range et retries de transport d'une lecture, pas une garantie DRM. Il n'y a pas de pause du délai serveur lors d'un chargement lent.
