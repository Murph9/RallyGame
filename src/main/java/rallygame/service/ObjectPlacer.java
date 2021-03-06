package rallygame.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import jme3tools.optimize.GeometryBatchFactory;
import rallygame.effects.LoadModelWrapper;

public class ObjectPlacer extends BaseAppState {

    public static int incObjNum = 0;
    public class ObjectId {
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
    public class NodeId extends ObjectId {}

    private final boolean usePhysics;
    private final Node rootNode;

    private final Map<ObjectId, Spatial> objects;
    private final Map<NodeId, Node> nodes;

    public ObjectPlacer(boolean usePhysics) {
        this.usePhysics = usePhysics;
        rootNode = new Node("object root");

        objects = new HashMap<>();
        nodes = new HashMap<>();
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
        sp.setLocalTranslation(location);
        sp.addControl(new RigidBodyControl(0));
        rootNode.attachChild(sp);
        if (usePhysics)
            getState(BulletAppState.class).getPhysicsSpace().add(sp);
        
        return id;
    }
    public void remove(ObjectId id) {
        if (id == null)
            return;
        Spatial sp = objects.remove(id);
        if (sp == null)
            return;

        sp.removeFromParent();
        if (usePhysics)
            getState(BulletAppState.class).getPhysicsSpace().remove(sp);
    }

    /** Add many as optimised node */
    public NodeId addBulk(List<Spatial> sp) {
        NodeId id = new NodeId();
        Node node = new Node();
        nodes.put(id, node);
        for (Spatial s : sp) {
            node.attachChild(s);
        }
        node = GeometryBatchFactory.optimize(node, true);
        node.addControl(new RigidBodyControl(0));
        rootNode.attachChild(node);
        if (usePhysics)
            getState(BulletAppState.class).getPhysicsSpace().add(node);

        return id;
    }
    public void removeBulk(NodeId id) {
        if (id == null)
            return;
        Node node = nodes.remove(id);
        if (node == null)
            return;
        if (usePhysics)
            getState(BulletAppState.class).getPhysicsSpace().remove(node);
        node.removeFromParent();
    }
}
