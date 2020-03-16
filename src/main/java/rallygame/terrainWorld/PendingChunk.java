package rallygame.terrainWorld;

public class PendingChunk
{
    private final TerrainLocation location;
    private final TerrainChunk chunk;

    public PendingChunk(TerrainLocation location, TerrainChunk chunk)
    {
        this.location = location;
        this.chunk = chunk;
    }

    public TerrainLocation getLocation() { return this.location; }
    public TerrainChunk getChunk() { return this.chunk; }

}
