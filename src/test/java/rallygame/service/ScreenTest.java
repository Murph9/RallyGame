package rallygame.service;

import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.style.ElementId;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import rallygame.service.Screen.HorizontalPos;
import rallygame.service.Screen.VerticalPos;

public class ScreenTest {

    private AppSettings settings;
    private Screen screen;
    private Panel panel;
    
    @BeforeEach
    public void settings() {
        settings = new AppSettings(true);
        screen = new Screen(settings);

        panel = new StupidPanel();
        panel.setPreferredSize(new Vector3f(100, 200, 0));
    }

    @Test
    public void bottomRight() {
        Vector3f pos = screen.get(HorizontalPos.Right, VerticalPos.Bottom);
        assertEquals(new Vector3f(settings.getWidth(), 0, 0), pos);
    }

    @Test
    public void topRight() {
        Vector3f pos = screen.get(HorizontalPos.Right, VerticalPos.Top);
        assertEquals(new Vector3f(settings.getWidth(), settings.getHeight(), 0), pos);
    }

    @Test
    public void center() {
        Vector3f pos = screen.get(HorizontalPos.Middle, VerticalPos.Middle);
        assertEquals(new Vector3f(settings.getWidth() / 2, settings.getHeight() / 2, 0), pos);
    }

    @Test
    public void topLeft() {
        Vector3f pos = screen.get(HorizontalPos.Left, VerticalPos.Top);
        assertEquals(new Vector3f(0, settings.getHeight(), 0), pos);
    }

    @Test
    public void centerMe() {
        
        screen.centerMe(panel);

        Vector3f size = panel.getPreferredSize();
        Vector3f expected = screen.get(HorizontalPos.Middle, VerticalPos.Middle);
        expected.x -= size.x / 2f;
        expected.y += size.y / 2f;
        assertEquals(expected, panel.getLocalTranslation());
    }

    @Test
    public void topCenterMe() {
        screen.topCenterMe(panel);

        Vector3f size = panel.getPreferredSize();
        Vector3f expected = new Vector3f(settings.getWidth() / 2 - size.x / 2f, settings.getHeight(), 0);
        assertEquals(expected, panel.getLocalTranslation());
    }

    @Test
    public void topRightMe() {
        screen.topRightMe(panel);

        Vector3f size = panel.getPreferredSize();
        Vector3f expected = new Vector3f(settings.getWidth() - size.x, settings.getHeight(), 0);
        assertEquals(expected, panel.getLocalTranslation());
    }

    @Test
    public void bottomRightMe() {
        screen.topRightMe(panel);

        Vector3f size = panel.getPreferredSize();
        Vector3f expected = new Vector3f(settings.getWidth() - size.x, settings.getHeight(), 0);
        assertEquals(expected, panel.getLocalTranslation());
    }

    @Test
    public void topLeftMe() {
        screen.topLeftMe(panel);

        Vector3f expected = new Vector3f(0, settings.getHeight(), 0);
        assertEquals(expected, panel.getLocalTranslation());
    }

    @Test
    public void rightMiddleMe() {
        Vector3f pos = screen.get(HorizontalPos.Right, VerticalPos.Middle, panel.getPreferredSize());

        Vector3f size = panel.getPreferredSize();
        Vector3f expected = new Vector3f(settings.getWidth() - size.x, settings.getHeight()/2 + size.y/2, 0);

        assertEquals(expected, pos);
    }

    @Test
    public void bottomCenterMe() {
        Vector3f pos = screen.get(HorizontalPos.Middle, VerticalPos.Bottom, panel.getPreferredSize());

        Vector3f size = panel.getPreferredSize();
        Vector3f expected = new Vector3f(settings.getWidth()/2 - size.x/2, size.y, 0);

        assertEquals(expected, pos);
    }


    /**
     * lemur Container requires a real app, this doesn't :)
     */
    private class StupidPanel extends Panel {
        private Vector3f preferredSize;

        public StupidPanel() {
            super(false, new ElementId("aaa"), null);
        }

        @Override
        public void setPreferredSize(Vector3f size) {
            preferredSize = size;
        }
        @Override
        public Vector3f getPreferredSize() {
            return preferredSize;
        }
    }
}
