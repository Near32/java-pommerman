package players.rhea.utils;

import javafx.util.Pair;
import players.optimisers.ParameterSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static players.rhea.utils.Constants.*;

public class RHEALobsterParams extends RHEAParams {

    public final int LOBSTER_HEURISTIC = 2;

    public Map<String, Object[]> getParameterValues() {
        HashMap<String, Object[]> parameterValues = new HashMap<>();
        parameterValues.put("genetic_operator", new Integer[]{MUTATION_AND_CROSSOVER, MUTATION_ONLY, CROSSOVER_ONLY});
        parameterValues.put("mutation_type", new Integer[]{MUTATION_UNIFORM, MUTATION_BIT, MUTATION_BIAS});
        parameterValues.put("selection_type", new Integer[]{SELECT_RANK, SELECT_TOURNAMENT, SELECT_ROULETTE});
        parameterValues.put("crossover_type", new Integer[]{CROSS_UNIFORM, CROSS_ONE_POINT, CROSS_TWO_POINT});
        parameterValues.put("init_type", new Integer[]{INIT_RANDOM, INIT_1SLA, INIT_MCTS});
        parameterValues.put("elitism", new Boolean[]{false, true});
        parameterValues.put("keep_parents_next_gen", new Boolean[]{false, true});

//        parameterValues.put("budget_type", new Integer[]{TIME_BUDGET, ITERATION_BUDGET, FM_BUDGET});
//        parameterValues.put("iteration_budget", new Integer[]{100, 200, 500});
//        parameterValues.put("fm_budget", new Integer[]{500, 1000, 2000, 5000});
        parameterValues.put("mcts_budget_perc", new Double[]{0.25, 0.5, 0.75});

        parameterValues.put("frame_skip", new Integer[]{0, 5, 10});
        parameterValues.put("frame_skip_type", new Integer[]{SKIP_REPEAT, SKIP_NULL, SKIP_RANDOM, SKIP_SEQUENCE});

        parameterValues.put("population_size", new Integer[]{1, 2, 5, 10, 15, 20});
        parameterValues.put("individual_length", new Integer[]{5, 10, 15, 20});
        parameterValues.put("mcts_depth", new Integer[]{5, 10, 15, 20});
        parameterValues.put("gene_size", new Integer[]{1, 2, 3, 4, 5});
        parameterValues.put("offspring_count", new Integer[]{1, 2, 5, 10, 15, 20});
        parameterValues.put("no_elites", new Integer[]{1, 2, 3});
        parameterValues.put("tournament_size_perc", new Double[]{0.2, 0.5, 0.7});
        parameterValues.put("mutation_gene_count", new Integer[]{1, 2, 3, 4, 5});
        parameterValues.put("mutation_rate", new Double[]{0.1, 0.3, 0.5, 0.7, 0.9});

        parameterValues.put("evaluate_act", new Integer[]{EVALUATE_ACT_LAST, EVALUATE_ACT_DELTA, EVALUATE_ACT_AVG,
                EVALUATE_ACT_MIN, EVALUATE_ACT_MAX, EVALUATE_ACT_DISCOUNT});
//        parameterValues.put("evaluate_update", new Integer[]{EVALUATE_UPDATE_RAW, EVALUATE_UPDATE_DELTA,
//                EVALUATE_UPDATE_AVERAGE, EVALUATE_UPDATE_MIN, EVALUATE_UPDATE_MAX});
        parameterValues.put("evaluate_discount", new Double[]{0.9, 0.95, 0.99, 1.0});
        parameterValues.put("heuristic_type", new Integer[]{WIN_SCORE_HEURISTIC, PLAYER_COUNT_HEURISTIC,
                CUSTOM_HEURISTIC, ADVANCED_HEURISTIC, LOBSTER_HEURISTIC});

        parameterValues.put("shift_buffer", new Boolean[]{false, true});
//        parameterValues.put("shift_discount", new Double[]{0.9, 0.95, 0.99, 1.0});

        parameterValues.put("mc_rollouts", new Boolean[]{false, true});
        parameterValues.put("mc_rollouts_length_perc", new Double[]{0.25, 0.5, 0.75, 1.0, 2.0});
        parameterValues.put("mc_rollouts_repeat", new Integer[]{1, 5, 10});

        return parameterValues;
    }

    public static void main(String[] args) {
        new RHEALobsterParams().printParameterSearchSpace();
    }
}
