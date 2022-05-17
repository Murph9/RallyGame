package survival.wave;

// TODO all block collisions need to take health off
enum WaveType {
    SingleFollow(WaveBuilder::generateSingleFollow),
    Line(WaveBuilder::generateLine),
    Fast(WaveBuilder::generateFast),
    Explode(WaveBuilder::generateExplode),
    ;

    public final IWaveGenerator func;
    WaveType(IWaveGenerator func) {
        this.func = func;
    }

    public IWaveGenerator getGenerator() {
        return func;
    }
}