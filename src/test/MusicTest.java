package test;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioData.DataType;
import com.jme3.scene.Node;

class MusicTestApp extends SimpleApplication {

    public MusicTestApp() {
        super(new MusicTest());
    }

    public static void main(String[] args) {
        MusicTestApp app = new MusicTestApp();
        app.setDisplayStatView(true);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        inputManager.setCursorVisible(true);
        getStateManager().attach(new MusicTest());
    }
}

public class MusicTest extends BaseAppState {

    private AudioNode[] backgroundMusic;
    private final Node rootNode = new Node("Sound rootNode");

    @Override
    protected void initialize(Application app) {
        backgroundMusic = new AudioNode[2];

        // add looping background sound
        backgroundMusic[0] = new AudioNode(app.getAssetManager(), "assets/sounds/music/duel music-04_background.ogg", DataType.Buffer);
        backgroundMusic[0].setLooping(true);
        backgroundMusic[0].setPositional(false);
        rootNode.attachChild(backgroundMusic[0]);

        // add looping background sound
        backgroundMusic[1] = new AudioNode(app.getAssetManager(), "assets/sounds/music/duel music-04_main.ogg", DataType.Buffer);
        backgroundMusic[1].setLooping(true);
        backgroundMusic[1].setPositional(false);
        rootNode.attachChild(backgroundMusic[1]);
    }

    @Override
    protected void cleanup(Application app) {
    }

    @Override
    protected void onEnable() {
        for (AudioNode node: backgroundMusic)
            node.play();
    }
    
    @Override
    protected void onDisable() {
        for (AudioNode node : backgroundMusic)
            node.pause();
    }

}
