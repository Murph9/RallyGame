package rallygame.car.data.wheel;

public enum WheelTractionType {
    Road("road"),
    Track("track"),
    OldRoad("oldRoad"),
    OffRoad("offroad"),
    Wood("wood");

    private String name;
    WheelTractionType(String name) {
        this.name = name;
    }

    public String getFileName() {
        return String.format(YAML_WHEEL_DATA, this.name);
    }

    private static final String YAML_WHEEL_DATA = "/wheeldata/%s.yaml";
}
