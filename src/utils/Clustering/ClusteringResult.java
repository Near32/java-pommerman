package utils.Clustering;


import java.util.List;

public class ClusteringResult
{
    private int nodeIdx;
    private List<Float> heuristicScores;

    ClusteringResult(int nodeIdx, List<Float> hScores)
    {
        this.nodeIdx = nodeIdx;
        this.heuristicScores = hScores;
    }

    public int getNodeIdx()
    {
        return this.nodeIdx;
    }

    public List<Float> getHeuristicScores()
    {
        return this.heuristicScores;
    }

    @Override
    public String toString() {
        return heuristicScores.toString();
    }
}