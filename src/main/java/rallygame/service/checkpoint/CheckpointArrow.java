package rallygame.service.checkpoint;

import java.util.function.Function;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

import rallygame.car.ray.RayCarControl;
import rallygame.helper.Geo;
import rallygame.service.Screen;
import rallygame.service.Screen.HorizontalPos;
import rallygame.service.Screen.VerticalPos;

public class CheckpointArrow extends BaseAppState {

    private final RayCarControl player;
    private final Function<RayCarControl, Vector3f> posFunc;

    private Node rootUINode;
    private Geometry arrow;

    public CheckpointArrow(RayCarControl player, Function<RayCarControl, Vector3f> posFunc) {
        this.player = player;
        this.posFunc = posFunc;
    }

    @Override
    protected void initialize(Application app) {
        rootUINode = new Node("Checkpoint arrow node");
        ((SimpleApplication)app).getGuiNode().attachChild(rootUINode);

        var location = new Screen(app.getContext().getSettings()).get(HorizontalPos.Middle, VerticalPos.Top).add(0, -100, 0);
        arrow = Geo.makeShapeArrow(app.getAssetManager(), ColorRGBA.White, Vector3f.UNIT_X.mult(80), location);
        rootUINode.attachChild(arrow);
    }

    @Override
    protected void cleanup(Application app) {
        rootUINode.removeFromParent();
        rootUINode = null;
    }

    @Override
    public void update(float tpf) {
        var checkPos = this.posFunc.apply(this.player);
        if (checkPos != null) {
            Vector3f targetDir = checkPos.subtract(this.player.location);
            targetDir.y = 0; // no caring about the vertical
            targetDir.normalizeLocal();
    
            float angF = this.player.forward.angleBetween(targetDir);
            float ang = this.player.left.normalize().angleBetween(targetDir);
            float nowTurn = angF * Math.signum(FastMath.HALF_PI - ang);
    
            this.arrow.setLocalScale(nowTurn < 0 ? 1 : -1, 1, 1);
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
