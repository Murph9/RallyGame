package rallygame.game;

import rallygame.car.data.Car;
import rallygame.world.IWorld;

public interface IChooseStuff {
	void chooseCar(Car car);
	void chooseMap(IWorld world);
}