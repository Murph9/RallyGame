package rallygame.car.ray;

public class RayCarControlInput {

    public static final String ACTION_NOTHING = "$";
    public static final String ACTION_LEFT = "Left";
    public static final String ACTION_RIGHT = "Right";
    public static final String ACTION_ACCEL = "Accel";
    public static final String ACTION_BRAKE = "Brake";
    public static final String ACTION_NITRO = "Nitro";
    public static final String ACTION_JUMP = "Jump";
    public static final String ACTION_HANDBRAKE = "Handbrake";
    public static final String ACTION_FLIP = "Flip";
    public static final String ACTION_RESET = "Reset";
    public static final String ACTION_REVERSE = "Reverse";
    public static final String ACTION_IgnoreSteeringSpeedFactor = "IgnoreSteeringSpeedFactor";
    public static final String ACTION_IgnoreTractionModel = "IgnoreTractionModel";

    private final ICarControlled control;

    public RayCarControlInput(ICarControlled control) {
        this.control = control;
    }
    
    public void handleInput(String binding, boolean value, float tpf) {
        if (binding == null) {
            rallygame.helper.Log.p("No binding given?");
            return;
        }
        if (binding == ACTION_NOTHING)
            return; // actually ignore

        // value == 'if pressed (down) - we get one back when its unpressed (up)'
        // tpf is being used as the value for joysticks. deal with it
        switch (binding) {
        case ACTION_LEFT:
            control.setSteerLeft(tpf);
            break;

        case ACTION_RIGHT:
            control.setSteerRight(tpf);
            break;

        case ACTION_ACCEL:
            control.setAccel(tpf);
            break;

        case ACTION_BRAKE:
            control.setBraking(tpf);
            break;

        case ACTION_NITRO:
            control.setNitro(value);
            break;

        case ACTION_JUMP:
            if (value)
                control.jump();
            break;

        case ACTION_HANDBRAKE:
            control.setHandbrake(value);
            break;

        case ACTION_FLIP:
            if (value)    
                control.flip();
            break;

        case ACTION_RESET:
            if (value)
                control.reset();
            break;

        case ACTION_REVERSE:
            control.reverse(value);
            break;

        case ACTION_IgnoreSteeringSpeedFactor:
            control.ignoreSpeedFactor(value);
            break;

        case ACTION_IgnoreTractionModel:
            control.ignoreTractionModel(value);
            break;

        default:
            // nothing
            System.err.println("unknown binding: " + binding);
            break;
        }
    }
}