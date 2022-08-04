package rallygame.car.data;

import com.jme3.material.Material;

public enum SurfaceType {
    Normal,
    Dirt,
    Ice,
    Grass,
    None;

    public static SurfaceType fromMaterialName(Material mat) {
        if (mat == null)
            return SurfaceType.Normal;
        var str = mat.getName();
        if (str == null)
            return SurfaceType.Normal;

        str = str.toLowerCase();
        if (str.contains("@dirt"))
            return SurfaceType.Dirt;
        if (str.contains("@ice"))
            return SurfaceType.Ice;
        if (str.contains("@grass"))
            return SurfaceType.Grass;
        if (str.contains("@none"))
            return SurfaceType.None;
        return SurfaceType.Normal;
    }
}
