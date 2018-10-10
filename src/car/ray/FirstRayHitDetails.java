package car.ray;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;

public class FirstRayHitDetails {
	
	public Vector3f pos;
	public float dist; //0 is at ray start
	public Vector3f hitNormalInWorld;
	public PhysicsRigidBody obj;
}

