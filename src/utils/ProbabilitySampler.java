package utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ProbabilitySampler<T> {
    private Random random;
    private Map<T, Float> weights;
    private int nbrUpdates2Uniform;
    private int updateCounter;
    private Map<T, Float> weightUniformer;  // geometric ratios that transforms the distribution into a uniform one in (exactly) nbrUpdates2Uniform updates.
    ///

    // TODO: Hmm, what happens if we have negative weights put in at any point?
    //  eg an action has a negative novelty? (don't even know if that's possible). Might have to clamp weights to 0
    public ProbabilitySampler(Random random, Map<T, Float> weights, int nbrUpdates2Uniform)
    {
        this.random = random;
        this.weights = new HashMap<>();
        // Transform the weights to make sure that they fit a density probability distribution:
        updateWeights(weights);

        if(nbrUpdates2Uniform <= 0)
        {
            throw new IllegalArgumentException("Need a non-negative number of updates to transform the distribution to a uniform one.");
        }
        this.updateCounter = 0;
        this.nbrUpdates2Uniform = nbrUpdates2Uniform;
        this.weightUniformer = new HashMap<T, Float>();
        for(T key: this.weights.keySet())
        {
            Float w = this.weights.get(key);
            Float wu = (float) Math.exp(-Math.log(w) / this.nbrUpdates2Uniform);
            this.weightUniformer.put( key, wu);
        }
    }

    public ProbabilitySampler(Map<T, Float> weights, int nbrUpdates2Uniform)
    {
        this(new Random(), weights, nbrUpdates2Uniform);
    }

    public T sample(Map<T,Boolean> mask)
    {
        float weightSum = weightSum(this.weights, mask);
        float rn = random.nextFloat() * weightSum;
        float rangeMin = 0;
        T ret = null;
        for (Map.Entry<T, Float> kv : weights.entrySet())
        {
            T key = kv.getKey();
            if(mask == null || (mask.containsKey(key) && mask.get(key)) )
            {
                float rangeMax = rangeMin + kv.getValue();
                ret = kv.getKey();
                if (rn > rangeMin && rn < rangeMax) {
                    break;
                } else
                    rangeMin = rangeMax;
            }
        }
        return ret;
    }

    /** Transform the weights to make sure that they fit a density probability distribution: */
    public void updateWeights(Map<T,Float> weights)
    {
        this.weights.clear();
        Float sumexpw = 0f;
        for(T key: weights.keySet())
        {
            Float expw = (float)Math.exp(weights.get(key));
            sumexpw += expw;
            this.weights.put( key, expw);
        }
        for(T key: this.weights.keySet())
        {
            Float normalized_expw = this.weights.get(key)/sumexpw;
            this.weights.replace( key, normalized_expw);
        }
    }

    public void updateWeightsAveraged(Map<T,Float> weights)
    {
        weights = softmax(weights);
        this.weights = softmax(this.weights);
        for(T key: this.weights.keySet())
        {
            Float new_w = this.weights.get(key) + weights.get(key);
            this.weights.replace( key, new_w);
        }
        this.weights = softmax(this.weights);
    }

    /**
     * Updates the distribution towards the Uniform distribution.
     * @returns boolean specifying whether the distribution is uniform already or not.
     */
    public boolean updateWeights2Uniform()
    {
        if(updateCounter <= nbrUpdates2Uniform)
        {
            updateCounter++;
            for(T key: this.weights.keySet())
            {
                Float w = this.weights.get(key);
                Float wu = this.weightUniformer.get(key);
                Float new_w = w*wu;
                this.weights.replace(key, w, new_w);
            }
            return false;
        }
        else
        {
            return true;
        }
    }

    public Map<T, Float> getWeights()
    {
        return this.weights;
    }

    public static <T> Float weightSum(Map<T, Float> weights, Map<T,Boolean> mask)
    {
        Float sum = 0f;
        for(Map.Entry<T, Float> kv : weights.entrySet())
        {
            T key = kv.getKey();
            if (mask == null || (mask.containsKey(key) && mask.get(key)) ) {
                sum += kv.getValue();
            }
        }
        return sum;
    }

    public static <T> Map<T, Float> softmax(Map<T, Float> weights)
    {
        for(T key: weights.keySet())
        {
            Float new_w = (float) Math.exp(weights.get(key));
            weights.replace( key, new_w);
        }
        Float sum_exp = weightSum(weights, null);
        for(T key: weights.keySet())
        {
            Float new_w = weights.get(key)/sum_exp;
            weights.replace( key, new_w);
        }

        return weights;
    }


}
