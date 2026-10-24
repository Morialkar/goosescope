#!/bin/bash
# Les deux langues iOS ont exactement les mêmes clés (STANDARDS §3, IMPACT-IOS §4).
set -eu
cd "$(dirname "$0")/../Goosescope"
keys() { sed -n 's/^"\([^"]*\)" = .*/\1/p' "$1" | sort; }
if ! diff <(keys en.lproj/Localizable.strings) <(keys fr.lproj/Localizable.strings) >/dev/null; then
    echo "Clés différentes entre en.lproj et fr.lproj:"; diff <(keys en.lproj/Localizable.strings) <(keys fr.lproj/Localizable.strings); exit 1
fi
echo "Textes iOS: mêmes clés en anglais et en français ($(keys en.lproj/Localizable.strings | wc -l | tr -d ' '))"
