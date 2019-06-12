package players.mcts;

import players.mcts.nodes.SingleTreeNode;
import utils.ElapsedCpuTimer;
import utils.ProbabilitySampler;

import java.util.HashMap;
import java.util.Map;

public class CollapsingPlayer extends MCTSPlayer {
    // This hides the params field of MCTSPlayer
    public CollapsingMCTSParams params;

    public CollapsingPlayer(long seed, int id, CollapsingMCTSParams params) {
        super(seed, id, params);

        this.params = params;
        if(this.params.collapsing)
        {
            // Reset the action sampling distribution to a uniform one.
            Map<Integer, Float> weights = new HashMap<>(actions.length);
            for(int ai=0;ai<=actions.length;ai++)
                weights.put(ai, 1.0f);
            this.tree_action_sampler = new ProbabilitySampler<Integer>(weights,this.params.nbrUpdates2Uniform);
        }
    }

    @Override
    protected void search(SingleTreeNode root, ElapsedCpuTimer ect) {
        root.collapseMctsSearch(ect);
    }

    @Override
    public void reset(long seed, int playerID) {
        super.reset(seed, playerID);

        // Shame the params resetting has to be done in MCTS then this
        this.params = (CollapsingMCTSParams) getParameters();
        if (this.params == null) {
            this.params = new CollapsingMCTSParams();
        }
    }
}
