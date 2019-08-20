import bpy
import bmesh
context = bpy.context
scene = context.scene
bm = bmesh.new()
mesh_obs = [o for o in scene.objects if o.type == 'MESH']
for ob in mesh_obs:
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
    scene.objects.link(new_obj)
bm.free()

#https://blender.stackexchange.com/questions/116772/how-to-turn-any-mesh-into-its-bounding-box