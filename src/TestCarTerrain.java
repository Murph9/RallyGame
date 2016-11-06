
import java.util.LinkedList;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Line;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;

public class TestCarTerrain extends SimpleApplication implements ActionListener {
	
	private final LinkedList<Geometry> skidMarks = new LinkedList<Geometry>();
	
	private BulletAppState bulletAppState;
    private VehicleControl player;
    private Node carNode;
    private Material mat;
    
    private BitmapText hudText;
    
    private final float accelerationForce = 1500.0f;
    private final float brakeForce = 200.0f;
    private final float steeringAmount = 0.5f;
    private float steeringValue = 0;
    private float accelerationValue = 0;
    private Vector3f jumpForce = new Vector3f(0, 1000, 0);
    
    private int frameCount = 1;
    private final boolean ifArrows = false;
    
    private final boolean ifShadow = true;
    private DirectionalLightShadowRenderer dlsr;
    private DirectionalLightShadowFilter dlsf;
    

    public static void main(String[] args) {
    	TestCarTerrain app = new TestCarTerrain();
    	AppSettings settings = new AppSettings(true);
    	settings.setResolution(1300,720);
    	settings.setFrameRate(60);
        app.setSettings(settings);

        app.setShowSettings(false);
        app.setDisplayStatView(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
//        bulletAppState.setDebugEnabled(true);
        
        mat = new Material(getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
//        mat = new Material(getAssetManager(), "Common/MatDefs/Misc/ShowNormals.j3md");
        mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        
        createWorld();
        
        setupKeys();
        buildPlayer();
        initCamera();
        
//        initFire();
        
        hudText = new BitmapText(guiFont, false);          
        hudText.setSize(guiFont.getCharSet().getRenderedSize());      // font size
        hudText.setColor(ColorRGBA.White);                            // font color
//        hudText.setText(player.getCurrentVehicleSpeedKmHour()+"km/h");    // the text
        hudText.setText(player.getLinearVelocity().length()+"m/s");    // the text
        hudText.setLocalTranslation(0, 200/*hudText.getLineHeight()*/, 0); // position
        guiNode.attachChild(hudText);
    }
    
    /*private void initFire() {
    	ParticleEmitter fire = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
        Material mat_red = new Material(assetManager, 
                "Common/MatDefs/Misc/Particle.j3md");
        mat_red.setTexture("Texture", assetManager.loadTexture(
                "Effects/Explosion/flame.png"));
        fire.setMaterial(mat_red);
        fire.setImagesX(2); 
        fire.setImagesY(2); // 2x2 texture animation
        fire.setEndColor(  new ColorRGBA(1f, 0f, 0f, 1f));   // red
        fire.setStartColor(new ColorRGBA(1f, 1f, 0f, 0.5f)); // yellow
        fire.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 2, 0));
        fire.setStartSize(1.5f);
        fire.setEndSize(0.1f);
        fire.setGravity(0, 0, 0);
        fire.setLowLife(1f);
        fire.setHighLife(3f);
        fire.getParticleInfluencer().setVelocityVariation(0.3f);
        
        carNode.attachChild(fire);
	}*/

	private PhysicsSpace getPhysicsSpace(){
        return bulletAppState.getPhysicsSpace();
    }
    
    private void setupKeys() {
        inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("Space", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Reset", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(this, "Lefts");
        inputManager.addListener(this, "Rights");
        inputManager.addListener(this, "Ups");
        inputManager.addListener(this, "Downs");
        inputManager.addListener(this, "Space");
        inputManager.addListener(this, "Reset");
    }

    private void buildPlayer() {
        Spatial carmodel = assetManager.loadModel("assets/car_v3.obj");
//        carmodel.setMaterial(mat); //so it actually uses the mtl file
        
        //create a compound shape and attach CollisionShape for the car body at 0,1,0
        //this shifts the effective center of mass of the BoxCollisionShape to 0,-1,0
        CompoundCollisionShape compoundShape = new CompoundCollisionShape();
        compoundShape.addChildShape(CollisionShapeFactory.createDynamicMeshShape(carmodel), new Vector3f(0, 0, 0));
        
        //create vehicle node
        carNode = new Node("vehicleNode");
        player = new VehicleControl(compoundShape, 200); //200 = mass
        carNode.addControl(player);
        carNode.attachChild(carmodel);

        //setting suspension values for wheels, this can be a bit tricky
        //see also https://docs.google.com/Doc?docid=0AXVUZ5xw6XpKZGNuZG56a3FfMzU0Z2NyZnF4Zmo&hl=en
        float stiffness = 40.0f;//200=f1 car
        float compValue = 0.3f; //(should be lower than damp)
        float dampValue = 0.4f;
        
        player.setSuspensionCompression(compValue * 2.0f * FastMath.sqrt(stiffness));
        player.setSuspensionDamping(dampValue * 2.0f * FastMath.sqrt(stiffness));
        player.setSuspensionStiffness(stiffness);
        player.setMaxSuspensionForce(10000.0f);
        player.setMaxSuspensionTravelCm(480);
        
        //Create four wheels and add them at their locations
        Vector3f wheelDirection = new Vector3f(0, -1, 0); // was 0, -1, 0
        Vector3f wheelAxle = new Vector3f(-1, 0, 0); // was -1, 0, 0
        float radius = 0.3f;
        float restLength = 0.5f;
        float yOff = 0.5f;
        float xOff = 0.72f;
        float zOff = 1.1f;

        Node node1 = new Node("wheel 1 node");
        Spatial wheels1 = assetManager.loadModel("assets/wheel.obj");
        node1.attachChild(wheels1);
        wheels1.setMaterial(mat);
        player.addWheel(node1, new Vector3f(-xOff, yOff, zOff),
                wheelDirection, wheelAxle, restLength, radius, true);

        Node node2 = new Node("wheel 2 node");
        Spatial wheels2 = assetManager.loadModel("assets/wheel.obj");
        wheels2.scale(-1,1,1);
        node2.attachChild(wheels2);
        wheels2.setMaterial(mat);
        player.addWheel(node2, new Vector3f(xOff, yOff, zOff),
                wheelDirection, wheelAxle, restLength, radius, true);

        Node node3 = new Node("wheel 3 node");
        Spatial wheels3 = assetManager.loadModel("assets/wheel.obj");
        node3.attachChild(wheels3);
        wheels3.setMaterial(mat);
        player.addWheel(node3, new Vector3f(-xOff-0.05f, yOff, -zOff),
                wheelDirection, wheelAxle, restLength, radius, false);

        Node node4 = new Node("wheel 4 node");
        Spatial wheels4 = assetManager.loadModel("assets/wheel.obj");
        wheels4.scale(-1,1,1);
        node4.attachChild(wheels4);
        wheels4.setMaterial(mat);
        player.addWheel(node4, new Vector3f(xOff+0.05f, yOff, -zOff),
                wheelDirection, wheelAxle, restLength, radius, false);


        player.setFrictionSlip(0, 0.9f);
        player.setFrictionSlip(1, 0.9f);
        player.setFrictionSlip(2, 0.7f);
        player.setFrictionSlip(3, 0.7f);
        
        //attaching all the things
        carNode.attachChild(node1);
        carNode.attachChild(node2);
        carNode.attachChild(node3);
        carNode.attachChild(node4);
        
        rootNode.attachChild(carNode);

        getPhysicsSpace().add(player);
        
    }
    
    private void initCamera() {
//     	disable the default flyCam (don't forget that!)
    	flyCam.setEnabled(false);
    	
    	
    	//if the camera ever seems really glitchy, set the fps to not be infinite
    	ChaseCamera chaseCam = new ChaseCamera(cam, carNode, inputManager);
    	chaseCam.setSmoothMotion(true);
    	chaseCam.setLookAtOffset(Vector3f.UNIT_Y.mult(2));
    	chaseCam.setChasingSensitivity(20f);
    	chaseCam.setTrailingSensitivity(20f);
    	chaseCam.setDefaultVerticalRotation(FastMath.PI/20);
    	chaseCam.setDefaultDistance(8);

    }

    @Override
    public void simpleUpdate(float tpf) {
    	
//    	Quaternion camWorld = camNode.getWorldRotation();
//    	camNode.rotate(0, 0, -camWorld.getZ()); //TODO test drive unlimited camera anyone?
    		//this should be changed so it doesn't feel so weird...
    		//it seems to be strange in a certain quadrant of motion?
    	
//        hudText.setText(Math.abs(player.getCurrentVehicleSpeedKmHour())+" km/h" + getWheelTractionInfo());
        hudText.setText(player.getLinearVelocity().length() + "m/s" + getWheelTractionInfo());    // the text
        
        
        /*Some thoughts i found for fixing the drifting
         * Extra yaw torques (or angular impulses) can be applied to improve turning and create artificial sliding behavior.
		 * Change the wheel friction depending on the current wheel velocity to create the perfect drifting behavior.
		 * http://www.digitalrune.com/Documentation/html/143af493-329d-408f-975d-e63625646f2f.htm
         */
        
        //my suggestion is to just add a normal (towards the apex) force to simulate drifting 
        	//maybe scaled by the actual speed?

        Vector3f forward = new Vector3f();
        player.getForwardVector(forward);
        
    	float speed = player.getLinearVelocity().length();
//    	System.out.println(speed);
//    	player.applyCentralForce(forward.negate().mult(speed*speed)); //TODO drag
    	
    	Vector3f velocityVector = player.getLinearVelocity().normalize();
    	
    	Matrix3f playerRot = new Matrix3f();
    	player.getInterpolatedPhysicsRotation(playerRot);
    	
    	Vector3f leftPlayer = playerRot.mult(Vector3f.UNIT_X);
    	Vector3f rightPlayer = playerRot.mult(Vector3f.UNIT_X.negate());
    	
    	float driftAngle = (velocityVector.angleBetween(forward));
    	float driftRightAngle = FastMath.RAD_TO_DEG*(velocityVector.angleBetween(rightPlayer));
    	float driftLeftAngle = FastMath.RAD_TO_DEG*(velocityVector.angleBetween(leftPlayer));
    	
//        Vector3f angleVel = player.getAngularVelocity();
//        System.out.println(angleVel);
//        player.setAngularVelocity(new Vector3f(angleVel.x, angleVel.y*0.98f, angleVel.z));
        
        Quaternion rotate = new Quaternion().fromAngleAxis(steeringValue, new Vector3f(0,1,0));
        
        float w0 = player.getWheel(0).getSkidInfo();
        float w1 = player.getWheel(1).getSkidInfo(); //1.0 = no slip, 0.0 = no friction
        float w2 = player.getWheel(2).getSkidInfo();
        float w3 = player.getWheel(3).getSkidInfo();
        
        
        float driftForce = 400*speed*driftAngle;//guessed number for drifting
        float steeringForce = 4*player.getMass(); //something to do with speed?
        
        Vector3f frontdir = new Vector3f(forward.x, forward.y, forward.z);
        Vector3f reardir = new Vector3f(forward.x, forward.y, forward.z);
        
        //front values
        if (w0 < 1 && w1 < 1) {
        	//r = d - n*2*(d.n)
//        	Vector3f dir = forward.subtract(frontdir.mult(2*forward.dot(frontdir)));
        	
        	if (steeringValue != 0)
        		frontdir = rotate.mult(frontdir);
        	
        	if (steeringValue > 0)
        		player.applyForce(new Vector3f(steeringForce*Math.min(1,speed/10),0,0), Vector3f.UNIT_Z);
        	
        	if (steeringValue < 0) //TODO change to balanced value
        		player.applyForce(new Vector3f(-steeringForce*Math.min(1,speed/10),0,0), Vector3f.UNIT_Z);

        	frontdir = frontdir.mult(driftForce*(1-w0));
        	
        	//turning, Note when turning left im closer to the rightPlayer vector
        	if (driftRightAngle < 90) {
        		player.applyCentralForce(leftPlayer.mult(driftForce));
        		//TODO fix so that you actually notice it (but not so that its distracting)
        	}

        	if (driftLeftAngle < 90) {
        		player.applyCentralForce(rightPlayer.mult(driftForce));
        	}
        }
        
        //rear values
        if (w2 < 1 && w3 < 1) { /*???*/}
        
        //drift fix for arcade physics
        if (w0 < 1 && w1 < 1 && w2 < 1 && w3 < 1) {
        	float offset = 0.05f;
        	
	        if (driftRightAngle < 90) {
	        	player.applyForce(new Vector3f(-2000*FastMath.sin(driftAngle+offset),0,0), Vector3f.UNIT_Z);
	        }
	        
	        if (driftLeftAngle < 90) {
	        	player.applyForce(new Vector3f(2000*FastMath.sin(driftAngle+offset),0,0), Vector3f.UNIT_Z);
	        }
        }
        
        
        
        if (frameCount % 10 == 0) {
			////////////////////////////////// Front Wheels
        	Vector3f left = new Vector3f(player.getWheel(0).getCollisionLocation());
			Vector3f right = new Vector3f(player.getWheel(1).getCollisionLocation());
			Vector3f frontAvg = new Vector3f((left.x+right.x)/2, (left.y+right.y)/2, (left.z+right.z)/2);
			
			
        	////////////////////////////////// Rear Wheels
        	left = new Vector3f(player.getWheel(2).getCollisionLocation());
        	right = new Vector3f(player.getWheel(3).getCollisionLocation());
        	Vector3f rearAvg = new Vector3f((left.x+right.x)/2, (left.y+right.y)/2, (left.z+right.z)/2);

        	
        	if (ifArrows) {
        		putShapeArrow(ColorRGBA.Blue, reardir.mult(1/driftForce), rearAvg);
        		putShapeArrow(ColorRGBA.Red, frontdir.mult(1/driftForce), frontAvg);
        		
				//////////////////////////////////Velocity
				putShapeArrow(ColorRGBA.Green, velocityVector, player.getPhysicsLocation());
				
				////////////////////////////////// Left and right
				putShapeArrow(ColorRGBA.Cyan, leftPlayer, player.getPhysicsLocation());
				putShapeArrow(ColorRGBA.Cyan, rightPlayer, player.getPhysicsLocation());
        	}
        	
        	
        	////////////////////////////////// Tyre Path
        	for (int i = 0; i < 4; i++) {
        		Vector3f v = new Vector3f();
            	player.getForwardVector(v);
            	v.normalize();
            	
            	Line line = new Line(new Vector3f(0, 0, 0), v); //set across so its sideways to the car
            	
            	Geometry g = new Geometry("coordinate axis", line);
            	g.setLocalTranslation(player.getWheel(i).getCollisionLocation());
            	
          	  	Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
          	  	mat.getAdditionalRenderState().setWireframe(true);
          	  	mat.setColor("Color", ColorRGBA.Black);
          	  	g.setMaterial(mat);
          	  	g.setShadowMode(ShadowMode.Off);
          	  	rootNode.attachChild(g);
          	  	
          	  	skidMarks.addLast(g);
          	  	if (skidMarks.size() > 100) {
          	  		Geometry out = skidMarks.pop();
          	  		rootNode.detachChild(out);
          	  	}
        	}
        	
        }
        
        frameCount++;
    }
    
    private void putShapeArrow(ColorRGBA color, Vector3f dir, Vector3f pos) {
    	Arrow arrow = new Arrow(dir);
    	arrow.setLineWidth(1); // make arrow thicker
    	putShape(arrow, color).setLocalTranslation(player.getPhysicsLocation());
    }
    
    private Geometry putShape(Mesh shape, ColorRGBA color){
    	  Geometry g = new Geometry("coordinate axis", shape);
    	  Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    	  mat.getAdditionalRenderState().setWireframe(true);
    	  mat.setColor("Color", color);
    	  g.setMaterial(mat);
    	  g.setShadowMode(ShadowMode.Off);
    	  rootNode.attachChild(g);
    	  return g;
    }

    public void onAction(String binding, boolean value, float tpf) {
        if (binding.equals("Lefts")) {
            if (value) {
                steeringValue += steeringAmount; //steering amount here, angle
            } else {
                steeringValue += -steeringAmount;
            }
            player.steer(steeringValue);
        } else if (binding.equals("Rights")) {
            if (value) {
                steeringValue += -steeringAmount;
            } else {
                steeringValue += steeringAmount;
            }
            player.steer(steeringValue);
        } else if (binding.equals("Ups")) {
            if (value) {
                accelerationValue += accelerationForce;
            } else {
                accelerationValue -= accelerationForce;
            }
            player.accelerate(accelerationValue);
        } else if (binding.equals("Downs")) {
            if (value) {
                player.brake(brakeForce);
            } else {
                player.brake(0f);
            }
        } else if (binding.equals("Space")) {
            if (value) {
                player.applyImpulse(jumpForce, Vector3f.ZERO);
            }
        } else if (binding.equals("Reset")) {
            if (value) {
                player.setPhysicsLocation(Vector3f.ZERO);
                player.setPhysicsRotation(new Matrix3f());
                player.setLinearVelocity(Vector3f.ZERO);
                player.setAngularVelocity(Vector3f.ZERO);
                player.resetSuspension();
            } else {
            }
        }
    }

    private void createWorld() {
        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White);
        sun.setDirection(new Vector3f(-.3f,-.6f,-.5f).normalizeLocal());
        rootNode.addLight(sun);

//        Geometry worldModel = (Geometry) assetManager.loadModel("assets/kspTemp.obj");
//        Geometry worldModel = (Geometry) assetManager.loadModel("assets/F3 ENVIRONMENT.obj");
//        Geometry worldModel = (Geometry) assetManager.loadModel("assets/duct.obj")
        Geometry worldModel = (Geometry) assetManager.loadModel("assets/world.obj");

//        worldModel.setMaterial(mat);
        
        worldModel.move(0, -5, 0);
//    	worldModel.rotate(-90*FastMath.DEG_TO_RAD, 0, 0);
//    	worldModel.scale(2f);
//    	worldModel.scale(20f);
    	
    	CollisionShape world = CollisionShapeFactory.createMeshShape(worldModel);
    
    	RigidBodyControl landscape = new RigidBodyControl(world, 0);
    	worldModel.addControl(landscape);
    	
        rootNode.attachChild(worldModel);
        rootNode.setShadowMode(ShadowMode.CastAndReceive);
        bulletAppState.getPhysicsSpace().add(landscape);
        bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0, -9.81f, 0));
        
        if (ifShadow) {
	        //Shadows and lights
	        dlsr = new DirectionalLightShadowRenderer(assetManager, 2048, 3);
	        dlsr.setLight(sun);
	        dlsr.setLambda(0.55f);
	        dlsr.setShadowIntensity(0.6f);        
	        dlsr.setEdgeFilteringMode(EdgeFilteringMode.Nearest);
	//        dlsr.displayFrustum();
	        viewPort.addProcessor(dlsr);
	
	        dlsf = new DirectionalLightShadowFilter(assetManager, 2048, 3);
	        dlsf.setLight(sun);
	        dlsf.setLambda(0.55f);
	        dlsf.setShadowIntensity(0.6f);        
	        dlsf.setEdgeFilteringMode(EdgeFilteringMode.Nearest);
	        dlsf.setEnabled(false);	
	
	        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
	        fpp.addFilter(dlsf);
	
	        viewPort.addProcessor(fpp);
        
//        fpp = new FilterPostProcessor(assetManager);
//        SSAOFilter ssaoFilter = new SSAOFilter(12.94f, 43.92f, 0.33f, 0.61f);
//        fpp.addFilter(ssaoFilter);
//        viewPort.addProcessor(fpp);

        }
    }
    
    private String getWheelTractionInfo() {
    	String out = "";
    	out = "\n" + player.getWheel(0).getSkidInfo() + "\n" +
        player.getWheel(1).getSkidInfo() + "\n" + 
        player.getWheel(2).getSkidInfo() + "\n" +
        player.getWheel(3).getSkidInfo();
    	return out;
    }
}
