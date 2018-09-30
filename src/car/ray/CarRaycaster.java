package car.ray;

import java.util.ArrayList;
import java.util.List;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;

import helper.H;

public class CarRaycaster {
	public CarRaycaster() {}
	
	//https://github.com/bubblecloud/jbullet/blob/master/src/main/java/com/bulletphysics/dynamics/vehicle/DefaultVehicleRaycaster.java
	
	public FirstRayHitDetails castRay(PhysicsSpace space, PhysicsRigidBody body, Vector3f from, Vector3f dir) {
		Vector3f _from = from;
		Vector3f _dir = dir;
		
		List<PhysicsRayTestResult> results = new ArrayList<PhysicsRayTestResult>();
		space.rayTest(_from, _from.add(_dir), results);
		
		for (PhysicsRayTestResult result: results) {
			if (result.getCollisionObject().getObjectId() == body.getObjectId())
				continue;
			
			FirstRayHitDetails rd = new FirstRayHitDetails();
			rd.hitFraction = result.getHitFraction();
			rd.dist = rd.hitFraction * dir.length();
			rd.pos = _from.add(_dir.mult(rd.hitFraction));
			rd.hitNormalInWorld = result.getHitNormalLocal(); //this may look wrong: TODO check if its relative to the object this hit
			if (result.getCollisionObject() instanceof PhysicsRigidBody) {
				rd.obj = (PhysicsRigidBody)result.getCollisionObject();
			} else {
				H.p("no 'real' object found ray casting from: " + body);
			}
			return rd;
		}
	
		return null;
	}
	
}
