package rallygame.service.search;

import java.util.Set;

public interface ISearchWorld<T> {
    float getWeight(T v1, T v2);

    float getHeuristic(T v1, T v2);

    Set<T> getNeighbours(T v);
}