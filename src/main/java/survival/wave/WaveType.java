package survival.wave;

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