# 45unlock

Android-TV-App, die einen Ticketcode der FRITZ!Box-Kindersicherung per Fernbedienung einlöst. Statt im Browser des Fernsehers die Sperrseite zu bedienen, tippt man die sechs Ziffern ein und drückt Enter. Die App schließt sofort, löst das Ticket im Hintergrund ein und zeigt das Ergebnis als kurzes Banner über der laufenden App, z. B. „45 Minuten freigeschaltet“.

- Läuft bei einem laufenden Ticket ein weiteres Ticket ein, hängt die FRITZ!Box die 45 Minuten an („45 Minuten vorgemerkt – insgesamt noch X Minuten“).
- Oben rechts zeigen kleine Timer die Restzeit der laufenden Tickets.
- Die App spricht nur mit der FRITZ!Box: `fritz.box`, als Rückfall `192.168.178.1` (Werkseinstellung) und `192.168.188.1`. Der Code landet nie in einem Log oder im App-Speicher.

## Voraussetzungen

- Android TV 14 oder neuer (API 34)
- FRITZ!Box mit Kindersicherung, deren Zugangsprofil Tickets erlaubt
- Zum Bauen: JDK 17 oder neuer und das Android SDK. Den SDK-Pfad setzt `ANDROID_HOME` oder `sdk.dir` in `local.properties`, fehlende SDK-Pakete lädt Gradle selbst.

## Bauen

```sh
./gradlew assembleRelease     # erzeugt app/build/dist/45unlock.apk
```

`assembleRelease` kopiert die APK nach `app/build/dist/45unlock.apk` und bricht ab, wenn sie native Bibliotheken (`lib/`) enthält. Die APK bleibt so universell.

## Installation

Am Fernseher die Entwickleroptionen und das USB- bzw. Netzwerk-Debugging einschalten, dann:

```sh
adb connect <IP des Fernsehers>:5555
adb install -r app/build/dist/45unlock.apk
adb shell pm grant app.unlock45.tv android.permission.POST_NOTIFICATIONS
adb shell appops set app.unlock45.tv SYSTEM_ALERT_WINDOW allow
```

Die App fragt nie selbst nach Berechtigungen. Ohne `POST_NOTIFICATIONS` läuft die Einlösung trotzdem, nur die Benachrichtigung mit dem Ergebnis fehlt. `SYSTEM_ALERT_WINDOW` („Über anderen Apps einblenden“) braucht das Ergebnis-Banner über der laufenden App. Android TV hat dafür meist keinen Menüpunkt in den Einstellungen. Ohne die Berechtigung erscheint das Ergebnis nur als kurzer Text-Toast.

Der Release-Build wird mit dem lokalen Debug-Schlüssel (`~/.android/debug.keystore`) signiert, weil die App nur per ADB verteilt wird. `adb install -r` klappt deshalb nur, wenn die installierte Version mit demselben Schlüssel signiert ist. Sonst die App einmal deinstallieren.

## Schrift

Die UI-Schrift ist Inter Display SemiBold (SIL Open Font License, `licenses/Inter-OFL.txt`), reduziert auf Latin-1 und die deutschen Satzzeichen.
