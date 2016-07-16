package game;

import car.CarData;
import world.*;
import world.wp.WP;

interface State {
	CarData getCar();
	boolean isDynamicWorld();
	
	StaticWorld getStaticWorld(); //only valid on isnot dynamic
	WP[] getDynamicWorld();
}


public class Settings implements State {

	CarData car;
	
	StaticWorld sworld; //static world type
	WP[] dworld; //dynamic world type
	
	public CarData getCar() {
		return car;
	}
	public boolean isDynamicWorld() {
		return (sworld == null);
	}
	public StaticWorld getStaticWorld() {
		return sworld;
	}
	public WP[] getDynamicWorld() {
		return dworld;
	}
}
