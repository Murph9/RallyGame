package terrainWorld;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.terrain.geomipmap.TerrainLodControl;

import java.io.Closeable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;

//Copied from https://github.com/jayfella/TerrainWorld (and changed main world class to terrain, no file caching anymore)
//Which has the https://en.wikipedia.org/wiki/WTFPL license

public abstract class Terrain extends AbstractAppState implements Closeable
{
    protected final SimpleApplication app;
    private final PhysicsSpace physicsSpace;

    protected final int blockSize;
    protected final int tileSize;
    protected final int bitshift;
    private final int positionAdjuster;

    private int nViewDistance = 2, eViewDistance = 2, sViewDistance = 2, wViewDistance = 2;

    protected float worldHeight = 1f;

    private boolean isLoaded;
    private int totalVisibleChunks = 25;

    protected TileListener tileListener;

    private long cacheTime = 5000;

    protected final Map<TerrainLocation, TerrainChunk> worldTiles = new HashMap<TerrainLocation, TerrainChunk>();
    protected final Map<TerrainLocation, TerrainChunk> worldTilesCache = new ConcurrentHashMap<TerrainLocation, TerrainChunk>();

    ScheduledThreadPoolExecutor threadpool = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors());

    public Terrain(SimpleApplication app, PhysicsSpace physicsSpace, int tileSize, int blockSize)
    {
        this.app = app;
        this.physicsSpace = physicsSpace;

        this.tileSize = tileSize;
        this.blockSize = blockSize;

        this.bitshift = this.bitCalc(blockSize);
        this.positionAdjuster = (this.blockSize - 1) / 2;
    }

    private int bitCalc(int blockSize)
    {
        switch (blockSize)
        {
            case 17: return 4;
            case 33: return 5;
            case 65: return 6;
            case 129: return 7;
            case 257: return 8;
            case 513: return 9;
            case 1025: return 10;
        }

        throw new IllegalArgumentException("Invalid block size specified.");
    }

    public long getCacheTime() { return this.cacheTime; }

    /**
     * Set the time in which tiles are considered old enough to be
     * removed from the cache.
     *
     * @param time time in milliseconds. (1000L = 1 second).
     */
    public void setCacheTime(long time) { this.cacheTime = time; }


    /**
     * Set the view distance in tiles for each direction according
     * to the initial view direction Vector3f.UNIT_Z.
     *
     * @param n Tiles to load north of the initial view direction.
     * @param e Tiles to load east of the initial view direction.
     * @param s Tiles to load south of the initial view direction.
     * @param w Tiles to load west of the initial view direction.
     */
    public final void setViewDistance(int n, int e, int s, int w)
    {
        this.nViewDistance = n;
        this.eViewDistance = e;
        this.sViewDistance = s;
        this.wViewDistance = w;

        totalVisibleChunks = (wViewDistance + eViewDistance + 1) * (nViewDistance + sViewDistance + 1);
    }


    /**
     * Set the view distance in tiles for all directions.
     *
     * @param distance Tiles to load in all directions.
     */
    public final void setViewDistance(int distance)
    {
        // this.nViewDistance = this.eViewDistance = this.sViewDistance = this.wViewDistance = distance;
        setViewDistance(distance, distance, distance, distance);
    }


    /**
     *
     * @return notes whether or not this world has loaded all tiles required.
     */
    public final boolean isLoaded()
    {
        return this.isLoaded;
    }


    /**
     *
     * @return the amount of tiles that are currently loaded.
     */
    public final int getLoadedTileCount()
    {
        return this.worldTiles.size();
    }

    public final int getCachedTilesCount()
    {
        return this.worldTilesCache.size();
    }

    public final int getQuedGeneratingTilesCount()
    {
        return worldTilesQue.size();
    }

    public final int getQuedGeneratedTilesCount()
    {
        return newTiles.size();
    }

    /**
     *
     * @return the maximum height of this world.
     */
    public final float getWorldHeight()
    {
        return this.worldHeight;
    }

    /**
     * Set the maximum view height for this world.
     *
     * @param height The maximum height of this world.
     */
    public final void setWorldHeight(float height)
    {
        this.worldHeight = height;
    }

    public void setTileListener(TileListener listener)
    {
        this.tileListener = listener;
    }

    private boolean tileLoaded(TerrainChunk terrainChunk)
    {
        if (this.tileListener != null)
            return this.tileListener.tileLoaded(terrainChunk);

        return true;
    }
    public boolean tileUnloaded(TerrainChunk terrainChunk)
    {
        if (this.tileListener != null)
            return this.tileListener.tileUnloaded(terrainChunk);

        return true;
    }

    public void tileLoadedThreaded(TerrainChunk terrainChunk)
    {
        if (this.tileListener != null)
            this.tileListener.tileLoadedThreaded(terrainChunk);
    }

    private boolean checkForOldChunks()
    {
        Iterator<Map.Entry<TerrainLocation, TerrainChunk>> iterator = worldTiles.entrySet().iterator();

        while(iterator.hasNext())
        {
            Map.Entry<TerrainLocation, TerrainChunk> entry = iterator.next();
            TerrainLocation location = entry.getKey();

            if (location.getX() < topLx || location.getX() > botRx || location.getZ() < topLz || location.getZ() > botRz)
            {
                TerrainChunk chunk = entry.getValue();

                // throw the tile unloaded event
                // check if the tile unload has been cancelled
                if (!this.tileUnloaded(chunk))
                    return false;

                physicsSpace.remove(chunk);
                app.getRootNode().detachChild(chunk);

                // remove rigid objects...
                if (chunk.getStaticRigidObjectsNode() != null)
                {
                    physicsSpace.remove(chunk.getStaticRigidObjectsNode());
                    app.getRootNode().detachChild(chunk.getStaticRigidObjectsNode());
                }

                // remove non-rigid objects
                if (chunk.getStaticNonRigidObjectsNode() != null)
                {
                    app.getRootNode().detachChild(chunk.getStaticNonRigidObjectsNode());
                }

                iterator.remove();

                return true;
            }
        }

        return false;
    }

    private final Set<TerrainLocation> worldTilesQue = new HashSet<TerrainLocation>();
    private final ConcurrentLinkedQueue<PendingChunk> newTiles = new ConcurrentLinkedQueue<PendingChunk>();

    private boolean checkForNewChunks()
    {
        // tiles are always removed first to keep triangle count down, so we can
        // safely assume this is a reasonable comparative.
        if (worldTiles.size() == totalVisibleChunks)
        {
            isLoaded = true; // used to determine whether the player can join the world.
            return false;
        }

        // check if any requested tiles are ready to be added.
        PendingChunk pending = newTiles.poll();
        if (pending != null)
        {
            // throw the TileLoaded event & check if the tile load has been cancelled.
            if (!tileLoaded(pending.getChunk()))
                return false;

            TerrainLodControl lodControl = new TerrainLodControl(pending.getChunk(), app.getCamera());
            lodControl.setExecutor(threadpool);
            pending.getChunk().addControl(lodControl);

            pending.getChunk().setShadowMode(ShadowMode.Receive);
            worldTiles.put(pending.getLocation(), pending.getChunk());
            app.getRootNode().attachChild(pending.getChunk());
            physicsSpace.add(pending.getChunk());

            // add static rigid objects
            if (pending.getChunk().getStaticRigidObjectsNode() != null)
            {
                app.getRootNode().attachChild(pending.getChunk().getStaticRigidObjectsNode());
                physicsSpace.add(pending.getChunk().getStaticRigidObjectsNode());
            }

            // add static non-rigid objects
            if (pending.getChunk().getStaticNonRigidObjectsNode() != null)
            {
                app.getRootNode().attachChild(pending.getChunk().getStaticNonRigidObjectsNode());
            }

            return true;
        }
        else
        {
            for (int x = topLx; x <= botRx; x++)
            {
                for (int z = topLz; z <= botRz; z++)
                {
                    final TerrainLocation location = new TerrainLocation(x, z);

                    // check its already loaded.
                    if (worldTiles.get(location) != null)
                        continue;

                    // check if it's already in the que.
                    if (worldTilesQue.contains(location))
                        continue;

                    // check if its in the cache.
                    TerrainChunk chunk = worldTilesCache.get(location);
                    if (chunk != null)
                    {
                        // throw the TileLoaded event & check if the tile load has been cancelled.
                        if (!tileLoaded(chunk))
                            return false;

                        TerrainLodControl lodControl = new TerrainLodControl(chunk, app.getCamera());
                        lodControl.setExecutor(threadpool);
                        chunk.addControl(lodControl);

                        chunk.setShadowMode(ShadowMode.Receive);

                        app.getRootNode().attachChild(chunk);
                        physicsSpace.add(chunk);
                        worldTiles.put(location, chunk);

                        // updateNeighbours();

                        // add static rigid objects
                        if (chunk.getStaticRigidObjectsNode() != null)
                        {
                            app.getRootNode().attachChild(chunk.getStaticRigidObjectsNode());
                            physicsSpace.add(chunk.getStaticRigidObjectsNode());
                        }

                        // add static non-rigid objects
                        if (chunk.getStaticNonRigidObjectsNode() != null)
                        {
                            app.getRootNode().attachChild(chunk.getStaticNonRigidObjectsNode());
                        }


                        return true;
                    }
                    else
                    {
                        // its nowhere to be seen, generate it.
                        worldTilesQue.add(location);

                        threadpool.submit(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                TerrainChunk newChunk = getTerrainChunk(location);
                                PendingChunk pending = new PendingChunk(location, newChunk);

                                tileLoadedThreaded(newChunk);

                                newTiles.add(pending);

                                // thread safety...
                                app.enqueue(new Callable<Boolean>()
                                {
                                    public Boolean call()
                                    {
                                        worldTilesQue.remove(location);
                                        return true;
                                    }
                                });
                            }
                        });

                        return true;
                    }
                }
            }
        }

        return false;
    }

    private volatile boolean cacheInterrupted;
    private void recalculateCache()
    {
        worldTilesCache.clear();
        cacheInterrupted = false;

        Runnable cacheUpdater = new Runnable()
        {
            @Override
            public void run()
            {
                // top and bottom
                for (int x = (topLx -1); x <= (botRx + 1); x++)
                {
                    if (cacheInterrupted) return;

                    // top
                    final TerrainLocation topLocation = new TerrainLocation(x, topLz - 1);
                    final TerrainChunk topChunk = getTerrainChunk(topLocation);
                    topChunk.setShadowMode(ShadowMode.Receive);
                    tileLoadedThreaded(topChunk);

                    // bottom
                    final TerrainLocation bottomLocation = new TerrainLocation(x, botRz + 1);
                    final TerrainChunk bottomChunk = getTerrainChunk(bottomLocation);
                    bottomChunk.setShadowMode(ShadowMode.Receive);
                    tileLoadedThreaded(bottomChunk);

                    app.enqueue(new Callable<Boolean>()
                    {
                        public Boolean call()
                        {
                            worldTilesCache.put(topLocation, topChunk);
                            worldTilesCache.put(bottomLocation, bottomChunk);

                            return true;
                        }
                    });
                }

                // sides
                for (int z = topLz; z <= botRz; z++)
                {
                    if (cacheInterrupted) return;

                    // left
                    final TerrainLocation leftLocation = new TerrainLocation(topLx - 1, z);
                    final TerrainChunk leftChunk = getTerrainChunk(leftLocation);
                    leftChunk.setShadowMode(ShadowMode.Receive);
                    tileLoadedThreaded(leftChunk);

                    // right
                    final TerrainLocation rightLocation = new TerrainLocation(botRx + 1, z);
                    final TerrainChunk rightChunk = getTerrainChunk(rightLocation);
                    rightChunk.setShadowMode(ShadowMode.Receive);
                    tileLoadedThreaded(leftChunk);

                    app.enqueue(new Callable<Boolean>()
                    {
                        public Boolean call()
                        {
                            worldTilesCache.put(leftLocation, leftChunk);
                            worldTilesCache.put(rightLocation, rightChunk);

                            return true;
                        }
                    });
                }
            }
        };

        threadpool.execute(cacheUpdater);
    }

    private float actualX, actualZ;
    private int
            locX, locZ,
            oldLocX = Integer.MAX_VALUE, oldLocZ = Integer.MAX_VALUE,
            topLx, topLz, botRx, botRz;

    @Override
    public void update(float tpf)
    {
        actualX = (int)(app.getCamera().getLocation().getX() + positionAdjuster);
        actualZ = (int)(app.getCamera().getLocation().getZ() + positionAdjuster);

        locX = (int)actualX >> this.bitshift;
        locZ = (int)actualZ >> this.bitshift;

        // if the last recored locations are the same as the new locations
        // and all que's are empty, dont bother checking for new or old tiles
        if ((locX == oldLocX) && (locZ == oldLocZ) && worldTilesQue.isEmpty() && newTiles.isEmpty())
        {
            return;
        }

        topLx = locX - wViewDistance;
        topLz = locZ - nViewDistance;

        botRx = locX + eViewDistance;
        botRz = locZ + sViewDistance;

        if (checkForOldChunks())
            return;

        if (checkForNewChunks())
            return;

        if (worldTilesQue.isEmpty() && newTiles.isEmpty())
        {
            cacheInterrupted = true;
            recalculateCache();

            oldLocX = locX;
            oldLocZ = locZ;
        }
    }

    public abstract TerrainChunk getTerrainChunk(TerrainLocation location);

    /**
     *
     * @param location The X and Z location in which you need the height
     * @return the height of the location. Returns 0f if the tile is not loaded.
     */
    public final float getHeight(Vector3f location)
    {
        int tqLocX = (int)location.getX() >> this.bitshift;
        int tqLocZ = (int)location.getZ() >> this.bitshift;

        TerrainLocation tLoc = new TerrainLocation(tqLocX, tqLocZ);

        TerrainChunk tq = this.worldTiles.get(tLoc);

        if (tq == null)
            return 0f;

        float tqPosX = location.getX() - (tqLocX * this.blockSize);
        float tqPosZ = location.getZ() - (tqLocZ * this.blockSize);

        float height = tq.getHeightmapHeight(new Vector2f(tqPosX, tqPosZ));

        return height * this.worldHeight;
    }

    @Override
    public void close()
    {
        threadpool.shutdown();
    }
}
