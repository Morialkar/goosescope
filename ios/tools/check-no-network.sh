#!/bin/bash
# SPEC-IOS NF12, AC26: iOS n'a pas de permission réseau à retirer, donc on vérifie le binaire lui-même.
# Aucun framework réseau lié, aucun symbole réseau importé, dans l'app et dans ses extensions.
set -eu
APP="${1:-$(dirname "$0")/../build/Build/Products/Debug-iphoneos/Goosescope.app}"
FORBIDDEN_LIBS='CFNetwork|/Network\.framework|NetworkExtension'
FORBIDDEN_SYMBOLS='NSURLSession|NSURLConnection|_nw_|CFSocket|CFStream|_getaddrinfo|_socket$|_connect$|WKWebView'
status=0
while IFS= read -r bin; do
    name="${bin#"$APP"/}"
    libs=$(otool -L "$bin" | tail -n +2 | grep -E "$FORBIDDEN_LIBS" || true)
    syms=$(nm -u "$bin" 2>/dev/null | grep -E "$FORBIDDEN_SYMBOLS" || true)
    if [ -n "$libs$syms" ]; then
        echo "RÉSEAU dans $name:"; echo "$libs"; echo "$syms"; status=1
    else
        echo "OK  $name: aucun framework ni symbole réseau"
    fi
done < <(find "$APP" -type f \( -name Goosescope -o -name GoosescopeControls -o -name GoosescopeCapture \) -perm -u+x)
exit $status
