#!/bin/sh
# Lit le nom de couleur affiché par Goosescope sur la tablette (PLAN T13), sans toucher à l'écran.
adb shell uiautomator dump /sdcard/gs-ui.xml >/dev/null 2>&1
adb shell cat /sdcard/gs-ui.xml | grep -o 'text="[^"]\+"' | sed 's/text=//' | tr '\n' ' '
echo
