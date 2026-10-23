# AC1 / NF7 — démarrage à froid

- Appareil: Galaxy Tab S10 FE (SM-X528U), Android 16, ADB sans fil.
- Mesure: `adb shell am start -W -n com.morialkar.goosescope/.app.MainActivity` après `am force-stop`.
- 2026-10-23: TotalTime 832 ms (T10), 864 ms (T12). Cible ≤ 2 s: **conforme**.
