import bpy
import os

path = 'C:/path/to/files/'  # set this path

for root, dirs, files in os.walk(path):
    for f in files:
        if f.endswith('.blend'):
            mesh_file = os.path.join(path, f)
            obj_file = os.path.splitext(mesh_file)[0] + ".gltf"

            bpy.ops.object.select_all(action='SELECT')
            bpy.ops.object.delete()

            bpy.ops.wm.open_mainfile(filepath=filepath_src)

            bpy.ops.object.select_all(action='SELECT')

            bpy.ops.export_scene.gltf(filepath=obj_file, export_materials=False, export_force_sampling=True, check_existing=False)

