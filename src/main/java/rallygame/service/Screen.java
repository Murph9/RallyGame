package rallygame.service;

import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.Panel;

public class Screen {

    private final int width;
    private final int height;

    public enum VerticalPos {
        Middle, Top, Bottom;

        public float calcPos(int height) {
            return calcPos(height, 0);
        }

        public float calcPos(int height, float size) {
            switch (this) {
                case Top:
                    return height;
                case Middle:
                    return height / 2 + size / 2;
                case Bottom :
                    return size;
                default:
                    throw new IllegalArgumentException("Unknown vertical pos: " + this);
            }
        }
    }

    public enum HorizontalPos {
        Middle, Left, Right;

        public float calcPos(int width) {
            return calcPos(width, 0);
        }

        public float calcPos(int width, float size) {
            switch (this) {
                case Right:
                    return width - size;
                case Middle:
                    return width / 2 - size / 2;
                case Left:
                    return 0;
                default:
                    throw new IllegalArgumentException("Unknown horizontal pos: " + this);
            }
        }
    }

    public Screen(AppSettings set) {
        this.height = set.getHeight();
        this.width = set.getWidth();
    }

    public Vector3f get(HorizontalPos horiPos, VerticalPos vertPos) {
        return get(horiPos, vertPos, new Vector3f());
    }

    public Vector3f get(HorizontalPos horiPos, VerticalPos vertPos, Vector3f size) {
        return new Vector3f(horiPos.calcPos(width, size.x), vertPos.calcPos(height, size.y), 0);
    }

    public void centerMe(Panel c) {
        Vector3f middle2 = get(HorizontalPos.Middle, VerticalPos.Middle, c.getPreferredSize());
        c.setLocalTranslation(middle2);
    }

    public void topCenterMe(Panel c) {
        Vector3f pos = get(HorizontalPos.Middle, VerticalPos.Top, c.getPreferredSize());
        c.setLocalTranslation(pos);
    }

    public void topRightMe(Panel c) {
        Vector3f pos = get(HorizontalPos.Right, VerticalPos.Top, c.getPreferredSize());
        c.setLocalTranslation(pos);
    }

    public void topLeftMe(Panel c) {
        Vector3f pos = get(HorizontalPos.Left, VerticalPos.Top, c.getPreferredSize());
        c.setLocalTranslation(pos);
    }

    public void bottomCenterMe(Panel c) {
        Vector3f pos = get(HorizontalPos.Middle, VerticalPos.Bottom, c.getPreferredSize());
        c.setLocalTranslation(pos);
    }

    /**Places the Panel at position, with the point at the center.*/
    public void centeredAt(Panel c, Vector3f pos) {
        Vector3f panelSize = c.getPreferredSize();
        c.setLocalTranslation(new Vector3f(pos.x - panelSize.x/2, pos.y + panelSize.y/2, pos.z));
    }
}
