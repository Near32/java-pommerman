package utils.Clustering;

import core.GameState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import utils.Clustering.ClusteringResult;
import utils.Types;

public class KMeansStateClusterer {
  private Integer meansExpected;
  private Random random = new Random();
  private Integer cycles;

  public static void main(String[] args) throws Exception {
    KMeansStateClusterer clusterer = new KMeansStateClusterer(2,4);
    List<GameState> gss = new ArrayList<GameState>();
    long seed = 1;
    int size = 11;
    Types.GAME_MODE gameMode = Types.GAME_MODE.FFA;
    for(int i = 0; i < 6; i++) {
      gss.add(new GameState(seed,size,gameMode));
    }

    List<List<ClusteringResult>> clusters = clusterer.generateClusters(gss, gs->{
      List<Float> fake = new ArrayList<>();
      for(int i = 0; i < 2; i++) {
        fake.add(clusterer.RandomWithinRange(0,1));
      }
      System.out.println("Generated Thing: " + printVector(fake));

      return fake;
    });

    System.out.println("Amount of clusters: " + clusters.size());
    for(int i = 0; i < clusters.size(); i++ ) {
      System.out.println("Cluster " + i);
        System.out.println("Generated Thing: " + printIntVector(clusters.get(i)));
      
    }
  }

  private static String printIntVector(List<ClusteringResult> integers) {
    return integers.stream().map(Object::toString).collect(Collectors.joining(","));
  }

  private static String printVector(List<Float> vector) {
    return vector.stream().map(Object::toString).collect(Collectors.joining(","));
  }

  public KMeansStateClusterer(Integer meansExpected, Integer cycles) {
    this.meansExpected = meansExpected;
    this.cycles = cycles;
  }

  private float RandomWithinRange(float min, float max) {
    float next = random.nextFloat() * (max - min);
    return min + next;
  }

  //TODO : new class to contain the list of list of map of idx of children and heuristic score associated.
  public List<List<ClusteringResult>> generateClusters(List<GameState> gamestates, Function<GameState, List<Float>> heuristicFunction) {
    List<List<Float>> heuristicVectors = gamestates.stream().map(heuristicFunction).collect(Collectors.toList());

    int vectorLength = heuristicVectors.get(0).size();

    List<Float> min = new ArrayList<>();
    List<Float> max = new ArrayList<>();

    normaliseVectors(heuristicVectors, vectorLength, min, max);


    //Initialise the means
    List<List<Float>> mean = new ArrayList<>();
    for (int i = 0; i < meansExpected; i++) {
      mean.add(new ArrayList<>());
    }

    //Initialise the random clusters
    for (int i = 0; i < vectorLength; i++) {
      for (int j = 0; j < meansExpected; j++) {
        mean.get(j).add(RandomWithinRange(0, 1));
      }
    }

    List<List<Integer>> clusterIndices = null;
    // obviously, cycles >= 1, so clusterIndices will have a value...
    for (int i = 0; i < cycles; i++) {
      // Compute the cluster, one time.
      clusterIndices = generateClusters(mean, heuristicVectors);
      // re-evaluate the barycentres of each cluster:
      mean = calculateClusterMeans(clusterIndices, heuristicVectors);
    }

    // Re-format the output:
    List<List<ClusteringResult>> clusters = new ArrayList<>();
    for (List<Integer> li: clusterIndices)
    {
      List<ClusteringResult> lcr = new ArrayList<>();
      for (Integer nodeIdx: li)
      {
        ClusteringResult cr = new ClusteringResult(nodeIdx, heuristicVectors.get(nodeIdx));
        lcr.add(cr);
      }
      clusters.add(lcr);
    }


    return clusters;

  }

  private List<List<Float>> calculateClusterMeans(List<List<Integer>> clusters, List<List<Float>> vectors) {
    List<List<Float>> means = new ArrayList<>();
    for (List<Integer> cluster : clusters) {
      List<Float> mean = new ArrayList<>();
      for (int j = 0; j < vectors.get(0).size(); j++) {
        mean.add((float) 0);
      }

      for (Integer vectorIndex : cluster) {
        for (int i = 0; i < vectors.get(vectorIndex).size(); i++) {
          mean.set(i, mean.get(i) + vectors.get(vectorIndex).get(i));
        }
      }

      for (int j = 0; j < vectors.get(0).size(); j++) {
        mean.set(j, mean.get(j) / cluster.size());
      }
      means.add(mean);
    }
    return means;
  }

  private List<List<Integer>> generateClusters(List<List<Float>> mean, List<List<Float>> heuristicVectors) {
    List<List<Integer>> result = new ArrayList<>();
    for (int i = 0; i < mean.size(); i++) {
      result.add(new ArrayList<>());
    }

    for (int i = 0; i < heuristicVectors.size(); i++) {
      float closest = Integer.MAX_VALUE;
      int closestMeanIndex = -1;
      for (int j = 0; j < mean.size(); j++) {
        float distance = findVectorDistance(mean.get(j), heuristicVectors.get(i));
        if (distance < closest) {
          closest = distance;
          closestMeanIndex = j;
        }
      }
      result.get(closestMeanIndex).add(i);
    }
    return result;
  }

  public static float findVectorDistance(List<Float> vector1, List<Float> vector2) {
    float dist = 0;
    for (int i = 0; i < vector1.size(); i++) {
      float diff = (vector1.get(i) - vector2.get(i));
      dist += diff * diff;
    }
    return (float) Math.sqrt(dist);
  }

  public static void normaliseVectors(List<List<Float>> heuristicVectors, int vectorLength, List<Float> min, List<Float> max) {
    for (int i = 0; i < vectorLength; i++) {
      float minVal = Integer.MAX_VALUE;
      float maxVal = Integer.MIN_VALUE;
      for (List<Float> heuristicVector : heuristicVectors) {
        float value = heuristicVector.get(i);
        minVal = Math.min(value, minVal);
        maxVal = Math.max(value, maxVal);
      }
      min.add(minVal);
      max.add(maxVal);
    }

    //Normalise
    for (int i = 0; i < vectorLength; i++) {
      float difference = max.get(i) - min.get(i);

      for (List<Float> heuristicVector : heuristicVectors) {
        if (difference == 0) {
          heuristicVector.set(i, 0f);
          continue;
        }


        float value = heuristicVector.get(i);
        value -= min.get(i);
        value /= difference;
        heuristicVector.set(i, value);
      }
    }
  }
}
