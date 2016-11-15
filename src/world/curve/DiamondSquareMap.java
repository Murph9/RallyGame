package world.curve;

import java.util.Random;

import com.jme3.math.FastMath;
import com.jme3.terrain.heightmap.AbstractHeightMap;

import game.H;

public class DiamondSquareMap extends AbstractHeightMap {

	public DiamondSquareMap(int size) {
		this.size = size;
	}
	
	@Override
	public boolean load() {
		if (heightData != null)
			unloadHeightMap();
		
		heightData = new float[size*size];
		Random r = FastMath.rand;
		
		float[][] data = DiamondSquare(r.nextLong(), size);
		
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				heightData[x+y*size] = data[y][x];
			}
		}
		
		return true;
	}
	
	private float[][] DiamondSquare(long mseed, int DATA_SIZE) {
		float[][] data;
		
		//http://stackoverflow.com/a/33535810 (plus some style fixes)
		
		//size of grid to generate, note this must be a
	    //value 2^n+1
	    int width = DATA_SIZE;
	    int height = DATA_SIZE;
	    //an initial seed value for the corners of the data
	    final float SEED = 1000.0f;
	    data = new float[DATA_SIZE][DATA_SIZE];
	    //seed the data
	    data[0][0] = data[0][DATA_SIZE-1] = data[DATA_SIZE-1][0] = data[DATA_SIZE-1][DATA_SIZE-1] = SEED;

	    float valmin = Float.MAX_VALUE;
	    float valmax = Float.MIN_VALUE;

	    float h = 500.0f;//the range (-h -> +h) for the average offset
	    Random r = new Random(mseed);//for the new value in range of h
	    //side length is distance of a single square side
	    //or distance of diagonal in diamond
	    for(int sideLength = DATA_SIZE-1;
	            //side length must be >= 2 so we always have
	            //a new value (if its 1 we overwrite existing values
	            //on the last iteration)
	            sideLength >= 2;
	            //each iteration we are looking at smaller squares
	            //diamonds, and we decrease the variation of the offset
	            sideLength /=2, h/= 2.0) {
	        //half the length of the side of a square
	        //or distance from diamond center to one corner
	        //(just to make calcs below a little clearer)
	        int halfSide = sideLength/2;

	        //generate the new square values
	        for(int x=0;x<DATA_SIZE-1;x+=sideLength){
	            for(int y=0;y<DATA_SIZE-1;y+=sideLength){
	                //x, y is upper left corner of square
	                //calculate average of existing corners
	                float avg = data[x][y] + //top left
	                        data[x+sideLength][y] +//top right
	                        data[x][y+sideLength] + //lower left
	                        data[x+sideLength][y+sideLength];//lower right
	                avg /= 4.0;

	                //center is average plus random offset
	                data[x+halfSide][y+halfSide] = 
	                        //We calculate random value in range of 2h
	                        //and then subtract h so the end value is
	                        //in the range (-h, +h)
	                        avg + (r.nextFloat()*2*h) - h;

	                valmax = Math.max(valmax, data[x+halfSide][y+halfSide]);
	                valmin = Math.min(valmin, data[x+halfSide][y+halfSide]);
	            }
	        }

	        //generate the diamond values
	        //since the diamonds are staggered we only move x
	        //by half side
	        //NOTE: if the data shouldn't wrap then x < DATA_SIZE
	        //to generate the far edge values
	        for(int x=0;x<DATA_SIZE-1;x+=halfSide){
	            //and y is x offset by half a side, but moved by
	            //the full side length
	            //NOTE: if the data shouldn't wrap then y < DATA_SIZE
	            //to generate the far edge values
	            for(int y=(x+halfSide)%sideLength;y<DATA_SIZE-1;y+=sideLength){
	                //x, y is center of diamond
	                //note we must use mod  and add DATA_SIZE for subtraction 
	                //so that we can wrap around the array to find the corners
	                float avg = 
	                        data[(x-halfSide+DATA_SIZE-1)%(DATA_SIZE-1)][y] + //left of center
	                        data[(x+halfSide)%(DATA_SIZE-1)][y] + //right of center
	                        data[x][(y+halfSide)%(DATA_SIZE-1)] + //below center
	                        data[x][(y-halfSide+DATA_SIZE-1)%(DATA_SIZE-1)]; //above center
	                avg /= 4.0;

	                //new value = average plus random offset
	                //We calculate random value in range of 2h
	                //and then subtract h so the end value is
	                //in the range (-h, +h)
	                avg = avg + (r.nextFloat()*2*h) - h;
	                //update value for center of diamond
	                data[x][y] = avg;

	                valmax = Math.max(valmax, avg);
	                valmin = Math.min(valmin, avg);

	                //wrap values on the edges, remove
	                //this and adjust loop condition above
	                //for non-wrapping values.
	                if(x == 0)  data[DATA_SIZE-1][y] = avg;
	                if(y == 0)  data[x][DATA_SIZE-1] = avg;
	            }
	        }
	    }


	    for(int i=0; i<width; i++) {
	        for(int j=0; j<height; j++) { 
	            data[i][j] = ((data[i][j] - valmin) / (valmax - valmin)); 
	        } 
	    }
	    return data;
	}
}
