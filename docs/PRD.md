# Exigences produit — TOEFL AI Simulator
## Objectif
Préparer un étudiant au TOEFL iBT 2026, puis permettre un SaaS multi-utilisateur. Produit indépendant d'ETS : les estimations IA ne sont jamais des scores officiels. Aucun contenu officiel protégé n'est reproduit.
## Utilisateurs et parcours
Étudiant : choisir une tâche, pratiquer, soumettre, consulter une correction après clôture. Administrateur de contenu : préparer puis publier des questions originales et un blueprint vérifié. À terme : tests de section chronométrés, examens blancs complets et suivi longitudinal.
## Périmètre et jalons
- Sprint 0 : documentation, contrats, structure, Angular, Spring, FastAPI, PostgreSQL/Flyway, MinIO, santé et tests de démarrage. Aucun parcours d'examen implémenté.
- Sprint 1 : identité locale sécurisée, premières migrations métier, layouts Angular et tableau de bord connecté aux données réelles. Une session appartient à un seul utilisateur. Pas d'inscription publique avant durcissement de l'authentification.
- Sprint 2 : premier parcours Reading complet, trois rendus, sessions résilientes, timing et correction objective.
- Jalons suivants : toutes les tâches Reading/Listening ; tests chronométrés ; Writing/Speaking et estimation IA ; analyses personnalisées ; examens blancs. Adaptativité après validation des règles et calibration.
## Exigences transversales
Isolation par propriétaire, clés de correction exclusivement serveur, stockage durable des réponses, reprise après actualisation, heure serveur et expiration persistée, contenu et blueprints versionnés. Audio dans MinIO, références privées et accès signé court. IA avec consentement, minimisation des données, délai et traitement des erreurs ; suppression et rétention à préciser avant commercialisation.
## Critères d'acceptation
Sprint 0 : build Angular, tests Java/Python, migration PostgreSQL sur base vide et après redémarrage, tous les services sains et contrôle HTTP représentatif. Aucun secret versionné.
Sprint 1 : connexion et déconnexion fonctionnelles, routes privées protégées, isolation de deux comptes et dashboard sans métriques simulées. Sprint 2 : sauvegarde répétée sans duplication, reprise fidèle, correction après clôture et expiration côté serveur.
## Hors périmètre actuel
Facturation, Redis/workers, génération automatique de questions, estimation IA réelle, examen adaptatif et équivalence officielle du score.
## Questions ouvertes
Vérification ETS des règles 2026 ; choix d'authentification commerciale ; politique de conservation et hébergement ; fournisseurs IA ; accessibilité et appareils/audio supportés. Les règles non vérifiées ne doivent pas être publiées comme blueprint officiel.

## État de cette livraison
Après le cadrage Sprint 0, la demande suivante a autorisé le premier parcours Reading complet. Auth personnelle, dashboard réel et tranche Reading des trois types sont implémentés. La couverture ne signifie pas que toutes les tâches des Sprints 1 et 2 ou toutes les règles officielles sont terminées. MinIO reste une limitation réseau séparée ; Reading n’en dépend pas.
