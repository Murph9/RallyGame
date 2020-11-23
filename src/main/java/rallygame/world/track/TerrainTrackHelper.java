package rallygame.world.track;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

import rallygame.helper.Log;

public class TerrainTrackHelper {

	//takes in a terrain float array and roads adds roads to it and returns the modified terrain
	//all 'height' values should be between 0 and 1 as this isn't scaled
	
	private final float[] heightMap;
	private final int size;
	private final float maxSlope;
	
	protected TerrainTrackHelper(float[] terrainHeightMap, int size, float maxSlope) {
		this.heightMap = terrainHeightMap;
		this.size = size;
		this.maxSlope = maxSlope;
		
		//https://stackoverflow.com/a/600306/9353639
		try {
			if (((size & (size - 1)) != 0))
				throw new Exception("Size is not a power of 2");
			if ((size + 1)*(size + 1) != terrainHeightMap.length)
				throw new Exception("Size: '"+size+" -> "+(size + 1)*(size + 1)+"' doesn't match the height map length: " + terrainHeightMap.length);
			if (maxSlope > 1 || maxSlope < 0)
	            throw new Exception("Max slope'" + maxSlope + "' must be a between 0 and 1.");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Log.p("terrainHeightMap init, size: " + terrainHeightMap.length);
	}
	
	/**Get the new height map, warning could be expensive do not call often.*/
	public float[] getHeightMap()
    {
		return heightMap.clone();
    }

    public float getHeight(Vector3f pos)
    {
        return getHeight(new Point(0,0).from(pos));
    }
    public float getHeight(Point pos)
    {
        return getHeight(indexOf(pos));
    }
    private float getHeight(int index)
    {
        if (index < 0 || index >= heightMap.length)
            return Float.NaN;
        return heightMap[index];
    }

    //height must be between 0 and 1
    public void setHeight(Point pos, float scaledHeight) throws Exception
    {
        if (scaledHeight < 0 || scaledHeight > 1)
            throw new Exception("scaledHeight was out of range [0,1]: " + scaledHeight);

        int index = indexOf(pos);

        if (index == -1)
            throw new Exception("pos was out of the terrain: " + pos);

        heightMap[index] = scaledHeight;

        //do a very slow slope setting on neighbours using maxSlope
        Queue<Integer> queue = new LinkedList<Integer>();
        queue.add(index);
        
        adjustAdjacentHeights(queue);
    }
    public void setHeights(List<Vector3f> posList) throws Exception {
    	//will ignore any invalid entries unlike the other setHeight method
    	
        Queue<Integer> queue = new LinkedList<Integer>();
        for (int i = 0; i < posList.size(); i++) {
        	int index = indexOf(new Point(0,0).from(posList.get(i)));
        	float scaledHeight = posList.get(i).y;
        	
        	if (index < 0 || index > heightMap.length - 1) {
        		Log.e("Point '"+posList.get(i)+" ("+i+")' not in set" + posList.get(i));
        		continue;
        	}
                
        	if (scaledHeight < 0 || scaledHeight > 1) {
        		Log.e("Scaled height '"+scaledHeight+" ("+i+")' not in [0,1]: " + posList.get(i));
        		continue;
        	}
        	
            heightMap[index] = scaledHeight;
            queue.add(index);
        }

        adjustAdjacentHeights(queue);
    }
    
    private void adjustAdjacentHeights(Queue<Integer> queue) {
    	//basically a breadth first not quite search, to find all nodes this could have touched
    	//that are more heightly different that the given maxSlope 
    	HashSet<Integer> visited = new HashSet<Integer>();
    	
    	while (!queue.isEmpty())
        {
            int cur = queue.poll();
            for (int i: getValidNeighboursIndex(cur, getHeight(cur)))
            {
                if (!visited.contains(i))
                {
                    float diffDirection = FastMath.sign(getHeight(cur) - getHeight(i));
                    heightMap[i] = getHeight(cur) - diffDirection * this.maxSlope;

                    queue.add(i);
                    visited.add(i);
                }
            }
        }
    }
    
    
    private int[] getValidNeighboursIndex(int index, float curHeight)
    {
    	List<Integer> neighbours = new LinkedList<>();
        Point pos = posOf(index);
        if (isInside(pos.x + 0, pos.z + 1))
            neighbours.add(indexOf(pos.add(0, 1)));
        if (isInside(pos.x + 0, pos.z - 1))
            neighbours.add(indexOf(pos.add(0, -1)));

        if (isInside(pos.x + 1, pos.z + 0))
            neighbours.add(indexOf(pos.add(1, 0)));
        if (isInside(pos.x - 1, pos.z + 0))
            neighbours.add(indexOf(pos.add(-1, 0)));

        return neighbours
        		.stream()
        		.filter(x -> isInside(posOf(index).x, posOf(index).z) && isDiffEnough(curHeight, x))
        		.mapToInt(x -> x)
        		.toArray();
    }

    private boolean isDiffEnough(float curHeight, int newIndex)
    {
        return Math.abs(curHeight - getHeight(posOf(newIndex))) > this.maxSlope;
    }

    private int indexOf(Point pos)
    {
        int x = (int)(pos.x + size / 2f);
        int z = (int)(pos.z + size / 2f);
        
        return z * (size + 1) + x;
    }

    private Point posOf(int index)
    {
        if (index < 0 || index > heightMap.length)
            return null;

        int x = -size / 2 + (index % (size + 1));
        int z = (index) / (size + 1) - (size/2);
        return new Point(x,z);
    }

    private boolean isInside(int x, int z)
    {
        if (x < -size/2f || x > size / 2f || z < -size / 2f || z > size / 2f)
            return false;
        return true;
    }
    

    // ReSharper disable once UnusedMember.Global
    public static void RunInternalTests()
    {
        int size = 1 << 2;//4
        float[] map =
        {
            0.1f, 0.2f, 0.25f,0.2f, 0.1f,
            0.2f, 0.25f,0.3f, 0.25f,0.2f,
            0.25f,0.3f, 0.35f,0.3f, 0.25f,
            0.2f, 0.25f,0.3f, 0.25f,0.2f,
            0.1f, 0.2f, 0.25f,0.2f, 0.1f
        };

        TerrainTrackHelper helper = new TerrainTrackHelper(map, size, 0.1f);

        /*
        for (int i = -size / 2; i <= size / 2; i++)
        {
            for (int j = -size / 2; j <= size / 2; j++)
            {
                Vector2i pos = new Vector2i(i, j);
                Log.p(pos + " " + helper.indexOf(pos));
            }
        }
        for (int index = 0; index <= map.Length; index++)
        {
            Log.p(index + " " + helper.posOf(index));
        }
        */

        //posOf and isInside checks
        for (int i = 0; i < map.length; i++)
        {
            Point pos = helper.posOf(i);
            int index = helper.indexOf(pos);
            Point outPos = helper.posOf(index);
            
            if (pos.x == outPos.x && pos.z == outPos.z)
            	Log.p("pos != outPos => " + pos + " != " + outPos + ", index: " + i + " != " + index);

            if (helper.isInside(pos.x, pos.z))
            	Log.p("Should be inside: i=" + i + " at " + helper.posOf(i));
        }
    }

	
	class Point {
		public int x,z;

        public Point(int x, int z)
        {
            this.x = x;
            this.z = z;
        }

        public String ToString()
        {
            return "(" + x + ", " + z + ")";
        }

        public Point add(int x, int z)
        {
            return new Point(this.x + x, this.z + z);
        }
        
        public Point from(Vector3f pos) {
        	this.x = (int)pos.x;
        	this.z = (int)pos.z;
        	
        	return this;
        }
	}
}
