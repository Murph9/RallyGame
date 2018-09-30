package car.ray;

import com.jme3.math.Vector3f;

public class RayWheel {

	protected final int num;
	//protected final Spatial spat; //TODO move
	protected final WheelDataConst data;
	
	public boolean inContact;
	public Vector3f curBasePosWorld;
	public float susDiffLength;
	public float susForce;
	
	public float radSec;
	public float skidFraction; //was 'skid'
	
	public float maxLong;
	public float maxLat;
	
	public RayWheel(int num, WheelDataConst data, Vector3f offset) {
		this.num = num;
		this.data = data;
		
		//TODO move to FakeRayWheelControl
		/*
		//hard coded object (roughly wheel shaped...)
		Material material = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		material.setColor("Color", ColorRGBA.Red);
		
		Cylinder cyl = new Cylinder(16, 16, data.radius, data.radius*0.6f, true);
        Geometry cylGeometry = new Geometry("chassis", cyl);
        cylGeometry.setMaterial(material);
        rootNode.attachChild(cylGeometry);
        
        //rotate and translate the rootNode
        rootNode.setLocalTranslation(offset);
        
        this.spat = cylGeometry;
        */
	}
	
	//TODO what does this class actually 'do'?
}
