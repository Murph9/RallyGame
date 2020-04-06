package rallygame.service;

import java.util.HashMap;
import java.util.Map;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import rallygame.effects.LoadModelWrapper;

public class ObjectPlacer extends BaseAppState {

    public static int incObjNum = 0;
    class ObjectId {
        private final int value;

        ObjectId() {
            this.value = incObjNum++;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ObjectId) {
                ObjectId id = (ObjectId)obj;
                return id.value == this.value;
            }
            return false;
        }
        @Override
        public int hashCode() {
            return value;
        }
    }

    private final Node rootNode;
    private final Map<ObjectId, Spatial> objects;

    public ObjectPlacer() {
        rootNode = new Node("object root");
        objects = new HashMap<>();
    }

    @Override
    protected void initialize(Application app) {
        ((SimpleApplication)app).getRootNode().attachChild(rootNode);
    }

    @Override
    protected void cleanup(Application app) {
        ((SimpleApplication) app).getRootNode().detachChild(rootNode);
        for (Spatial sp: objects.values())
            sp.removeFromParent();
        objects.clear();
    }

    @Override
    protected void onEnable() { }
    @Override
    protected void onDisable() { }

    public ObjectId add(String modelName, Vector3f location) {
        Spatial sp = LoadModelWrapper.create(getApplication().getAssetManager(), modelName);
        return add(sp, location);
    }
    public ObjectId add(Spatial sp, Vector3f location) {
        ObjectId id = new ObjectId();
        objects.put(id, sp);
        sp.setLocalTranslation(location); //TODO remove from this class?

        rootNode.attachChild(sp);
        return id;
    }
    public void remove(ObjectId id) {
        if (id == null)
            return;
        Spatial sp = objects.remove(id);
        if (sp == null)
            return;
        sp.removeFromParent();
    }
}
