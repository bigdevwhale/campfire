#!/usr/bin/env bash
# Screenshots of the demo build for the READMEs: tools/capture.sh <out-dir> en|ru
# Build it with `./gradlew assembleDebug -Pdemo -Pshots` (app.campfire.shots, never someone's demo):
# the fire stands still there, so uiautomator can read the screen, and one tap on the sky lights it
# for everyone (adb can't hold two stones at once).
set -e
cd "$(dirname "$0")/.."
source tools/phone.sh
OUT="$1"; LANG_="${2:-en}"
PKG=app.campfire.shots
mkdir -p "$OUT"
SHOTS="$OUT"
case "$PKG" in *.demo|*.shots) ;; *) echo "refusing to clear $PKG"; exit 1 ;; esac
"$ADB" shell pm clear $PKG >/dev/null
# AppCompat syncs its (empty) stored language to the system on the very first launch and wipes a
# locale set from adb. So: launch once, then wipe only the data, set the language, launch again —
# and the demo seeds its evenings in that language.
"$ADB" shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; sleep 4
"$ADB" shell am force-stop $PKG
"$ADB" shell run-as $PKG rm -rf databases files/datastore
"$ADB" shell cmd locale set-app-locales $PKG --locales "$LANG_"; sleep 1
"$ADB" shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 6
if [ "$LANG_" = ru ]; then T_BOX="Коробка"; T_SET="Настройки"; T_NIGHT="Ночёвка"; T_LET="коснитесь, чтобы отпустить"; T_WORD="Одно слово о дне"
else T_BOX="The box"; T_SET="Settings"; T_NIGHT="Spending the night"; T_LET="tap to let it go"; T_WORD="One word for the day"; fi
back() { guard $PKG && "$ADB" shell input keyevent 4; sleep 2; }
shot 1-cold
tap_at $PKG 300 800; sleep 2          # light it (shots build: the sky lights it for everyone)
shot 2-fire
tap_at $PKG 540 1400; sleep 2; shot 3-ember-question
tap_at $PKG 760 1840; sleep 1.5; shot 4-branch   # hand the branch to a stone on the right
for _ in 1 2 3 4; do tap_at $PKG 540 1400; sleep 1.5; done
shot 5-closer
# The ash's word is a bare text field: tap it by its class, there is no label on it to find.
tap_field() {
  guard $PKG || return 1
  "$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  local b; b=$("$ADB" exec-out cat /sdcard/ui.xml | python -c "
import sys,re
for n in re.findall(r'<node [^>]*>', sys.stdin.read()):
    if 'EditText' in n:
        a=list(map(int,re.search(r'bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"',n).groups())); print((a[0]+a[2])//2,(a[1]+a[3])//2); break
")
  [ -n "$b" ] && "$ADB" shell input tap $b
}
on_screen() { "$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; "$ADB" exec-out cat /sdcard/ui.xml | grep -q "text=\"$1"; }
# Ask and let go until the logs are spent and the ash asks for its word.
for _ in $(seq 1 16); do
  tap_at $PKG 540 1400; sleep 1.5
  on_screen "$T_WORD" && break
done
shot 6-ash
[ "$LANG_" = en ] && { tap_field; sleep 1; "$ADB" shell input text "thunder"; sleep 1; "$ADB" shell input keyevent 4; sleep 1; shot 6-ash; }
# adb can't put two palms on the fire, so restart the screen to get back to the cold clearing.
"$ADB" shell am force-stop $PKG; "$ADB" shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; sleep 4
tap_text $PKG "$T_BOX"; sleep 2; shot 7-box
back
tap_text $PKG "$T_SET"; sleep 2; shot 8-settings
back
tap_text $PKG "$T_NIGHT"; sleep 2; shot 9-night
tap_at $PKG 540 1200; sleep 1
echo done
