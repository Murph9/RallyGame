package rallygame.service.averager;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class AverageFloatFramerate {

    private final float period;
    private List<Entry> total;
    public AverageFloatFramerate(float period) {
        this.period = period;
        total = new LinkedList<Entry>();
    }

    public float get(float value, float tpf) {
        total.add(0, new Entry(value, tpf));

        float left = period;
        float totalValue = 0;
        Iterator<Entry> li = total.iterator();
        while (left > 0 && li.hasNext()) {
            Entry e = li.next();
            float time = Math.min(left, e.tpf);
            totalValue += time*e.value;
            left -= e.tpf;
        }

        return totalValue / Math.min(period, period - left);
    }

    class Entry {
        public final float value;
        public final float tpf;
        Entry(float value, float tpf) {
            this.value = value;
            this.tpf = tpf;
        }

        @Override
        public String toString() {
            return "v:"+value+"["+tpf+"]";
        }
    }
}
