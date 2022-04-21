package rallygame.world.wp;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;

import rallygame.world.ICheckpointWorld;
import rallygame.world.WorldType;

public class StaticBuilt extends DefaultBuilder implements ICheckpointWorld {

    private List<Vector3f> placedPieces = new LinkedList<>();

    public StaticBuilt() {
        super(Floating.values());
    }

    @Override
    public void initialize(Application app) {
        super.initialize(app);

        var trackLayout = new WP[] {
            Floating.STRAIGHT,
            Floating.STRAIGHT,
            Floating.STRAIGHT,
            Floating.RIGHT_CURVE,
            Floating.RIGHT_CURVE,
            Floating.STRAIGHT,
            Floating.STRAIGHT,
            Floating.RIGHT_CURVE,
            Floating.RIGHT_CURVE,
            Floating.STRAIGHT,
            Floating.STRAIGHT,
            Floating.STRAIGHT,
            Floating.STRAIGHT,
            Floating.RIGHT_CURVE,
            Floating.RIGHT_CURVE,
            Floating.STRAIGHT,
            Floating.STRAIGHT,
            Floating.RIGHT_CURVE,
            Floating.RIGHT_CURVE,
            Floating.STRAIGHT,
        };
        for (var wp : trackLayout) {
            var piece = this.wpos.stream().filter(x -> x.wp == wp).findFirst();
            placePiece(piece.get());
        }
    }

    @Override
    public void update(float tpf) {
        // no
    }

    // TODO reset can break everything

    @Override
    public WorldType getType() {
        return WorldType.FIXEDDYNAMIC;
    }

    @Override
    public DefaultBuilder copy() {
        return new StaticBuilt();
    }

    @Override
    public Transform start(int i) {
        return null;
    }

    @Override
    public Vector3f[] checkpoints() {
        return this.placedPieces.toArray(new Vector3f[this.placedPieces.size()]);
    }

    @Override
    protected Vector3f placePiece(WPObject wpo) {
        var position = super.placePiece(wpo);
        this.placedPieces.add(position);
        return position;
    }
}
