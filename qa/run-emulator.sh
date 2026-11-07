#!/usr/bin/env bash
# Runs inside the GitHub emulator: installs the app, runs the walkthrough, collects results.
set -x
cd android-mycity
OUT=qa-out; mkdir -p $OUT
adb emu geo fix 78.3915 17.4483            # Madhapur, Hyderabad
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
./gradlew installDebug installDebugAndroidTest --stacktrace > $OUT/install.txt 2>&1 || echo "INSTALL FAILED" >> $OUT/install.txt
adb logcat -c
adb shell am instrument -w -r -e class com.sheharsaathi.app.QaWalkthroughTest \
  com.sheharsaathi.app.test/androidx.test.runner.AndroidJUnitRunner > $OUT/instrument.txt 2>&1
adb pull /sdcard/Android/data/com.sheharsaathi.app/files/qa $OUT/shots || true
adb logcat -d -v time > $OUT/logcat-full.txt || true
grep -E "AndroidRuntime|FATAL|E/|W/System.err|sheharsaathi" $OUT/logcat-full.txt | grep -v -E "chatty|ResourcesCompat" | tail -400 > $OUT/logcat-errors.txt || true
exit 0
