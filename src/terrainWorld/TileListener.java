package terrainWorld;

public interface TileListener
{

    /**
     * This event is fired when a TerrainChunk has been constructed
     * and ready to add to the scene. This event occurs BEFORE the
     * TerrainChunk has been added.
     *
     * @param TerrainChunk
     * @return false to cancel the tile from loading, else return
     * true to allow the tile to load.
     *
     */
    boolean tileLoaded(TerrainChunk terrainChunk);

    /**
     * This event is fired when a TerrainChunk has been flagged for removal
     * from the scene. This event occurs BEFORE the TerrainChunk has
     * been removed.
     *
     * @param TerrainChunk
     * @return false to cancel the tile from unloading, else return
     * true to allow the tile to unload.
     */
    boolean tileUnloaded(TerrainChunk terrainChunk);

    /**
     * This event is fired when the tile has been loaded from the threadpool
     * and allows you to modify the TerrainChunk from a threaded state
     * rather than on the GL thread.
     *
     * @param terrainChunk
     */
    void tileLoadedThreaded(TerrainChunk terrainChunk);

    /**
     * This event is fired when a heightmap image is required for
     * a TerrainChunk. This event is only fired when using an
     * ImageBasedWorld
     * @param x The X co-ordinate of the TerrainChunk
     * @param z The Z co-ordinate of the TerrainChunk
     * @return The path of the image file, including the file extension
     * For example: "/Textures/heightmaps/hmap_" + x + "_" + z + ".png"
     */
    String imageHeightmapRequired(int x, int z);
}
