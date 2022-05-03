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

	public String getCarName() {
		return carName;
	}
}