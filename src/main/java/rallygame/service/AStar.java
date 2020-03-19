package rallygame.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//AStar on a terrainquad
public class AStar<T> {

    public interface IAStarWorld<T> {
        float getWeight(T v1, T v2);

        float getHeuristic(T v1, T v2);

        Set<T> getNeighbours(T v);
    }

    private final IAStarWorld<T> world;
    public final Set<T> closed;

    public AStar(IAStarWorld<T> world) {
        this.world = world;
        closed = Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    public List<T> findPath(T start, T end) {
        PriorityQueue<Node<T>> queue = new PriorityQueue<>((x, y) -> {
            return Float.compare(x.weight + x.heuristic, y.weight + y.heuristic);
        });

        closed.clear();
        Map<T, Float> gScore = new HashMap<>();

        Node<T> curNode = new Node<T>(null, start, 0, 0);
        closed.add(curNode.value);
        for (T pos : world.getNeighbours(curNode.value))
            queue.add(new Node<T>(curNode, pos, 0, world.getHeuristic(end, pos)));

        while (!queue.isEmpty()) {
            curNode = queue.poll();
            closed.add(curNode.value);

            if (curNode.value.equals(end))
                return generatePath(curNode);

            for (T pos : world.getNeighbours(curNode.value)) {
                Node<T> n = new Node<T>(curNode, pos, curNode.weight + world.getWeight(curNode.value, pos),
                        world.getHeuristic(end, pos));
                if (!(closed.contains(n.value))) {
                    if (!gScore.containsKey(n.value)
                            || (gScore.containsKey(n.value) && (n.weight) < gScore.get(n.value))) {
                        queue.add(n);
                    }
                }
            }
        }

        throw new IllegalStateException("Unknown path, please try and fix that");
    }

    private static <T> List<T> generatePath(Node<T> end) {
        List<T> result = new LinkedList<>();
        Node<T> cur = end;
        while (cur != null) {
            result.add(cur.value);
            cur = cur.parent;
        }
        return result;
    }
}

class Node<U> {
    final Node<U> parent;
    final U value;
    final float weight;
    final float heuristic;

    Node(Node<U> parent, U value, float w, float h) {
        this.parent = parent;
        this.value = value;
        this.weight = w;
        this.heuristic = h;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        if (!(obj instanceof Node)) {
            return false;
        }

        Node<?> n = (Node<?>) obj;
        return n.parent.value.equals(this.parent.value) && n.value.equals(this.value);
    }

    @Override
    public int hashCode() {
        return parent.hashCode() * value.hashCode();
    }
}