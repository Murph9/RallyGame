package rallygame.service.search;

import java.util.List;
import java.util.concurrent.TimeoutException;

public interface ISearch<T> {
    List<T> findPath(T start, T end) throws TimeoutException;
}