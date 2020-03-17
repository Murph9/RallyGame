# RallyGame

Requires:
Java 8

A simple 3D rally like game in Java.
Can be played on my webpage at https://www.murph9.com under my games

Currently still very much in testing, you can drive around in many different car types. From normal cars, a 4x4, a track car, an impossible hyper-super-car and the infamous White Beast.

The world can be selected from a prebuilt selection of pre made worlds. Or you could be adventurous and pick the dynamic world type and have a selection of tracks generated in front of you.

---
There is the duel files which are meant to be the real game that comes out of this.
It creates its own jar file via gradle task

---

.blend files and the gradle convertAllBlendFiles task and using vscode:
As of 2020-03-17 it doesn't run the gradle build/run calls when you run the application
So after you have generated the assets.jar you will need to 'force java compilation' -> full so it picks up the new files