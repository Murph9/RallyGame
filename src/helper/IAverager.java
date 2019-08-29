package helper;

public interface IAverager<T> {
    T get(T value);

    public enum Type {
        Simple, Weighted;
    }
}
