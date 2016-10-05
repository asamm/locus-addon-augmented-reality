# Locus Map - add-on Augmented reality

Add-on for [Locus Map](http://www.locusmap.eu) (Android) application, focused on visualization of selected points in the phone camera view.

Add-on is partially independent on Locus Map application, but not entirely.

- handling (open, display, update data) is made over [Locus API](https://github.com/asamm/locus-api/blob/master/locus-api-android/src/main/java/locus/api/android/features/augmentedReality/UtilsAddonAR.java)
- open is anyway hardcoded directly in Locus Map, not over API (over API currently not exists function "give me all visible points")

### Possible improvements

- more stable sensors, fixing some orientation troubles on some devices
- display of track(s) also in AR view
- handle received data also over plain intent from other apps
- remove "hardcoded" part from Locus Map and extends Locus API to offer also full list of visible points

### Important information

Add-on use [global gradle parameters](https://github.com/asamm/locus-api/wiki/Adding-Locus-API-to-project#using-global-parameters) defined for a whole project.

Available at: [Google Play](https://play.google.com/store/apps/details?id=menion.android.locus.addon.ar)
Inspiration: [Mixare](http://www.mixare.org/) project
