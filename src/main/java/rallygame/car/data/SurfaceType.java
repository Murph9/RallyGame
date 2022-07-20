package rallygame.car.data;

public enum SurfaceType {
    Normal,
    Dirt,
    Snow,
    Ice,
    Grass;

    public static SurfaceType fromString(String str) {
        //if (str.contains("ss"))
            //return Grass;
        return SurfaceType.Normal;
    }
}
