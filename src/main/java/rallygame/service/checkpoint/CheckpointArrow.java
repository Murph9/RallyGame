package rallygame.service.checkpoint;

import java.util.function.Function;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

import rallygame.car.ray.RayCarControl;
import rallygame.helper.Geo;

public class CheckpointArrow extends BaseAppState {

    private final RayCarControl player;
    private final Function<RayCarControl, Vector3f> posFunc;

    private Node rootNode;
    private Geometry arrow;

    public CheckpointArrow(RayCarControl player, Function<RayCarControl, Vector3f> posFunc) {
        this.player = player;
        this.posFunc = posFunc;
    }

    @Override
    protected void initialize(Application app) {
        rootNode = new Node("Checkpoint arrow node");
        ((SimpleApplication)app).getRootNode().attachChild(rootNode);

        arrow = Geo.makeShapeArrow(app.getAssetManager(), ColorRGBA.White, Vector3f.UNIT_Z.mult(3), new Vector3f());
        arrow.getMaterial().getAdditionalRenderState().setLineWidth(10);
        rootNode.attachChild(arrow);
    }

    @Override
    protected void cleanup(Application app) {
        rootNode.removeFromParent();
        rootNode = null;
    }

    @Override
    public void update(float tpf) {
        var checkPos = this.posFunc.apply(this.player);
        if (checkPos != null) {
            checkPos = checkPos.clone();
            checkPos.y = this.player.location.y + 4;
            this.arrow.lookAt(checkPos, Vector3f.UNIT_Y);
            
            this.arrow.setLocalTranslation(this.player.location.add(0, 4, 0));
        }
        
        super.update(tpf);
    }

    @Override
    protected void onEnable() {
    }
    @Override
    protected void onDisable() {
    }
    
}
