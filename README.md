# RallyGame

- Requires Java 8 or better and a non `potato` computer

A simple 3D rally like game in Java.
Can be played on my webpage at https://www.murph9.com/mygames

Currently still very much in testing, you can drive around in many different car types. From normal cars, a 4x4, a track car, an impossible hyper-super-car and the infamous White Beast.

The world can be selected from a prebuilt selection of pre made worlds. Or you could be adventurous and pick the dynamic world type and have a selection of tracks generated in front of you.

---
The duel project is mean to be the real game that comes out of this.
It is a separate download, or gradle task or fat jar file via gradle task

---

## Build steps

Java 8 and blender to compile the project, see gradle file to set the blender install location

Blender is required to generate the `*.gltf` files which are in the source as `*.blend`

The commands you need:
- `./gradlew build`
- `./gradlew run`
- `./gradlew runDuel`

### Debugging notes

.blend files and the gradle convertAllBlendFiles task and using vscode:
As of 2020-03-17 it doesn't run the gradle build/run calls when you run the application
So after you have generated the assets.jar you will need to 'force java compilation' -> full so it picks up the new files


my testing on ubuntu with blender required me to run this command to get the blender commandline to work:

`python3 -m pip install numpy`