package rallygame.service.search;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiConsumer;

public class GreedySearch<T> implements ISearch<T> {

    private final ISearchWorld<T> world;
    private final Set<T> closed;
    private final BiConsumer<String, List<T>> progressCallback;
    private long prevTime;

    public GreedySearch(ISearchWorld<T> world) {
        this(world, null);
    }
    
    public GreedySearch(ISearchWorld<T> world, BiConsumer<String, List<T>> progressCallback) {
        this.world = world;
        this.progressCallback = progressCallback;
        closed = new HashSet<>();
    }

    public List<T> findPath(T start, T end) {
        PriorityQueue<Node> queue = new PriorityQueue<>((x, y) -> {
            return Float.compare(x.score, y.score);
        });

        closed.clear();

        Node curNode = new Node(null, start, world.getHeuristic(end, start));
        closed.add(curNode.value);
        for (T pos : world.getNeighbours(curNode.value))
            queue.add(new Node(curNode, pos, world.getHeuristic(end, pos)));

        prevTime = System.nanoTime();
        while (!queue.isEmpty()) {
            curNode = queue.poll();
            closed.add(curNode.value);

            if (curNode.value.equals(end))
                return generatePath(curNode);

            for (T pos : world.getNeighbours(curNode.value)) {
                Node n = new Node(curNode, pos, world.getHeuristic(end, pos));
                if (!(closed.contains(n.value))) {
                    queue.add(n);
                }
            }

            long nowTime = System.nanoTime();
            if (prevTime + 4e8 < nowTime) {
                prevTime = nowTime;
                if (progressCallback != null)
                    this.progressCallback.accept(queue.size() + "", generatePath(curNode));
            }
        }

        throw new IllegalStateException("Unknown path, please try and fix that");
    }

    private static <T> List<T> generatePath(GreedySearch<T>.Node end) {
        List<T> result = new LinkedList<>();
        GreedySearch<T>.Node cur = end;
        while (cur != null) {
            result.add(cur.value);
            cur = cur.parent;
        }
        return result;
    }

    class Node {
        final Node parent;
        final T value;
        final float score;

        Node(Node parent, T value, float s) {
            this.parent = parent;
            this.value = value;
            this.score = s;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;

            if (!(obj instanceof AStar.Node)) {
                return false;
            }

            GreedySearch<?>.Node n = (GreedySearch<?>.Node) obj;
            return n.parent.value.equals(this.parent.value) && n.value.equals(this.value);
        }

        @Override
        public int hashCode() {
            return parent.hashCode() * value.hashCode();
        }
    }
}
