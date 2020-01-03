package world;


public interface IWorld {
    /** Reset the world */
    void reset();

    /** Get type of the world */
    WorldType getType();
}