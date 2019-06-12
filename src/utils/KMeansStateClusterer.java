package utils;

import core.ForwardModel;

import java.io.Console;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class KMeansStateClusterer {
  private Integer meansExpected;
  Random random = new Random();
  Integer cycles;

  public static void main(String[] args) throws Exception {
    KMeansStateClusterer clusterer = new KMeansStateClusterer(2, 4);
    ForwardModel[] models = new ForwardModel[6];
    for (int i = 0; i < models.length; i++) {
      models[i] = new ForwardModel(true);
    }

    List<List<Integer>> clusters = clusterer.generateClusters(models, model -> {
      List<Float> fake = new ArrayList<>();
      for (int i = 0; i < 2; i++) {
        fake.add(clusterer.RandomWithinRange(0, 1));
      }
      System.out.println("Generated Thing: " + printVector(fake));

      return fake;
    });

    System.out.println("Amount of clusters: " + clusters.size());
    for (int i = 0; i < clusters.size(); i++) {
      System.out.println("Cluster " + i);
      System.out.println("Generated Thing: " + printIntVector(clusters.get(i)));

    }
  }

  private static String printIntVector(List<Integer> integers) {
    return String.join(",", integers.stream().map(Object::toString).collect(Collectors.toList()));
  }

  private static String printVector(List<Float> vector) {
    return String.join(",", vector.stream().map(Object::toString).collect(Collectors.toList()));
  }

  public KMeansStateClusterer(Integer meansExpected, Integer cycles) {
    this.meansExpected = meansExpected;
    this.cycles = cycles;
  }

  float RandomWithinRange(float min, float max) {
    float next = random.nextFloat() * (max - min);
    return min + next;
  }

  public List<List<Integer>> generateClusters(ForwardModel[] gamestates, Function<ForwardModel, List<Float>> heuristicFunction) {
    List<List<Float>> heuristicVectors = Arrays.stream(gamestates).map(heuristicFunction).collect(Collectors.toList());

    int vectorLength = heuristicVectors.get(0).size();

    List<Float> min = new ArrayList<>();
    List<Float> max = new ArrayList<>();

    normaliseVecors(heuristicVectors, vectorLength, min, max);


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

    List<List<Integer>> clusters = null;
    for (int i = 0; i < cycles; i++) {
      clusters = generateClusters(mean, heuristicVectors);
      mean = calculateClusterMeans(clusters, heuristicVectors);
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

  //Borrowed from https://stackoverflow.com/questions/28428365/how-to-find-correlation-between-two-integer-arrays-in-java
  public static float pearsonCorrelationDistance(List<Float> xs, List<Float> ys) {
    double sx = 0.0;
    double sy = 0.0;
    double sxx = 0.0;
    double syy = 0.0;
    double sxy = 0.0;

    int n = xs.size();

    for(int i = 0; i < n; ++i) {
      double x = xs.get(i);
      double y = ys.get(i);

      sx += x;
      sy += y;
      sxx += x * x;
      syy += y * y;
      sxy += x * y;
    }

    // covariation
    double cov = sxy / n - sx * sy / n / n;
    // standard error of x
    double sigmax = Math.sqrt(sxx / n -  sx * sx / n / n);
    // standard error of y
    double sigmay = Math.sqrt(syy / n -  sy * sy / n / n);

    // correlation is just a normalized covariation
    return  1-(float)(cov / sigmax / sigmay);
  }


  public static float eisenCosineCorrelationDistance(List<Float> vector1, List<Float> vector2) {
    float top = 0;
    float xsq = 0;
    float ysq = 0;
    for (int i = 0; i < vector1.size(); i++) {
      top += vector1.get(i) * vector2.get(i);
      xsq += Math.pow(vector1.get(i), 2);
      ysq += Math.pow(vector2.get(i), 2);
    }
    return 1.0f - ((float) (Math.abs(top) / Math.sqrt(xsq * ysq)));
  }

  public static float findVectorDistance(List<Float> vector1, List<Float> vector2) {
    int type = 0;
    float dist = 0;

    switch (type) {
      case 3:
        return pearsonCorrelationDistance(vector1, vector2);
      case 2:
        //EisenCosine Correlation
        return eisenCosineCorrelationDistance(vector1, vector2);
      case 1:
        //Manhattan distance
        for (int i = 0; i < vector1.size(); i++) {
          dist += Math.abs(vector1.get(i) - vector2.get(i));
        }
        return (dist);
      case 0:
      default:
        //Euclidean distance
        for (int i = 0; i < vector1.size(); i++) {
          float diff = (vector1.get(i) - vector2.get(i));
          dist += diff * diff;
        }
        return (float) Math.sqrt(dist);
    }
  }


  public static void normaliseVecors(List<List<Float>> heuristicVectors, int vectorLength, List<Float> min, List<Float> max) {
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
