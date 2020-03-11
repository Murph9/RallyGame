import bpy
import sys

bpy.ops.export_scene.gltf(filepath=bpy.data.filepath, export_materials=False, check_existing=False)
