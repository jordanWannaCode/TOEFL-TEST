# Contenus et audios Listening
La banque initiale contient quatre groupes originaux et sept questions objectives. Source : content/listening/manifest.json ; copie embarquée et WAV sous apps/api/src/main/resources/listening. Audio synthétique eSpeak NG, deux configurations de voix en-us/en-gb ; aucun appel LLM ou fournisseur distant.
| Groupe | Questions | Correction relue |
|---|---|---|
| Finding the lab | 1 / INTENT | a : une question de localisation reçoit une localisation. |
| A study room reservation | 2 / DETAIL, INTENT | b : déplacement à 3h30 ; c : vérifier si une démarche supplémentaire est nécessaire. |
| A change to the campus shuttle | 2 / MAIN_IDEA, DETAIL | d : arrêt temporaire ; a : marcher jusqu'au bâtiment des sciences. Les horaires ne changent pas. |
| How seeds travel | 2 / MAIN_IDEA, INFERENCE | b : deux stratégies de dispersion ; c : le déplacement seul ne garantit pas des conditions de croissance favorables. |
Relecture interne des scripts, questions, distracteurs et clés effectuée lors de cette livraison. WAV générés à partir des tours du script, métadonnées/associations et checksums validés, décodage et lecture réelle vérifiés. Les voix sont artificielles ; prosodie, accent, difficulté et qualité pédagogique nécessitent une revue humaine externe avant banque commerciale. Il ne s'agit pas de questions officielles ni d'une calibration ETS.

## Import de fichiers manuels
Le format manifest schemaVersion=1 décrit chaque révision : id UUID, version, taskType, title, difficulty, speakers [{id,label,voice}], turns [{speakerId,text}], questions [{id,prompt,skill,options:[{id,text}]}], answers, feedback, audioFile, durationMs, sha256, synthetic et reviewed.
Formats initiaux : WAV PCM décodable, 10 Mio maximum, nom de fichier composé de lettres/chiffres/tirets/underscores. SHA-256 et durée doivent correspondre au fichier. Quatre options distinctes, une clé et une explication par question ; tours liés à des locuteurs définis. L'import rejette chemins sortants, checksum/durée invalides et associations incohérentes avant publication.
Définir LISTENING_IMPORT_DIRECTORY dans le processus backend vers un dossier contenant manifest.json et les WAV. Dans Docker, utiliser un override local avec environment: {LISTENING_IMPORT_DIRECTORY: /imports} et un volume read-only ./mon-import:/imports:ro, puis recréer le backend. Aucun endpoint public d'import administratif n'est ouvert.
reviewed=false crée DRAFT et n'expose pas l'exercice. La relecture puis reviewed=true peut promouvoir le même contenu inchangé. Une révision publiée est immuable ; un contenu différent impose un nouvel ID/version. Les blueprints publiés sont versionnés ; importer une nouvelle sélection peut publier une nouvelle section personnalisée sans modifier les tentatives existantes. Les mêmes octets audio peuvent être partagés entre révisions.

## Synthèse locale et futurs fournisseurs
```bash
# eSpeak NG doit être installé, aucune clé réseau nécessaire.
python3 scripts/generate-listening-audio.py
# Options : ESPEAK_EXECUTABLE et ESPEAK_DATA_PATH pour une installation locale.
```
Le script marque toujours les sorties DRAFT (reviewed=false). Après relecture, mettre reviewed=true dans le manifest et synchroniser la copie embarquée, ou importer le dossier révisé. Ne pas régénérer une révision déjà publiée sous le même ID si ses octets changent.
SpeechProvider est l'interface Python ; EspeakSpeechProvider est l'adaptateur local. D'autres fournisseurs pourront implémenter ce contrat avec leurs propres voix. Le service de correction objective Spring reste indépendant de cette chaîne.
