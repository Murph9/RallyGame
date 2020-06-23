package rallygame.service.search;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import rallygame.helper.H;

//AStar on a terrainquad
public class AStar<T> implements ISearch<T> {

    private final ISearchWorld<T> world;
    private BiConsumer<String, List<T>> progressCallback;
    private long timeoutNanos;

    private long startTime;
    private long prevTime;

    public AStar(ISearchWorld<T> world) {
        this(world, null);
    }
    public AStar(ISearchWorld<T> world, BiConsumer<String, List<T>> progressCallback) {
        this.world = world;
        this.progressCallback = progressCallback;
        
    }
    public void setProgressCallback(BiConsumer<String, List<T>> progressCallback) {
        this.progressCallback = progressCallback;
    }
    public void setTimeoutMills(long mills) {
        this.timeoutNanos = mills*1000;
    }

    @Override
    public List<T> findPath(T start, T end) throws TimeoutException {
        if (start.equals(end))
            return Arrays.asList(start);

        PriorityQueue<AStar<T>.Node> queue = new PriorityQueue<>((x, y) -> {
            return Float.compare(x.weight + x.heuristic, y.weight + y.heuristic);
        });

        var closed = new HashSet<>();
        Map<T, Float> gScore = new HashMap<>();

        AStar<T>.Node curNode = new Node(null, start, 0, 0);
        closed.add(curNode.value);
        for (T pos : world.getNeighbours(curNode.value))
            queue.add(new Node(curNode, pos, 0, world.getHeuristic(end, pos)));

        startTime = prevTime = System.nanoTime();
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

            long nowTime = System.nanoTime();
            if (prevTime + 4e8 < nowTime) {
                prevTime = nowTime;
                if (progressCallback != null) {
                    this.progressCallback.accept(queue.size() + "", generatePath(curNode));
                }
            }
            if (timeoutNanos != 0 && nowTime - startTime > timeoutNanos)
                throw new TimeoutException("The given timeout of " + H.roundDecimal(timeoutNanos / 1E9, 4) + "s was reached");
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
