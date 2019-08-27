package helper;

import java.util.ArrayDeque;
import java.util.Deque;

import com.jme3.math.Vector3f;

public class AverageV3f implements IAverager<Vector3f> {

    private final int size;
    private final Deque<Vector3f> list;

    public AverageV3f(int size) {
        this.size = Math.max(size, 1);
        this.list = new ArrayDeque<>();
    }

    public Vector3f get(Vector3f value) {
        if (size <= 1)
            return value;

        list.addFirst(value);
        if (list.size() > size)
            list.removeLast();
        
        Vector3f total = new Vector3f();
        for (Vector3f f: this.list) {
            total.x += f.x;
            total.y += f.y;
            total.z += f.z;
        }
        return total.divide(size);
    }
}