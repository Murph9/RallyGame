package rallygame.car.data;

public enum Car {
	Survivor("Survivor"),

	Normal("Normal"),
	Runner("Runner"),
	Rally("Rally"),
	Roadster("Roadster"),
	
	Hunter("Hunter"),
	Ricer("Ricer"),
	Muscle("Muscle"),
	Wagon("Wagon"),
	Bus("Bus"),
	
	Ultra("Ultra"),
	LeMans("LeMans"),
	Inline("Inline"),
	TouringCar("Touring"),
	Hill("Hill"),

	WhiteSloth("WhiteSloth"),
	Rocket("Rocket"),
	
	Debug("Debug"),
	;
	
	private final String carName;
	private Car(String carName) {
		this.carName = carName;
	}

	public String getName() { return this.carName; }
	public String getFileName() {
		return String.format(YAML_CAR_DATA, this.carName);
	}
	
    private static final String YAML_CAR_DATA = "/cardata/%s.yaml";
}