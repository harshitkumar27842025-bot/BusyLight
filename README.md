# Busy Light (Android / Android TV) - Kuando Busylight Omega

Light is RED when:
  - Word / Excel / PowerPoint / Microsoft 365 app is in the foreground, or
  - Teams / Zoom / Meet / Webex / Skype / Chrome is in the foreground AND the microphone is in use (a call).
Otherwise GREEN. Idle meeting apps do not turn it red.

## Get the APK (no Android Studio needed)
1. Create a free GitHub account, make a new repository, and upload ALL files of this folder
   (including the hidden .github folder - if the upload skips it, use "Add file > Create new file",
   name it  .github/workflows/build-apk.yml  and paste the contents of build-apk.yml).
2. Open the repo's "Actions" tab > "Build APK" > "Run workflow".
3. When it finishes (about 5 min), open the run and download the "busylight-apk" artifact (zip).
   Unzip it to get app-debug.apk.
4. Copy app-debug.apk to the TV (USB drive) and open it with the TV's file manager.
   Allow "install unknown apps" for the file manager when asked.

(Alternative: open this folder in Android Studio and choose Build > Build APK(s).)

## First run on the TV
1. Plug the Busylight into the TV's USB port.
2. Open "Busy Light" and press "Grant Usage Access" (enable Busy Light).
   If the TV has no such screen, run from a PC:
   adb shell appops set com.example.busylight GET_USAGE_STATS allow
3. Press "Start light service" and allow the USB permission popup.

## API (same Wi-Fi)
GET http://<tv-ip>:8765/status | /red | /green | /auto

## Customize
Config.kt - package names, debounce timing, port.
