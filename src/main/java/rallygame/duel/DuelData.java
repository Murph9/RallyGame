package rallygame.duel;

import rallygame.car.data.Car;
import rallygame.car.data.CarDataAdjuster;

public class DuelData {
    // Contains whatever data is required to run the game global state

    public Car yourCar;
    public CarDataAdjuster yourAdjuster;
    public Car theirCar;
    public CarDataAdjuster theirAdjuster;

    public int wins;
}
