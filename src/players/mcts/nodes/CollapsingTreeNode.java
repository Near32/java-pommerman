package players.mcts.nodes;

import core.GameState;
import players.mcts.CollapsingMCTSParams;
import utils.Clustering.ClusteringResult;
import utils.Clustering.KMeansStateClusterer;
import utils.ElapsedCpuTimer;
import utils.ProbabilitySampler;
import utils.Types;
import utils.Utils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CollapsingTreeNode extends SingleTreeNode {
    // Shadows the SingleTreeNode's MCTSparams
    CollapsingMCTSParams params;

    private Function<GameState, List<Float>> ClusteringHeuristicFunction;
    private KMeansStateClusterer clusterer;      //Clusterer
    private List<List<ClusteringResult>> clusters;      //Results of the clustering. Initialized at expansion time...
    /** Sampler of actions, each action having a given weight affecting how much it is chosen during simulation roll-outs*/
    private ProbabilitySampler<Integer> actionSampler;

    protected List<SingleTreeNode> representativeChildren;  //Representatives of clusters of children of this node.
    protected int[] children2ClusterIdx;                    //For each child, the index of the cluster it is part of.

    CollapsingTreeNode(CollapsingMCTSParams p, Random rnd, int num_actions, Types.ACTIONS[] actions, ProbabilitySampler<Integer> actionSampler) {
        super(p, rnd, num_actions, actions, actionSampler);
        this.params = p;
        this.representativeChildren = null;
        children2ClusterIdx = new int[num_actions];
        if(parent == null) {
            this.ClusteringHeuristicFunction = p.ClusteringHeuristicFunction;
        }
        this.clusterer = new KMeansStateClusterer( (int)(this.params.maxClusterRatio*this.num_actions), this.params.nbrClustererCycles );
        this.actionSampler = actionSampler;
    }

    CollapsingTreeNode(GameState currentState, CollapsingMCTSParams p, Random rnd, int num_actions, Types.ACTIONS[] actions, ProbabilitySampler<Integer> actionSampler) {
        super(currentState, p, rnd, num_actions, actions, actionSampler);
        this.params = p;
    }

    /**
     * Main entry point of the algorithm. Runs Collapse-MCTS until budget is exhausted. Budget is determined by the MCTSParams
     * variable of this class.
     * @param elapsedTimer In case budget is expressed in time, elapsedTimer includes the time remaining until this
     *                     time is exhausted.
     */
    @Override
    void mctsSearch(ElapsedCpuTimer elapsedTimer) {
        //Some auxiliary variables to manage different budget expirations
        long remaining;
        int numIters = 0;
        int remainingLimit = 2; //Safe time threshold for time budget.

        //Run Collapse-MCTS in a loop until termination condition.
        boolean stop = false;
        while(!stop){

            //Start from a copy of the game state
            GameState state = rootState.copy();

            //1. Selection and 2. Expansion are executed in treePolicy(state)
            CollapsingTreeNode selected = collapseTreePolicy(state);
            //3. Simulation - rollout
            double delta = selected.rollOut(state);
            //4. Back-propagation
            backUp(selected, delta);

            //Stopping condition: it can be time, number of iterations or uses of the forward model.
            //For each case, update counts to determine if we must stop.
            if(params.stop_type == params.STOP_TIME) {
                remaining = elapsedTimer.remainingTimeMillis();
                stop = remaining <= remainingLimit;
            }else if(params.stop_type == params.STOP_ITERATIONS) {
                numIters++;
                stop = numIters >= params.num_iterations;
            }else if(params.stop_type == params.STOP_FMCALLS)
            {
                fmCallsCount+=params.rollout_depth;
                stop = (fmCallsCount + params.rollout_depth) > params.num_fmcalls;
            }
        }

    }

    /**
     * Performs the tree policy of Collapse-MCTS.
     * Navigates down the tree selecting nodes using UCT, until a not-fully expanded
     * node is reach. Then, it starts the call to expand it.
     * @param state Current state to do the policy from.
     * @return the expanded node.
     */
    @Override
    private CollapsingTreeNode collapseTreePolicy(GameState state) {

        //'cur': our current node in the tree.
        CollapsingTreeNode cur = this;

        //We keep going down the tree as long as the game is not over and we haven't reached the maximum depth
        while (!state.isTerminal() && cur.m_depth < params.rollout_depth)
        {
            //If not fully expanded, expand this one.
            if (cur.notFullyExpanded()) {
                // expand it all:
                SingleTreeNode[] expandedChildren = cur.expandAll(state);
                // compute clusters,
                // based on the similarity of the vectors
                // of heuristic scores:
                cur.computeClusters(expandedChildren);
                // update the action sampling distribution:
                cur.updateActionSamplingDistribution();
                // select the cluster representative according to uct:
                return cur.uctOnRepresentatives(state);

            } else {
                //If fully expanded, apply UCT to pick one of the children of 'cur'
                cur = cur.uctOnRepresentatives(state);
            }
        }

        //This one is the node to start the rollout from.
        return cur;
    }

    /**
     * Compute the clusters for the Collapse-MCTS iteration of the expansion part.
     * @param expandedChildren SingleTreeNode[] that have just been expanded from their parent node.
     */
    private void computeClusters(SingleTreeNode[] expandedChildren)
    {
        List<GameState> gss = Arrays.stream(expandedChildren).map(child->child.nodeState).collect(Collectors.toList());

        // Initialization of the clusters for this SingleTreeNode:
        this.clusters = clusterer.generateClusters(gss, ClusteringHeuristicFunction);

        int nbrCluster = clusters.size();
        this.representativeChildren = new ArrayList<>();
        int countCluster = 0;
        // Initialise the array telling which cluster each child belongs to
        children2ClusterIdx = new int[children.length];
        for(int idxCluster = 0; idxCluster < clusters.size(); idxCluster++)
        {
            List<ClusteringResult> cluster = clusters.get(idxCluster);
            if(cluster.size() > 0)
            {
                List<Float> norms = new ArrayList<Float>();
                for (int i = 0; i < cluster.size(); i++) {
                    // Get the absolute index of the child that's in the cluster, and set it in children2ClusterIdx
                    children2ClusterIdx[cluster.get(i).getNodeIdx()] = idxCluster;
                    norms.add(euclidianNorm(cluster.get(i).getHeuristicScores()));
                }
                // Find the greatest:
                int idxReprInCluster = maxIndexInVector(norms);
                // Make the representative child of the cluster be the node
                // that scores the highest in terms of the chosen norm over
                // the heuristic vector:
                int idxReprInChildren = cluster.get(idxReprInCluster).getNodeIdx();
                SingleTreeNode repr = this.children[idxReprInChildren];
                this.representativeChildren.add(repr);
            }
        }
    }

    /**
     * Compute the euclidian norm (vector :--> float value) of a List<Float>.
     * @return float normValue.
     */
    private static float euclidianNorm(List<Float> vector)
    {
        float squared_sum= 0;
        for (Float aFloat : vector) {
            squared_sum += aFloat * aFloat;
        }
        return (float) Math.sqrt(squared_sum);
    }

    /**
     * Finds the maximal value in an array and return the index.
     * @return int Idx of max value in array.
     */
    private static int maxIndexInVector(List<Float> vector)
    {
        if(vector == null || vector.size() < 1 )
        {
            throw new IllegalArgumentException("Cannot find max index in an null/empty array.");
        }

        int maxIdx=0;
        float maxValue = vector.get(0);
        for(int i=0; i<vector.size(); i++)
        {
            float vi = vector.get(i);
            if( vi > maxValue)
            {
                maxValue = vi;
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    /**
     * Back propagation step of MCTS. Takes the value of a state and updates the accummulated reward on each
     * traversed node, using the parent link. Updates count visits as well. Also updates bounds of the rewards
     * seen so far.
     * @param node Node to start backup from. This node should be the one expanded in this iteration.
     * @param result Reward to back-propagate
     */
    protected void backUp(CollapsingTreeNode node, double result)
    {
        CollapsingTreeNode n = node;

        //Go up until n == null, which happens after updating the root.
        while(n != null)
        {
            n.nVisits++;                    //Another visit to this node (N(s)++)
            n.totValue += result;           //Accummulate result (computationally cheaper than having a running average).

            //Update the bounds.
            if (result < n.bounds[0]) {
                n.bounds[0] = result;
            }
            if (result > n.bounds[1]) {
                n.bounds[1] = result;
            }

            // If the representativeChildren of node n are initialized,
            // then we can update all the nodes' statistics to the statistics
            // of the representative node of the cluster they belong to:
            if(n.representativeChildren != null)
            {
                for(int idxCluster=0;idxCluster<n.representativeChildren.size();idxCluster++)
                {
                    SingleTreeNode repr = n.representativeChildren.get(idxCluster);
                    List<ClusteringResult> lcr = n.clusters.get(idxCluster);
                    for(int idxNodeInCluster=0;idxNodeInCluster<lcr.size();idxNodeInCluster++)
                    {
                        int idxChildNode = lcr.get(idxNodeInCluster).getNodeIdx();
                        n.children[idxChildNode].totValue = repr.totValue;
                        n.children[idxChildNode].nVisits= repr.nVisits;
                    }
                }
            }
            //Next one, the parent.
            n = (CollapsingTreeNode)n.parent;
        }
    }

    private void updateActionSamplingDistribution()
    {
        Float thisEuclidianNormHeuristicScore = euclidianNorm( ClusteringHeuristicFunction.apply(this.nodeState));
        Map<Integer, Float> new_weights = new HashMap<>(num_actions);
        for(int idxCluster=0;idxCluster<this.clusters.size();idxCluster++)
        {
            List<ClusteringResult> lcr = this.clusters.get(idxCluster);
            for(ClusteringResult cr: lcr)
            {
                // Update towards good and novel states:
                Integer actionIdx = cr.getNodeIdx();
                Float novelty = euclidianNorm(cr.getHeuristicScores()) - thisEuclidianNormHeuristicScore;
                Float new_action_sampling_unnorm_weight= novelty;
                new_weights.put(actionIdx, new_action_sampling_unnorm_weight);
            }
        }
        actionSampler.updateWeights(new_weights);
    }

    /**
     * Performs UCT in a node. Selects the action to follow during the tree policy.
     * @param state
     * @return
     */
    private SingleTreeNode uctOnRepresentatives(GameState state) {

        //We'll pick the action with the highest UCB1 value.
        SingleTreeNode selected = null;
        double bestValue = -Double.MAX_VALUE;
        for (SingleTreeNode representative : this.representativeChildren)
        {
            //For each representative, calculate the different parts. First, exploitation:
            double hvVal = representative.totValue;
            double reprValue =  hvVal / (representative.nVisits + params.epsilon);  //Use epsilon to avoid /0.

            //Normalize rewards between 0 and 1 for the exploitation term (allows use of sqrt(2) as balance constant K
            double exploit = reprValue = Utils.normalise(reprValue, bounds[0], bounds[1]);
            double explore = Math.sqrt(Math.log(this.nVisits + 1) / (representative.nVisits + params.epsilon)); //Note we can use representative.nVisits for N(s,a)

            //UCB1!
            double uctValue = exploit + params.K * explore;

            //Little trick: in case there are ties of values, add some little random noise to it to break ties
            uctValue = Utils.noise(uctValue, params.epsilon, this.m_rnd.nextDouble());

            // keep the best one.
            if (uctValue > bestValue) {
                selected = representative;
                bestValue = uctValue;
            }
        }

        if (selected == null)
        {
            //This would be odd, but can happen if we reach a tree with no children. That probable means ERROR.
            throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.representativeChildren.size() + " " +
                    + bounds[0] + " " + bounds[1]);
        }

        //We need to roll the state, using the Forward Model, to keep going down the tree.
        roll(state, actions[selected.childIdx]);

        //Return the selected node to continue the Selection phase.
        return selected;
    }
}
