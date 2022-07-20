package rallygame.service.ray;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;

public class SceneRaycaster extends BaseAppState {

    @Override
    protected void initialize(Application app) {}

    @Override
    protected void cleanup(Application app) {}

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}

    public CollisionResults castRay(Vector3f pos, Vector3f dir) {
        var results = new CollisionResults();
        Ray ray = new Ray(pos, dir.normalize());
        ((SimpleApplication)this.getApplication()).getRootNode().collideWith(ray, results);
        return results;
    }
}
