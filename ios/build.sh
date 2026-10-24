#!/bin/sh
# Construit l'app iOS et l'installe sur l'iPhone de test (PLAN-IOS I2).
# Usage: ios/build.sh [build|install|test]. Identifiants locaux dans ios/local.env (non commité).
set -eu
cd "$(dirname "$0")/.."
. ios/local.env
: "${JAVA_HOME:=/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
export JAVA_HOME
ACTION="${1:-install}"

./gradlew :core:assembleGoosescopeCoreReleaseXCFramework --console=plain -q
ios/tools/check-strings.sh
(cd ios && xcodegen generate --quiet)

if [ "$ACTION" = "test" ]; then
    xcodebuild -project ios/Goosescope.xcodeproj -scheme Goosescope -destination "id=$DEVICE" \
        -derivedDataPath ios/build DEVELOPMENT_TEAM="$TEAM" -allowProvisioningUpdates -quiet test
    exit 0
fi

xcodebuild -project ios/Goosescope.xcodeproj -scheme Goosescope -configuration Debug \
    -destination "id=$DEVICE" -derivedDataPath ios/build DEVELOPMENT_TEAM="$TEAM" \
    -allowProvisioningUpdates -quiet build
# Politique de confidentialité: « nous vérifions chaque version » (SPEC-IOS AC26). Échoue si du réseau apparaît.
ios/tools/check-no-network.sh ios/build/Build/Products/Debug-iphoneos/Goosescope.app

if [ "$ACTION" = "install" ]; then
    APP=ios/build/Build/Products/Debug-iphoneos/Goosescope.app
    xcrun devicectl device install app --device "$DEVICE" "$APP" >/dev/null
    xcrun devicectl device process launch --device "$DEVICE" com.morialkar.goosescope >/dev/null
    echo "Installé et lancé sur $DEVICE"
fi
