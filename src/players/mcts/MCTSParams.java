package players.mcts;

import core.GameState;
import players.heuristics.*;
import javafx.util.Pair;
import players.optimisers.ParameterSet;
import utils.Clustering.Clusterer;

import java.util.*;
import java.util.function.Function;

@SuppressWarnings("WeakerAccess")
public class MCTSParams implements ParameterSet {

    // Constants
    public final boolean reuse_tree = false;    // Whether we re-use the tree from one tick to another.

    public final double HUGE_NEGATIVE = -1000;
    public final double HUGE_POSITIVE =  1000;

    public final int STOP_TIME = 0;
    public final int STOP_ITERATIONS = 1;
    public final int STOP_FMCALLS = 2;

    public final int CUSTOM_HEURISTIC = 0;
    public final int ADVANCED_HEURISTIC = 1;
    public final int OUR_HEURISTIC= 2;
    public final int MULTI_HEURISTIC_A = 3;
    public double epsilon = 1e-6;

    // Collapsing-MCTS:
    public boolean collapsing = false;        // Whether we use Vanilla/Collapsing-MCTS.
    public Function<GameState, List<Float>> ClusteringHeuristicFunction = null; // Make sure to set this if clustering is true
    public boolean useActionSamplingDistributionAtExpansion = false;
    public  boolean useDBScan =false;
    public int nbrUpdates2Uniform = 100;
    public float maxClusterRatio = 0.25f;
    public int nbrClustererCycles = 4;
    /** Controls whether there is one action probability distribution for the whole tree, or one per node */
    public boolean globalActionDistribution = false;
    public Clusterer.DISTANCE_METRIC distanceMeasure = Clusterer.DISTANCE_METRIC.Euclidean;
    public int DBSscanMaxElements = 4;
    public double DBSscanMaxDist = 1;

    // Parameters
    public double K = Math.sqrt(2);
    public int rollout_depth = 8;//10;
    public int heuristic_method = CUSTOM_HEURISTIC;

    // Budget settings
    public int stop_type = STOP_TIME;
    public int num_iterations = 200;
    public int num_fmcalls = 2000;
    public int num_time = 40;

    @Override
    public void setParameterValue(String param, Object value) {
        switch(param) {
            case "K": K = (double) value; break;
            case "rollout_depth": rollout_depth = (int) value; break;
            case "heuristic_method": heuristic_method = (int) value; break;
            // Our params
            case "collapsing": collapsing = (boolean) value; break;
            case "ClusteringHeuristicFunction": ClusteringHeuristicFunction = (Function<GameState, List<Float>>) value; break;
            case "nbrUpdates2Uniform": nbrUpdates2Uniform = (int) value; break;
            case "maxClusterRatio": maxClusterRatio = (float) value; break;
            case "nbrClustererCycles": nbrClustererCycles = (int) value; break;
            case "globalActionDistribution": globalActionDistribution = (boolean) value; break;

            case "useDBScan": useDBScan = (boolean)value;break;
            case "distanceMeasure": distanceMeasure = Clusterer.DISTANCE_METRIC.fromInt((int)value);break;
            case "DBSscanMaxElements": DBSscanMaxElements = (int)value;break;
            case "DBSscanMaxDist": DBSscanMaxDist = (float)value;break;
        }
    }

    @Override
    public Object getParameterValue(String param) {
        switch(param) {
            case "K": return K;
            case "rollout_depth": return rollout_depth;
            case "heuristic_method": return heuristic_method;

            // Our params
            case "collapsing": return collapsing;
            case "ClusteringHeuristicFunction": return ClusteringHeuristicFunction;
            case "nbrUpdates2Uniform": return nbrUpdates2Uniform;
            case "maxClusterRatio": return maxClusterRatio;
            case "nbrClustererCycles": return nbrClustererCycles;

            case "globalActionDistribution": return globalActionDistribution;

            case "useDBScan": return useDBScan;
            case "distanceMeasure":return  distanceMeasure.getKey();
            case "DBSscanMaxElements":return DBSscanMaxElements;
            case "DBSscanMaxDist":return DBSscanMaxDist;
        }
        return null;
    }

    @Override
    public ArrayList<String> getParameters() {
        ArrayList<String> paramList = new ArrayList<>();
        paramList.add("K");
        paramList.add("rollout_depth");
        paramList.add("heuristic_method");

        paramList.add("collapsing");
        paramList.add("ClusteringHeuristicFunction");
        paramList.add("nbrUpdates2Uniform");
        paramList.add("maxClusterRatio");
        paramList.add("nbrClustererCycles");
        paramList.add("globalActionDistribution");

        paramList.add("useDBScan");
        paramList.add("distanceMeasure");
        paramList.add("DBSscanMaxElements");
        paramList.add("DBSscanMaxDist");
        return paramList;
    }

    @Override
    public Map<String, Object[]> getParameterValues() {
        List<Function<GameState, List<Float>>> clusterHeuristics = new ArrayList<>();
        clusterHeuristics.add(gs->
        {
            List<Float> ret = new ArrayList<>();
            MultiHeuristicA sh = new MultiHeuristicA();
            List<Double> values = sh.evaluateState(gs);
            for(int i=0;i<values.size();i++){
                double value = values.get(i);
                ret.add( (float) value);
            }
            return ret;
        });

        HashMap<String, Object[]> parameterValues = new HashMap<>();
        parameterValues.put("K", new Double[]{1.0, Math.sqrt(2), 2.0});
        parameterValues.put("rollout_depth", new Integer[]{5, 8, 10, 12, 15});
        parameterValues.put("heuristic_method", new Integer[]{CUSTOM_HEURISTIC, ADVANCED_HEURISTIC, OUR_HEURISTIC});

        parameterValues.put("collapsing", new Boolean[] {true});
        parameterValues.put("ClusteringHeuristicFunction", clusterHeuristics.toArray());
        parameterValues.put("nbrUpdates2Uniform", new Integer[] {30,50, 100});
        parameterValues.put("maxClusterRatio", new Float[] {0.1f});
        parameterValues.put("nbrClustererCycles", new Integer[] {1});
        parameterValues.put("globalActionDistribution", new Boolean[] {true, false});

        parameterValues.put("useDBScan", new Boolean[] {true});
        parameterValues.put("distanceMeasure", new Integer[] {0,1,2,3});
        parameterValues.put("DBSscanMaxElements", new Integer[] {6,8,10,12});
        parameterValues.put("DBSscanMaxDist", new Float[] {.6f,.8f,1.0f,1.2f});

        return parameterValues;
    }

    @Override
    public Pair<String, ArrayList<Object>> getParameterParent(String parameter) {
        return null;  // No parameter dependencies
    }

    @Override
    public Map<Object, ArrayList<String>> getParameterChildren(String root) {
        return new HashMap<>();  // No parameter dependencies
    }

    @Override
    public Map<String, String[]> constantNames() {
        HashMap<String, String[]> names = new HashMap<>();
        names.put("heuristic_method", new String[]{"CUSTOM_HEURISTIC", "ADVANCED_HEURISTIC", "OUR_HEURISTIC"});
        return names;
    }
}
