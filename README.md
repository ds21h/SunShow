# SunShow

This is an Android app to calculate sun and moondata.
It calculates for a given date and location the times of sunrise and sunset. It also calculates the moonrise and moonset times together with the moonphase.

The actual calculator comes from T. Alonso Albi (http://conga.oan.es/~alonso/doku.php?id=blog:sun_moon_position)

The project structure is that of AndroidStudio.

History:

Version 1.3.1 - 18-02-2022
= Upgraded to API 31 (Android 12)
- Upgraded to new maps SDK

Version 1.3 - 10-08-2021
- Replaced AsyncTask (depreciated) with Runnable in SingleThreadPool on app level.

Version 1.2.1 - 05-03-2021
- Bugfix: MapLocation in AppData was not properly initialised.
- SunMoonCalculator upgraded to latest version (2021-02-23)
- Updated constructor call for SunMoonCalculator. It needs an additional integer for observers altitude (0).

Version 1.2 - 05-10-2020
- SunMoonCalculator upgraded to latest version (2020-08-07)
- Currentlocation now has 3 stages:
  - Init: Longitude and lattitude are both zero. Location displayed in red and italics
  - Last known: The last known location of the device. Normally this is the current location, but this is not reliable. Location displayed in italics.
  - Fix: This is the real current location. Location normally displayed.
- Minimum API level is now 19 (Android 4.4)
- Known defect: Moon pictures are valid only for the northern hemisphere

Version 1.1 - 30-05-2019
- SunMoonCalculator upgraded to latest version (2018-11-26)
- Mooncalculations now regard the night starting at the selected date
- When the calculated moon time is in the next calender day the time is followed by '(+)'
- Shift over of moon calculations is on new moon

Version 1.0 - 29-04-2019
- First version
