package service;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import helper.Log;

public class ConstantChecker extends BaseAppState {

    private final Vector3f unitX;
    private final Vector3f unitY;
    private final Vector3f unitZ;
    private final Vector3f unitXYZ;
    private final Vector3f zero;

    private final Quaternion qzero;
    private final Quaternion identity;

    public ConstantChecker() {
        unitX = Vector3f.UNIT_X.clone();
        unitY = Vector3f.UNIT_Y.clone();
        unitZ = Vector3f.UNIT_Z.clone();
        unitXYZ = Vector3f.UNIT_XYZ.clone();
        zero = Vector3f.ZERO.clone();

        qzero = Quaternion.ZERO.clone();
        identity = Quaternion.IDENTITY.clone();
    }

    @Override
    protected void initialize(Application app) {
    }

    @Override
    protected void cleanup(Application app) {
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }


    @Override
    public void update(float tpf) {
        super.update(tpf);
        // NOTE: please measure the performance

        if (!isEnabled())
            return;
        
        boolean error = false;
        // prevent any really dumb stuff with Vector3f
        if (!Vector3f.ZERO.equals(zero)) {
            Log.e("Vector3f.ZERO is not zero");
            error = true;
        }
        if (!Vector3f.UNIT_X.equals(unitX)) {
            Log.e("Vector3f.UNIT_X modified");
            error = true;
        }
        if (!Vector3f.UNIT_Y.equals(unitY)) {
            Log.e("Vector3f.UNIT_Y modified");
            error = true;
        }
        if (!Vector3f.UNIT_Z.equals(unitZ)) {
            Log.e("Vector3f.UNIT_Z modified");
            error = true;
        }
        if (!Vector3f.UNIT_XYZ.equals(unitXYZ)) {
            Log.e("Vector3f.UNIT_XYZ modified");
            error = true;
        }

        // and dumb stuff with quaternion
        if (!Quaternion.ZERO.equals(qzero)) {
            Log.e("Quaternion.ZERO modified");
            error = true;
        }
        if (!Quaternion.IDENTITY.equals(identity)) {
            Log.e("Quaternion.IDENTITY modified");
            error = true;
        }

        if (error) {
            Log.exit(342, "ConstantChecker error, see above");
        }
    }
}