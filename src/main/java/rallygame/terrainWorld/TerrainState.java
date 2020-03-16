package rallygame.terrainWorld;

import java.util.Deque;

public final class TerrainState
{
    private final Deque<TerrainChunk> newChunks;
    private final Deque<TerrainLocation> oldChunks;

    public TerrainState(Deque<TerrainChunk> newChunks, Deque<TerrainLocation> oldChunks)
    {
        this.newChunks = newChunks;
        this.oldChunks = oldChunks;
    }

    public Deque<TerrainChunk> getNewChunks() { return this.newChunks; }
    public Deque<TerrainLocation> getOldChunks() { return this.oldChunks; }
}