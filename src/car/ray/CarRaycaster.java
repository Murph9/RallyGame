package car.ray;

import java.util.LinkedList;
import java.util.List;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;

public class CarRaycaster {
	public CarRaycaster() {}
	
	//https://github.com/bubblecloud/jbullet/blob/master/src/main/java/com/bulletphysics/dynamics/vehicle/DefaultVehicleRaycaster.java
	
	public FirstRayHitDetails castRay(PhysicsSpace space, PhysicsRigidBody body, Vector3f from, Vector3f dir) {
		List<PhysicsRayTestResult> results = new LinkedList<PhysicsRayTestResult>();
		space.rayTest(from, from.add(dir), results);
		
		for (PhysicsRayTestResult result: results) {
			if (result.getCollisionObject().getObjectId() == body.getObjectId())
				continue; //no self collision please
			if (!(result.getCollisionObject() instanceof PhysicsRigidBody))
				continue;

			FirstRayHitDetails rd = new FirstRayHitDetails();
			rd.dist = result.getHitFraction() * dir.length();
			rd.pos = from.add(dir.mult(result.getHitFraction()));
			rd.hitNormalInWorld = result.getHitNormalLocal(); //this may look wrong: TODO check if its relative to the object the ray hit
			rd.obj = (PhysicsRigidBody)result.getCollisionObject();
			return rd;
		}
	
		return null;
	}
	
}
