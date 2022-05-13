package survival.controls;

import java.util.function.BiConsumer;

import com.jme3.bullet.control.RigidBodyControl;

public interface PhysicsBehaviour extends BiConsumer<RigidBodyControl, Float> {
}