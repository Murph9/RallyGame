package world;

import world.highway.HighwayWorld;
import world.lsystem.LSystemWorld;
import world.osm.OsmWorld;
import world.wp.WP.DynamicType;

public enum WorldType {
	DYNAMIC,
	STATIC,
	
	OBJECT,
	FULLCITY,
	
	TRACK,
	LSYSTEM,
	HIGHWAY,
	OSM,

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
        case FULLCITY:
            return new FullCityWorld();
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

        default:
            throw new IllegalArgumentException("Non valid world type given: " + worldTypeStr);
        }
    }
}
