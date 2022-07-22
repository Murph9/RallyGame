package rallygame.car.data;

public enum SurfaceType {
    Normal,
    Dirt,
    Ice,
    Grass;

    public static SurfaceType fromString(String str) {
        str = str.toLowerCase();
        if (str.contains("@dirt"))
            return SurfaceType.Dirt;
        if (str.contains("@ice"))
            return SurfaceType.Ice;
        if (str.contains("@grass"))
            return SurfaceType.Grass;
        return SurfaceType.Normal;
    }
}
