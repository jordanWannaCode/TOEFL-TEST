# Conventions de développement
- Documentation et comptes rendus en français ; code, identifiants et API en anglais.
- Utiliser le checkout existant : les tâches cloud sont déjà isolées. Ne pas créer de worktree sans demande explicite.
- Livrer par sprint ; Sprint 0 initialise uniquement infrastructure, contrats et contrôles de santé.
- Monolithe Spring modulaire : modules identity, content, blueprint, examination, scoring, evaluation, analytics, media. Dépendances entre modules via interfaces publiques ; aucune dépendance vers les internes d'un autre module.
- Règles métier, autorisation et temps de référence côté serveur. Chaque requête métier est limitée au propriétaire authentifié ; ne jamais faire confiance à un ownerId envoyé par le client.
- Séparer contenu publié, clés de correction, réponses et évaluations. Aucune clé dans un DTO de session active, les journaux ou les réponses frontend.
- Blueprints immuables et versionnés après publication ; sessions attachées à une version et à un instantané du contenu.
- Fournisseurs IA derrière des interfaces ; scores estimés, provenance et version du rubric obligatoires. Ne pas utiliser de questions ETS protégées.
- Aucun secret, réponse personnelle ou donnée de production dans Git. Pas de données factices présentées comme réelles.
- Les évolutions SQL passent par Flyway ; ne jamais modifier une migration déjà appliquée.
- Contrats dans docs/api ; toute évolution incompatible impose une nouvelle version.
- Tests pertinents JUnit, Pytest et Playwright ; exécuter scripts/check.sh et scripts/smoke.sh avec Compose démarré. Documenter les limites des tests.
- Ne pas ajouter Redis, workers, facturation ou orchestration avant un besoin mesuré.
- Avant livraison : vérifier git diff, tests/build, documentation des commandes et absence de secrets. Ne pas committer sans demande.

- Listening : stockage MinIO privé, autorisations audio liées au propriétaire et à la session ; transcripts après clôture uniquement. Les règles non vérifiées restent des politiques personnalisées, pas des règles ETS. Les sorties TTS restent DRAFT jusqu’à relecture.
