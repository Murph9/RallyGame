package world.curve;

import com.jme3.math.FastMath;
import com.jme3.terrain.heightmap.AbstractHeightMap;

public class PerlinNoise extends AbstractHeightMap {

	//http://stackoverflow.com/questions/4051027/algorithm-for-creating-infinite-terrain-landscape-surface
	//http://gamedev.stackexchange.com/questions/93831/generate-next-chunk-with-perlin-noise
	//http://flafla2.github.io/2014/08/09/perlinnoise.html
	//https://gist.github.com/Flafla2/f0260a861be0ebdeef76
	
	//http://devmag.org.za/2009/04/25/perlin-noise/
	public PerlinNoise(int size) {
		this.size = size;
		this.width = size;
		this.height = size;
	}

	//TODO (is generating distinct lines..)
	
	private int width;
	private int height;
	
	@Override
	public boolean load() {
		if (heightData != null)
			unloadHeightMap();
		
		heightData = new float[size*size];

		float[][] baseNoise = GenerateWhiteNoise(width, height);
		float[][] data = GeneratePerlinNoise(baseNoise, 4);
		
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				heightData[x+y*size] = data[y][x];
			}
		}
		
		return true;
	}
	
	private static float[][] Empty2DArray(int width, int height) {
		float[][] arrays = new float[width][1];
		for (int i = 0; i < width; i++)
			arrays[i] = new float[height];
		return arrays;
	}
	
	private float[][] GeneratePerlinNoise(float[][] baseNoise, int octaveCount)
    {
        int width = baseNoise.length;
        int height = baseNoise[0].length;

        float[][][] smoothNoise = new float[octaveCount][][]; //an array of 2D arrays containing

        float persistance = 0.7f;

        //generate smooth noise
        for (int i = 0; i < octaveCount; i++)
            smoothNoise[i] = GenerateSmoothNoise(baseNoise, i);

        float[][] perlinNoise = Empty2DArray(width, height); //an array of floats initialised to 0

        float amplitude = 1.0f;
        float totalAmplitude = 0.0f;
        
        //blend noise together
        for (int octave = octaveCount - 1; octave >= 0; octave--)
        {
            amplitude *= persistance;
            totalAmplitude += amplitude;

            for (int i = 0; i < width; i++)
                for (int j = 0; j < height; j++)
                    perlinNoise[i][j] += smoothNoise[octave][i][j] * amplitude;
        }

        //normalisation
        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++)
                perlinNoise[i][j] /= totalAmplitude;

        return perlinNoise;
    }
	
	private float[][] GenerateSmoothNoise(float[][] baseNoise, int octave)
    {
        int width = baseNoise.length;
        int height = baseNoise[0].length;

        float[][] smoothNoise = Empty2DArray(width, height);

        int samplePeriod = 1 << octave; // calculates 2 ^ k
        float sampleFrequency = 1.0f / samplePeriod;

        for (int i = 0; i < width; i++)
        {
            //calculate the horizontal sampling indices
            int sample_i0 = (i / samplePeriod) * samplePeriod;
            int sample_i1 = (sample_i0 + samplePeriod) % width; //wrap around
            float horizontal_blend = (i - sample_i0) * sampleFrequency;

            for (int j = 0; j < height; j++)
            {
                //calculate the vertical sampling indices
                int sample_j0 = (j / samplePeriod) * samplePeriod;
                int sample_j1 = (sample_j0 + samplePeriod) % height; //wrap around
                float vertical_blend = (j - sample_j0) * sampleFrequency;

                //blend the top two corners
                float top = FastMath.interpolateLinear(baseNoise[sample_i0][sample_j0],
                    baseNoise[sample_i1][sample_j0], horizontal_blend);

                //blend the bottom two corners
                float bottom = FastMath.interpolateLinear(baseNoise[sample_i0][sample_j1],
                    baseNoise[sample_i1][sample_j1], horizontal_blend);

                //final blend
                smoothNoise[i][j] = FastMath.interpolateLinear(top, bottom, vertical_blend);
            }
        }
        
        return smoothNoise;
    }
	
	private float[][] GenerateWhiteNoise(int width, int height)
    {
        float[][] noise = Empty2DArray(width, height);

        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++)
                noise[i][j] = (float)FastMath.rand.nextFloat() % 1;

        return noise;
    }
}
