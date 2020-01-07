package service.averager;

public interface IAverager<T> {
    T get(T value);

    public enum Type {
        Simple, Weighted;
    }
}
