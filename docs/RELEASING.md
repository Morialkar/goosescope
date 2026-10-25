# Publier une version de Goosescope

Références: SPEC F22, AC16; ARCHITECTURE §9. `REQUIRES_REVIEW`: tout ce qui touche à la clé.

## La clé de publication

Une clé par app: si une clé fuit ou se perd, seule cette app est touchée, et l'app peut changer
de mains sans entraîner les autres. Si la clé est perdue, les utilisateurs doivent désinstaller
l'app pour installer une nouvelle version.

Elle vit dans `signing/`, dossier ignoré par git (le dépôt est public):

- `signing/goosescope-release.jks`: magasin PKCS12, alias `goosescope`, RSA 4096, valide 10 000 jours.
- `signing/password`: mot de passe aléatoire (`secrets.token_urlsafe(32)`), jamais affiché.
  `app/build.gradle.kts` le lit depuis ce fichier.

Sauvegarde: les deux fichiers sont copiés dans le dossier Proton Drive de Naomi,
`Goosescope signing/`. Pour reconstruire sur une autre machine, recopier ces deux fichiers dans
`signing/`.

Créée le 2026-10-23 sans que le mot de passe passe par une commande ou un terminal:

```bash
python3 -c "import secrets,sys; sys.stdout.write(secrets.token_urlsafe(32))" > signing/password
keytool -genkeypair -keystore signing/goosescope-release.jks -storetype PKCS12 -alias goosescope -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Goosescope, O=Morialkar" -storepass:file signing/password -keypass:file signing/password
```

## Construire et vérifier

1. Incrémenter `versionCode` (et `versionName`) dans `app/build.gradle.kts`.
2. `./gradlew :core:test :app:testDebugUnitTest :app:assembleRelease`
3. `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`
   (`~/Library/Android/sdk/build-tools/36.0.0/apksigner`). Le certificat doit être le même à
   chaque version.

Sans `signing/`, `assembleRelease` échoue volontairement plutôt que de produire un APK non signé.

## Publier

Seulement après la relecture des fiches (AC14) et la validation visuelle (AC8).

```bash
cp app/build/outputs/apk/release/app-release.apk /tmp/goosescope.apk
gh release create v<versionName> /tmp/goosescope.apk --title "Goosescope <versionName>" --notes "…"
```

Le nom `goosescope.apk` est celui que la page de téléchargement attend
(`/releases/latest/download/goosescope.apk`).

## Google Play (SPEC révision 9)

Outil: Gradle Play Publisher. La fiche est versionnée dans `app/src/main/play/` (titres,
descriptions, notes de version en `en-US` et `fr-CA`, icône, bannière, captures 1440×2880).

### Clé du compte de service (une seule fois, par Naomi)

1. Google Cloud: créer un compte de service, puis une clé JSON pour ce compte.
2. Play Console → Utilisateurs et autorisations: inviter l'adresse du compte de service, avec les
   droits sur Goosescope (publier sur les pistes de test, gérer la fiche).
3. Enregistrer la clé sous `signing/play-service-account.json` (dossier ignoré par git) et en garder
   une copie dans Proton Drive, `Goosescope signing/`. Ne jamais l'afficher ni la coller ailleurs.

### Avertissements Play attendus

- « Symboles de débogage natifs manquants »: le code natif vient d'AndroidX (CameraX, Compose) et
  ces bibliothèques sont livrées déjà sans symboles; il n'y a rien à fournir. Avertissement normal.
- Le fichier de désobscurcissement R8 (`mapping.txt`) est joint automatiquement à chaque AAB.

### Signature Play (fait le 2026-10-24)

Play App Signing utilise **notre** clé (`signing/goosescope-release.jks`), transmise à Google par
PEPK avant le premier build: l'app installée depuis Play et les APK GitHub ont la même signature
(SHA-256 du certificat `1da2816b…2ddf`). Le ZIP chiffré `goosescope-key-for-play.zip` est gardé
dans Proton Drive avec la clé.

### Commandes (seulement à la demande de Naomi, SPEC révision 9)

- Mettre à jour la fiche: `./gradlew publishReleaseListing --rerun`
  (`--rerun`: l'outil croit sinon la fiche déjà à jour après un envoi échoué).
  Pour une **nouvelle langue**, deux envois: d'abord le texte seul (images mises de côté, langue
  par défaut inchangée), qui crée la langue sur Play; ensuite le tout. Sinon Google refuse les
  images (« Listing for language not found ») ou la langue par défaut (« No listing provided in
  the default language »). Une « Internal error » de Google pendant les images se règle en
  relançant.
- Envoyer un build sur la piste interne, en brouillon: `./gradlew publishReleaseBundle`
  (incrémenter `versionCode` avant).
- Promouvoir vers une autre piste: `./gradlew promoteReleaseArtifact --from-track internal --promote-track alpha`

Restent dans la Play Console: classification du contenu, sécurité des données (aucune donnée
recueillie ni partagée), catégorie, public cible (13 ans et plus), politique de confidentialité
`https://goosescope.morialkar.com/privacy.html`.
