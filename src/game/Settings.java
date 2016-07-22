package game;

import car.CarData;
import world.*;
import world.wp.WP.DynamicType;

interface State {
	CarData getCar();
	
	WorldType getWorldType();
	StaticWorld getStaticWorld();
	DynamicType getDynamicWorld();
}


public class Settings implements State {

	CarData car;
	
	StaticWorld sworld; //static world type
	DynamicType dworld; //dynamic world type
	
	public CarData getCar() {
		return car;
	}
	
	public WorldType getWorldType() {
		if (sworld != null) {
			return WorldType.STATIC;
		} else if (dworld != null) {
			return WorldType.DYNAMIC;
		} else {
			return WorldType.OTHER;
		}
	}
	public StaticWorld getStaticWorld() {
		return sworld;
	}
	public DynamicType getDynamicWorld() {
		return dworld;
	}
}
