package rallygame.service.search;

import java.util.List;

public interface ISearch<T> {
    List<T> findPath(T start, T end);
}