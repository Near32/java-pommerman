package utils.Clustering;

import core.GameState;

import java.util.List;
import java.util.function.Function;

public interface Clusterer {


  //TODO : new class to contain the list of list of map of idx of children and heuristic score associated.
  List<List<ClusteringResult>> generateClusters(List<GameState> gamestates, Function<GameState, List<Float>> heuristicFunction);

  public enum DISTANCE_METRIC {
    Euclidean(0),
    Manhattan(1),
    Eisen(2),
    Pearson(3);
    private int key;
    DISTANCE_METRIC(int numVal) {  this.key = numVal;  }
    public int getKey() {  return key; }

    public static DISTANCE_METRIC fromInt(int value) {
      switch (value){
        case  1:
          return Manhattan;
        case  2:
          return Eisen;
        case 3:
          return Pearson;
        default:
          return Euclidean;
      }
    }
  }
}
