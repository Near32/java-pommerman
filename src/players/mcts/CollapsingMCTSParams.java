package players.mcts;

import core.GameState;
import players.heuristics.*;

import java.util.*;
import java.util.function.Function;

/** Used for testing our params, and also the vanilla ones
 *  The clustering-specific parameters aren't in here for now to avoid making potentially breaking changes
 *  with a small amount of time left
 *
 *  Also, means we can just toggle collapsing really easily for NTBEA*/
public class CollapsingMCTSParams extends MCTSParams {
    //public static final boolean TEST_VANILLA_PARAMS = false;
    //public static final boolean TEST_CLUSTER_PARAMS = true;

    public boolean collapsing = true;        // Whether we use Vanilla/Collapsing-MCTS.
    public Function<GameState, List<Float>> ClusteringHeuristicFunction = null;
    public int nbrUpdates2Uniform = 100;
    public float maxClusterRatio = 0.25f;
    public int nbrClustererCycles = 4;

    private static final Function<GameState, List<Float>> defaultClusteringHeuristic =
        gs -> {
            List<Float> ret = new ArrayList<>();
            for(int shk: new Integer[] {0, 1, 2, 3})
            {
                StateHeuristic sh;
                if (shk == MCTSParams.CUSTOM_HEURISTIC)
                    sh = new CustomHeuristic(gs);
                else if (shk == MCTSParams.OUR_HEURISTIC)
                    sh = new OurHeuristic();
                else if (shk == MCTSParams.ADVANCED_HEURISTIC) {
                    Random rnd = new Random();
                    sh = new AdvancedHeuristic(gs, rnd);
                }
                else
                    sh = new PlayerCountHeuristic();

                ret.add( (float) sh.evaluateState(gs));
            }
            return ret;
        };

    public CollapsingMCTSParams() {
        super();
        ClusteringHeuristicFunction = defaultClusteringHeuristic;
    }

    @Override
    public void setParameterValue(String param, Object value) {
        super.setParameterValue(param, value);
        switch(param) {
            case "collapsing": collapsing = (boolean) value; break;
            case "ClusteringHeuristicFunction": ClusteringHeuristicFunction = (Function<GameState, List<Float>>) value; break;
            case "nbrUpdates2Uniform": nbrUpdates2Uniform = (int) value; break;
            case "maxClusterRatio": maxClusterRatio = (float) value; break;
            case "nbrClustererCycles": nbrClustererCycles = (int) value; break;
        }
    }

    @Override
    public Object getParameterValue(String param) {
        Object value = super.getParameterValue(param);
        if(value != null)
            return value;

        switch(param) {
            case "collapsing": return collapsing;
            case "ClusteringHeuristicFunction": return ClusteringHeuristicFunction;
            case "nbrUpdates2Uniform": return nbrUpdates2Uniform;
            case "maxClusterRatio": return maxClusterRatio;
            case "nbrClustererCycles": return nbrClustererCycles;
        }
        return null;
    }

    @Override
    public ArrayList<String> getParameters() {
        ArrayList<String> paramList = new ArrayList<>();
        paramList.add("collapsing");
        paramList.add("ClusteringHeuristicFunction");
        paramList.add("nbrUpdates2Uniform");
        paramList.add("maxClusterRatio");
        paramList.add("nbrClustererCycles");
        return paramList;
    }

    @Override
    public Map<String, Object[]> getParameterValues() {
        List<Function<GameState, List<Float>>> clusteringHeuristics = new ArrayList<>();
        // This is fine as we never change defaultClusteringHeuristic
        clusteringHeuristics.add(defaultClusteringHeuristic);

        Map<String, Object[]> parameterValues = super.getParameterValues();
        parameterValues.put("collapsing", new Boolean[]{true, false});
        parameterValues.put("ClusteringHeuristicFunction", clusteringHeuristics.toArray());
        parameterValues.put("nbrUpdates2Uniform", new Integer[] {50, 100, 200});
        parameterValues.put("maxClusterRatio", new Float[] {0.25f, 1f/3f, 0.4f});
        parameterValues.put("maxClustererCycles", new Integer[] { 1, 4, 8 });
        return parameterValues;
    }
}
