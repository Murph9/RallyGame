package rallygame.game;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;

public class BasicCamera extends BaseAppState {

    private final Camera c;

    private Vector3f nextPos;
    private Vector3f nextLookAt;
	
	public BasicCamera(String name, Camera c, Vector3f pos, Vector3f lookat) {
        this.c = c;
        this.nextPos = pos;
        this.nextLookAt = lookat;
	}

	public void updatePosition(Vector3f pos, Vector3f lookAt) {
        nextPos = pos;
		nextLookAt = lookAt;
	}

    @Override
	public void render(RenderManager rm) {
        if (this.nextPos != null) {
            this.c.setLocation(this.nextPos);
            this.nextPos = null;
        }

        if (this.nextLookAt != null) {
            this.c.lookAt(this.nextLookAt, Vector3f.UNIT_Y);
            this.nextLookAt = null;
        }
    }

	@Override
	protected void initialize(Application app) {}
	@Override
	protected void cleanup(Application app) {}
	@Override
	protected void onEnable() {}
	@Override
	protected void onDisable() {}
}
