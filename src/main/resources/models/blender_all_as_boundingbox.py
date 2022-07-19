import bpy
import bmesh

bm = bmesh.new()
collection = bpy.context.scene.collection
for ob in [o for o in bpy.context.scene.objects if o.type == 'MESH']:
    me = ob.data.copy() # create a copy

    verts = [bm.verts.new(b) for b in ob.bound_box]
    bmesh.ops.convex_hull(bm, input=verts)
    bm.to_mesh(me)
    bm.clear()
    
    new_obj = ob.copy()
    new_obj.data = me # needed if copy
    new_obj.animation_data_clear()
    new_obj.modifiers.clear()
    new_obj.name = "collision"

    collection.objects.link(new_obj)
bm.free()

#https://blender.stackexchange.com/questions/116772/how-to-turn-any-mesh-into-its-bounding-box
# How to use:
# 1. open model in blender
# 2. click on the scripting tab at the top
# 3. paste this script into the code window (usually top left)
# 4. press the '>' play button and see boxes around everything