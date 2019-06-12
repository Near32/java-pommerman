package utils.Clustering;

import core.ForwardModel;
import core.GameState;
import org.christopherfrantz.dbscan.DBSCANClusterer;
import org.christopherfrantz.dbscan.DBSCANClusteringException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


import static utils.Clustering.KMeansStateClusterer.findVectorDistance;
import static utils.Clustering.KMeansStateClusterer.normaliseVectors;
import static utils.Clustering.KMeansStateClusterer.reformatIndicies;

public class DBScannClusterer implements Clusterer{
  private final int maxElements;
  private final double maxDist;
  private Clusterer.DISTANCE_METRIC metric;
private Clusterer backupClusterer;
  private static double calculateDistance(VectorContainer x, VectorContainer b, Clusterer.DISTANCE_METRIC metric) {
    return findVectorDistance(x.vector, b.vector,metric);
  }

  static class VectorContainer {
    List<Float> vector;
    public  int index;

    VectorContainer(int index, List<Float> vector) {
      this.vector = vector;
      this.index = index;
    }
  }


  public DBScannClusterer(int maxElements, double maxDist,Clusterer.DISTANCE_METRIC metric,  Clusterer backupClusterer)  {
    this.maxElements = maxElements;
    this.maxDist = maxDist;

    this.metric = metric;
    this.backupClusterer = backupClusterer;
  }

  public List<List<ClusteringResult>> generateClusters(List<GameState> gamestates, Function<GameState, List<Float>> heuristicFunction) {
    List<List<Float>> heuristicVectors = gamestates.stream().map(heuristicFunction).collect(Collectors.toList());
    int vectorLength = heuristicVectors.get(0).size();

    List<Float> min = new ArrayList<>();
    List<Float> max = new ArrayList<>();

    normaliseVectors(heuristicVectors, vectorLength, min, max);

    List<VectorContainer> containedVectors = IntStream.range(0, gamestates.size()).mapToObj(idx-> new VectorContainer(idx,heuristicVectors.get(idx))).collect(Collectors.toList());

    DBSCANClusterer<VectorContainer> clusterer = null;
    try {
      clusterer = new DBSCANClusterer<>(containedVectors,
              maxElements,
              maxDist,(a,b)-> DBScannClusterer.calculateDistance(a,b,metric));
      List<List<Integer>> clusterIndices = clusterer.performClustering()
              .stream()
              .map(s->s.stream()
                      .map(vc->vc.index)
                      .collect(Collectors.toList()))
              .collect(Collectors.toList());
      return reformatIndicies(heuristicVectors, clusterIndices);
    } catch (DBSCANClusteringException e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to dbscann");
    }
  }


}
