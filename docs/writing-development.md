# Développement Writing
`./scripts/init-local.sh` conserve les valeurs locales et génère `AI_INTERNAL_TOKEN` dans `.env` ignoré (permissions 0600). Ne jamais copier ce secret dans Git ou un journal.

Commandes :

```sh
./scripts/install.sh
make up
make smoke
./scripts/check.sh
```

`WRITING_SECTION_DURATION_SECONDS=300` publie une nouvelle version des blueprints personnalisés chronométrés si la durée change. Les sessions existantes gardent leur échéance. Practice reste sans limite. Les brouillons acquittés sont dans PostgreSQL, la copie non acquittée dans sessionStorage, limitée au compte courant et supprimée à la fermeture/logout. `spellcheck=false` désactive l'aide native intégrée ; aucune interface web ne bloque l'assistance externe.

IA désactivée par défaut. Pour un essai réel autorisé avec budget, configurer uniquement dans l'environnement sécurisé : `WRITING_AI_PROVIDER=openai` ou `anthropic`, `WRITING_AI_MODEL` avec un modèle compatible et `WRITING_AI_API_KEY`. Destination HTTPS fixe `api.openai.com` ou `api.anthropic.com`. Ne jamais mettre la clé dans la documentation, Git ou le navigateur. Recréer ai-service après changement, puis relancer une évaluation indisponible. Aucun appel fournisseur payant n'est nécessaire aux tests.

Le prompt est `apps/ai-service/app/evaluation/prompts.py`, le schéma `schema.py`, les rubrics sont introduits dans V6. Tout changement de rubric ou prompt doit créer une nouvelle version ; ne jamais réécrire V1–V6 appliquées. Les trois exercices originaux et variantes ont été relus en interne, difficultés indicatives A2/B1/B2 ; calibration externe et corpus étendu restent à faire. Les contenus générés restent DRAFT tant qu'ils n'ont pas été relus.

Les tests Java emploient un serveur HTTP fournisseur simulé ; E2E utilise une vraie communication Spring/FastAPI mais un fournisseur déterministe dans un projet Docker distinct. Les rapports affichent TEST_ONLY. Ce mécanisme n'est pas disponible en APP_ENV=production. Les bases et volumes E2E sont nettoyés ; aucune tentative simulée n'est ajoutée à la base personnelle.

La vérification d'un span prouve sa présence, pas le bien-fondé de sa correction. Le benchmark original de tests contrôle le contrat, les variantes et l'ancrage, pas la qualité d'un modèle distant. La version améliorée peut dériver du sens : relecture humaine nécessaire. Les recommandations sont des suggestions pédagogiques, pas un diagnostic certifié. Suppression en cascade des comptes/sessions/jobs prévue par le schéma ; API de suppression et politique de rétention à livrer avant commercialisation.
