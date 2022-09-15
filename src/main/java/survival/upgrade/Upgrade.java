package survival.upgrade;

import java.util.function.Consumer;

import rallygame.car.data.CarDataConst;
import survival.GameState;

public abstract class Upgrade<T> {
    public static Upgrade<GameState> ShorterTimer = new GameStateUpgrade(false, "Reduce the amount of time to reach the next checkpoint", x -> x.CheckpointTimerLength -= 3);
    public static Upgrade<GameState> IncreaseCheckpointRange = new GameStateUpgrade(false, "Increase the minimum distance of a checkpoint", x -> x.CheckpointDistance *= 1.1f);
    public static Upgrade<GameState> WaveSpeedInc = new GameStateUpgrade(false, "Increase the speed waves are spawned", x -> x.WaveSpeed -= 0.2f);
    public static Upgrade<GameState> ReducePlayerHealth = new GameStateUpgrade(false, "Reduce the amount of your health", x -> x.PlayerMaxHealth -= 2);
    
    // public static Upgrade<CarDataConst> MuchPOWER = new CarDataConstUpgrade(true, "MUCH POWER (5% nitro increase) [ctrl]", x -> x.nitro_force *= 1.05f);
    /*public static Upgrade<CarDataConst> ImproveGrip = new CarDataConstUpgrade(true, "Improve Grip (3%)", (data) -> {
        for (int i = 0; i < 4; i++) {
            data.wheelData[i].traction.pjk_lat.D *= 1.03f;
            data.wheelData[i].traction.pjk_long.D *= 1.03f;
        }
    });*/
    public static Upgrade<CarDataConst> ImproveEngine = new CarDataConstUpgrade(true, "Improve Engine (4%)", (data) -> {
        for (int i = 0; i < data.e_torque.length; i++) {
            data.e_torque[i] *= 1.04f;
        }
    });
    public static Upgrade<GameState> LongerTimer = new GameStateUpgrade(true, "Increase checkpoint time by 5 sec", x -> x.CheckpointTimerLength += 5);

    //TODO a little complex: public static Upgrade<List<Ability>> QuickerExplodeAbility = new Upgrade<>(true, "Reduce Explode Ability (-5%)", x -> x.ExplodeAbilityTimerMax *= 0.95f);
    // public static Upgrade<List<Ability>> StrongerExplodeAbility = new Upgrade<>(true, "Increase Explode Ability Strength (5%)", x -> x.ExplodeAbilityStrength *= 1.05f);

    // public static Upgrade<GameState> Heal = new GameStateUpgrade(true, "Half Heal", x -> x.PlayerHealth += x.PlayerMaxHealth/2f);
    // public static Upgrade<CarDataConst> ReduceFuelUse = new CarDataConstUpgrade(true, "Reduce Full use by (10%)", x -> x.fuelRpmRate *= 0.9f);

    public final boolean positive;
    public final String label;
    private final Consumer<T> func;

    public Upgrade(boolean positive, String label, Consumer<T> func) {
        this.positive = positive;
        this.label = label;
        this.func = func;
    }

    public void accept(T t) {
        func.accept(t);
    }

    public Consumer<T> get() {
        return func;
    }
}
