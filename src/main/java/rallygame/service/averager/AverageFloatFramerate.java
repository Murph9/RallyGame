package rallygame.service.averager;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class AverageFloatFramerate {

    private final float period;
    private final IAverager.Type type;
    private List<Entry> total;

    public AverageFloatFramerate(float period, IAverager.Type type) {
        this.period = period;
        this.type = type;
        total = new LinkedList<Entry>();
    }

    public float get(float value, float tpf) {
        if (tpf <= 0)
            throw new IllegalArgumentException("no negative tpf pls");

        total.add(0, new Entry(value, tpf));

        if (total.size() > 1000) {
            // do some culling when the list gets very long
            var newList = new LinkedList<Entry>();
            Iterator<Entry> li = total.iterator();
            float startsAt = period;
            while (startsAt > 0 && li.hasNext()) {
                Entry e = li.next();
                startsAt -= e.tpf;
                newList.add(e); //add existing elements to new list, then delete the old one
            }
            total = newList;
        }

        Iterator<Entry> li = total.iterator();
        switch (type) {
            case Weighted:
                float startsAt = period;
                float sumN = 0;
                float sumM = 0;
                while (startsAt > 0 && li.hasNext()) {
                    Entry e = li.next();
                    float midWeight = startsAt - e.tpf / 2;
                    float scaledWeight = e.tpf * midWeight;
                    float weightValue = scaledWeight * e.value;

                    sumM += scaledWeight;
                    sumN += weightValue;
                    startsAt -= e.tpf;
                }

                return sumN / sumM;
            case Simple:
                float left = period;
                float totalValue = 0;

                while (left > 0 && li.hasNext()) {
                    Entry e = li.next();
                    float time = Math.min(left, e.tpf);
                    totalValue += time * e.value;
                    left -= e.tpf;
                }

                return totalValue / Math.min(period, period - left);
        }
        throw new IllegalArgumentException("UNknown ytpe:" + type);
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
            return "v:" + value + "[" + tpf + "]";
        }
    }
}
