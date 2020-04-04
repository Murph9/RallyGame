package rallygame.service.averager;

public interface IAverager<T> {
    T get(T value);
    T get();

    public enum Type {
        Simple, Weighted;
    }
}
