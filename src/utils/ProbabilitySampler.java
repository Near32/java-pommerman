package utils;

import java.util.Map;
import java.util.Random;

public class ProbabilitySampler<T> {
    private Random random;
    private Map<T, Float> weights;
    ///

    public ProbabilitySampler(Random random, Map<T, Float> weights) {
        this.random = random;
        this.weights = weights;
    }

    public ProbabilitySampler(Map<T, Float> weights) {
        this(new Random(), weights);
    }

    public T sample() {
        float weightSum = weightSum();
        float rn = random.nextFloat() * weightSum;
        float rangeMin = 0;
        for (Map.Entry<T, Float> kv : weights.entrySet()) {
            float rangeMax = rangeMin + kv.getValue();
            if(rn > rangeMin && rn < rangeMax)
                return kv.getKey();
            else
                rangeMin = rangeMax;
        }
    }

    public void updateWeight() {

    }

    private float weightSum() {
        int sum = 0;
        for(float weight : weights.values())
            sum += weight;
        return sum;
    }


}
