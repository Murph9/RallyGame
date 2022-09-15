package survival.ability;

import java.util.Map;

public abstract class Ability {
    public static final String TYPE_EXPLODE = "explode";

    abstract String type();
    abstract boolean update(float tpf);
    abstract Map.Entry<String, Object> GetProperties();
}
