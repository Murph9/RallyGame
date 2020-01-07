package service.averager;

import java.util.ArrayDeque;
import java.util.Deque;

public abstract class AveragerBase<T extends Object> implements IAverager<T> {

    private final int size;
    private final IAverager.Type type;
    private final Deque<T> list;

    public AveragerBase(int size, IAverager.Type type) {
        this.type = type;
        this.size = Math.max(size, 1);
        this.list = new ArrayDeque<>();
    }

    protected abstract T createBlank();
    protected abstract T add(T value1, T value2);
    protected abstract T mult(T value, float mult);

    public T get(T value) {
        if (size <= 1)
            return value;

        list.addFirst(value);
        if (list.size() > size)
            list.removeLast();

        float size = list.size(); //float for dividing
        T total = createBlank();
        switch (type) {
        case Simple:
            for (T t : this.list) {
                total = add(total, t);
            }
            return mult(total, 1/(float)size);
        case Weighted:
            int i = 0;
            for (T t : this.list) {
                total = add(total, mult(t, (size - i)));
                i++;
            }
            return mult(total, 1/ ((size * (size + 1)) / 2));
        default:
            return value;
        }
    }
}