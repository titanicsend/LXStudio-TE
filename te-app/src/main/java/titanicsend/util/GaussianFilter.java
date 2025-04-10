package titanicsend.util;

import java.util.Arrays;

/**
 * Utility class to apply a Gaussian filter on the input data.
 *
 * <p>This class takes a filterWidth parameter and the larger the width, the smoother the output
 * value will be.
 *
 * <p>Example usage: > GaussianFilter filter = new GaussianFilter(10); > double filteredValue =
 * filter.applyGaussianFilter(value);
 *
 * <p>Note: this filter needs to keep track of the values over time, so make sure to instantiate the
 * filter as a member variable.
 */
public class GaussianFilter {

  private double[] filterBuffer;
  private int filterWidth;
  private double filteredValue;

  public GaussianFilter(int filterWidth) {
    this.filterWidth = filterWidth;
    filterBuffer = new double[filterWidth];
    Arrays.fill(filterBuffer, 0.0f);
  }

  public double applyGaussianFilter(double rawValue) {
    System.arraycopy(filterBuffer, 1, filterBuffer, 0, filterWidth - 1);
    filterBuffer[filterWidth - 1] = rawValue;

    filteredValue = 0.0f;
    double[] weights = calculateGaussianWeights(filterWidth);
    for (int i = 0; i < filterWidth; i++) {
      filteredValue += filterBuffer[i] * weights[i];
    }
    return filteredValue;
  }

  public double getFilteredValue() {
    return filteredValue;
  }

  private double[] calculateGaussianWeights(int windowSize) {
    double[] weights = new double[windowSize];
    double sigma = windowSize / 3.0f;
    double sum = 0.0f;

    for (int i = 0; i < windowSize; i++) {
      double x = i - windowSize / 2.0f;
      weights[i] = (double) Math.exp(-0.5 * x * x / (sigma * sigma));
      sum += weights[i];
    }

    for (int i = 0; i < windowSize; i++) {
      weights[i] /= sum;
    }
    return weights;
  }
}
