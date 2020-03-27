package rallygame.service.search;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiConsumer;

//AStar on a terrainquad
public class AStar<T> implements ISearch<T> {

    private final ISearchWorld<T> world;
    private final Set<T> closed;
    private final BiConsumer<String, List<T>> progressCallback;
    private long prevTime;

    public AStar(ISearchWorld<T> world) {
        this(world, null);
    }
    public AStar(ISearchWorld<T> world, BiConsumer<String, List<T>> progressCallback) {
        this.world = world;
        this.progressCallback = progressCallback;
        closed = new HashSet<>();
    }

    public List<T> findPath(T start, T end) {
        if (start.equals(end))
            return Arrays.asList(start);

        PriorityQueue<AStar<T>.Node> queue = new PriorityQueue<>((x, y) -> {
            return Float.compare(x.weight + x.heuristic, y.weight + y.heuristic);
        });

        closed.clear();
        Map<T, Float> gScore = new HashMap<>(); //TODO does this work?

        AStar<T>.Node curNode = new Node(null, start, 0, 0);
        closed.add(curNode.value);
        for (T pos : world.getNeighbours(curNode.value))
            queue.add(new Node(curNode, pos, 0, world.getHeuristic(end, pos)));

        prevTime = System.nanoTime();
        while (!queue.isEmpty()) {
            curNode = queue.poll();
            closed.add(curNode.value);
            if (!gScore.containsKey(curNode.value) || gScore.get(curNode.value) < curNode.weight)
                gScore.put(curNode.value, curNode.weight);

            if (curNode.value.equals(end))
                return generatePath(curNode);

            for (T pos : world.getNeighbours(curNode.value)) {
                AStar<T>.Node n = new Node(curNode, pos, curNode.weight + world.getWeight(curNode.value, pos),
                        world.getHeuristic(end, pos));
                if (!(closed.contains(n.value))) {
                    if (!gScore.containsKey(n.value) || n.weight < gScore.get(n.value)) {
                        queue.add(n);
                    }
                }
            }

            if (progressCallback != null) {
                long nowTime = System.nanoTime();
                if (prevTime + 4e8 < nowTime) {
                    prevTime = nowTime;
                    this.progressCallback.accept(queue.size() + "", generatePath(curNode));
                }
            }
        }

        throw new IllegalStateException("Unknown path, please try and fix that");
    }

    private static <T> List<T> generatePath(AStar<T>.Node end) {
        List<T> result = new LinkedList<>();
        AStar<T>.Node cur = end;
        while (cur != null) {
            result.add(cur.value);
            cur = cur.parent;
        }
        Collections.reverse(result);
        return result;
    }

    class Node {
        final Node parent;
        final T value;
        final float weight;
        final float heuristic;

        Node(Node parent, T value, float w, float h) {
            this.parent = parent;
            this.value = value;
            this.weight = w;
            this.heuristic = h;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;

            if (!(obj instanceof AStar.Node)) {
                return false;
            }

            AStar<?>.Node n = (AStar<?>.Node) obj;
            return n.parent.value.equals(this.parent.value) && n.value.equals(this.value);
        }

        @Override
        public int hashCode() {
            return parent.hashCode() * value.hashCode();
        }
    }
}
