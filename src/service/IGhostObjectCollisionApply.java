package service;

import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;

public interface IGhostObjectCollisionApply {

    void ghostCollision(GhostControl control, RigidBodyControl control2);
}