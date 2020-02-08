package game;

import car.data.Car;
import world.IWorld;

public interface IChooseStuff {
	void chooseCar(Car car);
	void chooseMap(IWorld world);
}