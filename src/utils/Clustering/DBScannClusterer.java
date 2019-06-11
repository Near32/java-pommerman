package utils.Clustering;

import core.ForwardModel;
import org.christopherfrantz.dbscan.DBSCANClusterer;
import org.christopherfrantz.dbscan.DBSCANClusteringException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static utils.Clustering.KMeansStateClusterer.findVectorDistance;
import static utils.Clustering.KMeansStateClusterer.normaliseVecors;

public class DBScannClusterer {
  private final int maxElements;
  private final double maxDist;

  private static double calculateDistance(VectorContainer x, VectorContainer b) {
    return findVectorDistance(x.vector, b.vector);
  }

  static class VectorContainer {
    List<Float> vector;
    public  int index;

    VectorContainer(int index, List<Float> vector) {
      this.vector = vector;
      this.index = index;
    }
  }


  public DBScannClusterer(int maxElements, double maxDist)  {
    this.maxElements = maxElements;
    this.maxDist = maxDist;

  }

  public List<List<Integer>> generateClusters(ForwardModel[] gamestates, Function<ForwardModel, List<Float>> heuristicFunction) throws DBSCANClusteringException {
    List<List<Float>> heuristicVectors = Arrays.stream(gamestates).map(heuristicFunction).collect(Collectors.toList());
    int vectorLength = heuristicVectors.get(0).size();

    List<Float> min = new ArrayList<>();
    List<Float> max = new ArrayList<>();

    normaliseVecors(heuristicVectors, vectorLength, min, max);

    List<VectorContainer> containedVectors = IntStream.range(0, gamestates.length).mapToObj(idx-> new VectorContainer(idx,heuristicVectors.get(idx))).collect(Collectors.toList());

    DBSCANClusterer<VectorContainer> clusterer = new DBSCANClusterer<>(containedVectors,
            maxElements,
            maxDist,
            DBScannClusterer::calculateDistance);
    return clusterer.performClustering()
            .stream()
            .map(s->s.stream()
                    .map(vc->vc.index)
                    .collect(Collectors.toList()))
            .collect(Collectors.toList());

  }
}
