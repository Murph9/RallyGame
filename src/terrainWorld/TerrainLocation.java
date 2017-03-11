package terrainWorld;

public final class TerrainLocation
{
    private final int x, z;

    public TerrainLocation(int x, int z)
    {
        this.x = x;
        this.z = z;
    }

    public TerrainLocation(float x, float z)
    {
        this.x = (int)x;
        this.z = (int)z;
    }

    public int getX() { return this.x; }
    public int getZ() { return this.z; }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof TerrainLocation)) { return false; }
        if (this == obj) return true;

        TerrainLocation comp = (TerrainLocation)obj;

        if (Integer.compare(x,comp.x) != 0) return false;
        if (Integer.compare(z,comp.z) != 0) return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        // prime numbers are faster!
        int hash = 7;
        hash = 97 * hash + this.x;
        hash = 97 * hash + this.z;
        return hash;
    }

    @Override
    public String toString()
    {
        return x + ", " + z;
    }
}