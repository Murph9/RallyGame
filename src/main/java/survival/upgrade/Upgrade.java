package survival.upgrade;

import java.util.List;
import java.util.function.Consumer;

public abstract class Upgrade<T> {

    public final boolean positive;
    public final String label;
    private final Consumer<T> func;

    public Upgrade(boolean positive, String label, Consumer<T> func) {
        this.positive = positive;
        this.label = label;
        this.func = func;
    }

    public abstract boolean applies(List<Upgrade<?>> existing);

    public void accept(T t) {
        func.accept(t);
    }

    public Consumer<T> get() {
        return func;
    }
}
