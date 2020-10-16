package rallygame.world;

import rallygame.world.highway.HighwayWorld;
import rallygame.world.lsystem.LSystemWorld;
import rallygame.world.osm.OsmWorld;
import rallygame.world.path.PathWorld;
import rallygame.world.track.TrackWorld;
import rallygame.world.wp.WP.DynamicType;

public enum WorldType {
	DYNAMIC,
	STATIC,
	
	OBJECT,
	
	TRACK,
	LSYSTEM,
	HIGHWAY,
    OSM,
    SMALLTILED,
    
    PATH,

	FLAT,
	MOVING,
	NONE
    ;
    
    WorldType() {

    }

    public static IWorld getWorld(String worldTypeStr, String subType) {
        WorldType worldType = WorldType.valueOf(WorldType.class, worldTypeStr);
        switch (worldType) {
        case STATIC:
            StaticWorld sworld = StaticWorld.valueOf(StaticWorld.class, subType);
            return new StaticWorldBuilder(sworld);
        case DYNAMIC:
            DynamicType dworld = DynamicType.valueOf(DynamicType.class, subType);
            return dworld.getBuilder();
        case OBJECT:
            return new ObjectWorld();
        case LSYSTEM:
            return new LSystemWorld();
        case HIGHWAY:
            return new HighwayWorld();
        case FLAT:
            return new FlatWorld();
        case MOVING:
            return new MovingWorld();
        case OSM:
            return new OsmWorld();
        case TRACK:
            return new TrackWorld();
        case SMALLTILED:
            return new SmallTiled();
        case PATH:
            return new PathWorld();

        default:
            throw new IllegalArgumentException("Non valid world type given: " + worldTypeStr);
        }
    }
}
