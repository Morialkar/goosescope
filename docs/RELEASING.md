# Publier une version de Goosescope

Références: SPEC F22, AC16; ARCHITECTURE §9. `REQUIRES_REVIEW`: tout ce qui touche à la clé.

## 1. Créer la clé de publication (une seule fois, par Naomi)

Une clé par app: si une clé fuit ou se perd, seule cette app est touchée, et l'app peut
changer de mains sans entraîner les autres.

La clé signe chaque APK publié. Si elle est perdue, les utilisateurs devront désinstaller
l'app pour installer une nouvelle version: la garder en lieu sûr, avec une sauvegarde.
Elle ne va **jamais** dans le dépôt (le `.gitignore` exclut `*.jks`).
Copie de sauvegarde: dossier Proton Drive de Naomi (`~/Library/CloudStorage/ProtonDrive-…/`),
les mots de passe dans un gestionnaire de mots de passe, jamais dans le même dossier que la clé.

```bash
keytool -genkeypair -v -keystore ~/keys/goosescope-release.jks -alias goosescope -keyalg RSA -keysize 4096 -validity 10000
```

Puis ajouter à `~/.gradle/gradle.properties` (hors du dépôt), avec les mots de passe choisis:

```properties
goosescope.storeFile=/Users/<toi>/keys/goosescope-release.jks
goosescope.storePassword=…
goosescope.keyAlias=goosescope
goosescope.keyPassword=…
```

## 2. Construire et vérifier

1. Incrémenter `versionCode` (et `versionName`) dans `app/build.gradle.kts`.
2. `./gradlew :core:test :app:testDebugUnitTest :app:assembleRelease`
3. `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`
   (`~/Library/Android/sdk/build-tools/36.0.0/apksigner`). Le certificat doit être le même à
   chaque version.

Sans les propriétés de signature, `assembleRelease` échoue volontairement.

## 3. Publier

```bash
cp app/build/outputs/apk/release/app-release.apk /tmp/goosescope.apk
gh release create v<versionName> /tmp/goosescope.apk --title "Goosescope <versionName>" --notes "…"
```

Le nom `goosescope.apk` est celui que la page de téléchargement attend
(`/releases/latest/download/goosescope.apk`).
