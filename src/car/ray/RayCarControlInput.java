package car.ray;

//TODO change CarUI to use this

public class RayCarControlInput {

    private final ICarControlled control;

    public RayCarControlInput(ICarControlled control) {
        this.control = control;
    }
    
    public void handleInput(String binding, boolean value, float tpf) {
        if (binding == null) {
            helper.Log.p("No binding given?");
            return;
        }

        // value == 'if pressed (down) - we get one back when its unpressed (up)'
        // tpf is being used as the value for joysticks. deal with it
        switch (binding) {
        case "Left":
            control.setSteerLeft(tpf);
            break;

        case "Right":
            control.setSteerRight(tpf);
            break;

        case "Accel":
            control.setAccel(tpf);
            break;

        case "Brake":
            control.setBraking(tpf);
            break;

        case "Nitro":
            control.setNitro(value);
            break;

        case "Jump":
            if (value)
                control.jump();
            break;

        case "Handbrake":
            control.setHandbrake(value);
            break;

        case "Flip":
            if (value)    
                control.flip();
            break;

        case "Reset":
            if (value)
                control.reset();
            break;

        case "Reverse":
            control.reverse(value);
            break;

        case "IgnoreSteeringSpeedFactor":
            control.ignoreSpeedFactor(value);
            break;

        case "IgnoreTractionModel":
            control.ignoreTractionModel(value);
            break;

        default:
            // nothing
            System.err.println("unknown binding: " + binding);
            break;
        }
    }
}