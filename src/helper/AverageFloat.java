package helper;

import java.util.ArrayDeque;
import java.util.Deque;

public class AverageFloat implements IAverager<Float> {

    private final int size;
    private final Deque<Float> list;

    public AverageFloat(int size) {
        this.size = Math.max(size, 1);
        this.list = new ArrayDeque<>();
    }

    public Float get(Float value) {
        if (size <= 1)
            return value;

        list.addFirst(value);
        if (list.size() > size)
            list.removeLast();
        
        float total = 0;
        for (float f: this.list) {
            total += f;
        }
        return total /= size;
    }
}
