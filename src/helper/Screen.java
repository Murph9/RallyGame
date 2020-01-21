package helper;

import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.Panel;

public class Screen {

    private AppSettings set;
    public Screen(AppSettings set) {
        this.set = set;
    }

    public Vector3f topLeft() {
        return new Vector3f(0, set.getHeight(), 0);
    }

    public Vector3f topRight() {
        return new Vector3f(set.getWidth(), set.getHeight(), 0);
    }

    public Vector3f bottomRight() {
        return new Vector3f(set.getWidth(), 0, 0);
    }

    public Vector3f center() {
        return new Vector3f(set.getWidth() / 2, set.getHeight() / 2, 0);
    }

    public Vector3f getCenterFor(Vector3f size) {
        Vector3f middle = center();
        middle.x -= size.x / 2f;
        middle.y += size.y / 2f;
        return middle;
    }
    public void centerMe(Panel c) {
        Vector3f size = c.getPreferredSize();
        c.setLocalTranslation(getCenterFor(size));
    }

    public Vector3f getTopCenterFor(Vector3f size) {
        return new Vector3f(set.getWidth() / 2 - size.x / 2f, set.getHeight(), 0);
    }
    public void topCenterMe(Panel c) {
        Vector3f size = c.getPreferredSize();
        c.setLocalTranslation(getTopCenterFor(size));
    }

    public Vector3f getTopRightFor(Vector3f size) {
        return new Vector3f(set.getWidth() - size.x, set.getHeight(), 0);
    }
    public void topRightMe(Panel c) {
        Vector3f size = c.getPreferredSize();
        c.setLocalTranslation(getTopRightFor(size));
    }
}