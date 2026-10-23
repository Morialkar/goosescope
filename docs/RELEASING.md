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
