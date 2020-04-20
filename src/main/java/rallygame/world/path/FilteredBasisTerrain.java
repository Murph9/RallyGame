package rallygame.world.path;

import java.nio.FloatBuffer;

import com.jme3.math.Vector2f;
import com.jme3.terrain.noise.ShaderUtils;
import com.jme3.terrain.noise.basis.FilteredBasis;
import com.jme3.terrain.noise.filter.IterativeFilter;
import com.jme3.terrain.noise.filter.OptimizedErode;
import com.jme3.terrain.noise.filter.PerturbFilter;
import com.jme3.terrain.noise.filter.SmoothFilter;
import com.jme3.terrain.noise.fractal.FractalSum;
import com.jme3.terrain.noise.modulator.NoiseModulator;

public class FilteredBasisTerrain {

    private final FilteredBasis basis;
    private final Vector2f fixedOffset;

    private FilteredBasisTerrain(FilteredBasis basis, Vector2f offset) {
        this.basis = basis;
        this.fixedOffset = offset;
    }

    public static FilteredBasisTerrain generate(Vector2f offset) {
        FractalSum base = new FractalSum();
        base.addModulator(new NoiseModulator() {
            @Override
            public float value(float... in) {
                return ShaderUtils.clamp(in[0] * 0.5f + 0.5f, 0, 1);
            }
        });
        FilteredBasis ground = new FilteredBasis(base);
        PerturbFilter perturb = new PerturbFilter();
        perturb.setMagnitude(0.119f);

        OptimizedErode therm = new OptimizedErode();
        therm.setRadius(10);
        therm.setTalus(0.011f);

        SmoothFilter smooth = new SmoothFilter();
        smooth.setRadius(1);
        smooth.setEffect(0.7f);

        IterativeFilter iterate = new IterativeFilter();
        iterate.addPreFilter(perturb);
        iterate.addPostFilter(smooth);
        iterate.setFilter(therm);
        iterate.setIterations(1); // higher numbers make it really smooth

        ground.addPreFilter(iterate);

        return new FilteredBasisTerrain(ground, offset);
    }

    public FloatBuffer getBuffer(int sideLength, Vector2f offset) {
        Vector2f off = fixedOffset.add(offset);
        return basis.getBuffer(off.x * (sideLength - 1), off.y * (sideLength - 1), 0, sideLength);
    }
}