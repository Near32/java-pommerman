package players.mcts;

import core.GameState;
import players.optimisers.ParameterizedPlayer;
import players.Player;
import utils.ElapsedCpuTimer;
import utils.Types;
import utils.ProbabilitySampler;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

public class MCTSPlayer extends ParameterizedPlayer {

    /**
     * Random generator.
     */
    private Random m_rnd;

    /**
     * All actions available.
     */
    public Types.ACTIONS[] actions;

    /**
     * Params for this MCTS
     */
    public MCTSParams params;

    /**
     * Previous root that may be re-used at the next tick.
     */
    private SingleTreeNode previous_root = null;

    // TODO: So far the action sampler is only for the whole tree. We can implement per-node
    public ProbabilitySampler<Integer> tree_action_sampler;

    public MCTSPlayer(long seed, int id) {
        this(seed, id, null);
    }

    /**
     * Constructors that receive parameters
     * @param seed seed for the algorithm to use in the random generator
     * @param id ID of this player in the game.
     * @param params Parameters for MCTS.
     */
    public MCTSPlayer(long seed, int id, MCTSParams params) {
        super(seed, id, params);
        reset(seed, id);

        ArrayList<Types.ACTIONS> actionsList = Types.ACTIONS.all();
        actions = new Types.ACTIONS[actionsList.size()];
        int i = 0;
        for (Types.ACTIONS act : actionsList) {
            actions[i++] = act;
        }

        if(this.params.collapsing)
        {
            this.tree_action_sampler = evenWeights();
        }
    }

    private ProbabilitySampler<Integer> evenWeights() {
        // Reset the action sampling distribution to a uniform one.
        Map<Integer, Float> weights = new HashMap<Integer,Float>(actions.length);
        for(int ai=0;ai<actions.length;ai++)  {   weights.put(ai, 1.0f);    }
        return new ProbabilitySampler<Integer>(weights,this.params.nbrUpdates2Uniform);
    }

    /**
     * Resets this player with seed and ID
     * @param seed seed for the algorithm to use in the random generator
     * @param playerID ID of this player in the game.
     */
    @Override
    public void reset(long seed, int playerID) {
        this.seed = seed;
        this.playerID = playerID;
        m_rnd = new Random(seed);

        this.params = (MCTSParams) getParameters();
        if (this.params == null) {
            this.params = new MCTSParams();
        }
    }

    /**
     * Action called every game tick. It must return an action to play in the real game.
     * @param gs - current game state.
     * @return the action to apply in the game.
     */
    @Override
    public Types.ACTIONS act(GameState gs) {

        //This allows us to use a time-bounded budget for MCTS
        ElapsedCpuTimer ect = new ElapsedCpuTimer();
        ect.setMaxTimeMillis(params.num_time);

        // Number of actions available
        int num_actions = actions.length;

        // Root of the tree
        SingleTreeNode m_root;
        if(this.params.reuse_tree && this.previous_root != null){
            m_root = this.previous_root.getChild(this.previous_root.mostVisitedAction());
        }
        else {
            if(params.collapsing && tree_action_sampler == null)
                tree_action_sampler = evenWeights();
            m_root = new SingleTreeNode(params, m_rnd, num_actions, actions, tree_action_sampler);
        }
        m_root.setRootGameState(gs);

        //Determine the action using MCTS...
        if(this.params.collapsing)
            m_root.collapseMctsSearch(ect);
        else
            m_root.mctsSearch(ect);

        //Determine the best action to take and return it.
        int action = m_root.mostVisitedAction();

        // Make sure that we will re-use the tree at the next iteration:
        if(this.params.reuse_tree){
            this.previous_root = m_root;
        }

        //... and return it.
        return actions[action];
    }

    @Override
    public Player copy() {
        return new MCTSPlayer(seed, playerID, params);
    }
}