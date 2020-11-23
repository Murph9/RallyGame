package rallygame.service;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bounding.BoundingSphere;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Camera.FrustumIntersect;
import com.jme3.scene.Node;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

public class WorldGuiText extends BaseAppState {

    private Node guiRootNode;
    private final List<WorldText> textList = new LinkedList<>();

    @Override
    protected void initialize(Application app) {
        guiRootNode = ((SimpleApplication)app).getGuiNode();
    }

    @Override
    protected void cleanup(Application app) {
        for (WorldText text: textList)
            guiRootNode.detachChild(text.con);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        Screen screen = new Screen(getApplication().getContext().getSettings());

        Camera cam = getApplication().getCamera();
        for (WorldText text: textList) {
            BoundingSphere bv = new BoundingSphere(0.1f, text.worldPos);
            int planeState = cam.getPlaneState(); // this code block follows the javadoc for Camera.contains(bv)
            cam.setPlaneState(0);
            if (cam.contains(bv) != FrustumIntersect.Outside) {
                guiRootNode.attachChild(text.con);
                screen.centeredAt(text.con, cam.getScreenCoordinates(text.worldPos));
            } else {
                guiRootNode.detachChild(text.con);
            }
            cam.setPlaneState(planeState);
        }
    }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }

    public WorldText addTextAt(String text, Vector3f worldPos) {
        Container con = new Container();
        Label l = con.addChild(new Label(text));
        
        con.setLocalTranslation(getApplication().getCamera().getScreenCoordinates(worldPos));
        
        WorldText wt = new WorldText(con, l, worldPos);
        textList.add(wt);
        return wt;
    }

    public void removeWorldText(WorldText text) {
        if (textList.contains(text)) {
            textList.remove(text);
            guiRootNode.detachChild(text.con);
        }
    }

    public class WorldText {
        final Container con;
        final Label label;
        final Vector3f worldPos;
        public WorldText(Container con, Label label, Vector3f worldPos) {
            this.con = con;
            this.label = label;
            this.worldPos = worldPos;
        }

        public void setWorldPos(Vector3f worldPos) {
            this.worldPos.set(worldPos);
        }

        public void setText(String text) {
            this.label.setText(text);
        }
    }
}