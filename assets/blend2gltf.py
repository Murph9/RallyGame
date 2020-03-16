import bpy
import sys

#https://docs.blender.org/api/current/bpy.ops.export_scene.html
bpy.ops.export_scene.gltf(filepath=bpy.data.filepath, export_materials=True, export_apply=True, check_existing=False)
