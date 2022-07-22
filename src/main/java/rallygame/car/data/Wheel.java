package rallygame.car.data;

public enum Wheel {
    Road("name"),
    Track("track"),
    oldRoad("oldRoad"),
    OffRoad("offroad"),
    Wood("wood");

    private String name;
    Wheel(String name) {
        this.name = name;
    }

    public String getFileName() {
        return String.format(YAML_WHEEL_DATA, this.name);
    }

    private static final String YAML_WHEEL_DATA = "/cardata/wheel/{0}.yaml";
}
