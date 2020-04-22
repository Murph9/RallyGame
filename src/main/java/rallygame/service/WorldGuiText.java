package rallygame.service;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bounding.BoundingSphere;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Camera.FrustumIntersect;
import com.jme3.scene.Node;

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
            guiRootNode.detachChild(text.text);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        Camera cam = getApplication().getCamera();
        for (WorldText text: textList) {
            if (cam.contains(new BoundingSphere(0.1f, text.worldPos)) != FrustumIntersect.Outside) {
                // calc center offset, so the text is centered about the point given (as this makes the most sense)
                float xTranslation = -text.text.getLineWidth() * text.text.getLocalScale().getX() / 2;
                float yTranslation = text.text.getLineHeight() * text.text.getLocalScale().getY() / 2;
                Vector3f centerOffset = new Vector3f(xTranslation, yTranslation, 0);
                
                guiRootNode.attachChild(text.text);
                text.text.setLocalTranslation(cam.getScreenCoordinates(text.worldPos).add(centerOffset));
            } else 
                guiRootNode.detachChild(text.text);
            
        }
    }

    @Override
    protected void onEnable() { }

    @Override
    protected void onDisable() { }

    public WorldText addTextAt(String text, Vector3f worldPos) {
        BitmapFont font = getApplication().getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        int fontSize = font.getCharSet().getRenderedSize();

        BitmapText guiText = new BitmapText(font);
        guiText.setSize(fontSize);
        guiText.setColor(ColorRGBA.White);
        guiText.setText(text);

        guiText.setLocalTranslation(getApplication().getCamera().getScreenCoordinates(worldPos));
        
        WorldText wt = new WorldText(guiText, worldPos);
        textList.add(wt);
        return wt;
    }

    public class WorldText {
        final BitmapText text;
        final Vector3f worldPos;
        public WorldText(BitmapText text, Vector3f worldPos) {
            this.text = text;
            this.worldPos = worldPos;
        }

        public void setWorldPos(Vector3f worldPos) {
            this.worldPos.set(worldPos);
        }

        public void setText(String text) {
            this.text.setText(text);
        }
    }
}