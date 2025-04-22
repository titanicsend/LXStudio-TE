package titanicsend.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Utility class used to log signals to a CSV file every "intervalSeconds".
 *
 * <p>This class uses a list of signal names, then accepts an array of values for each of these
 * signals. The names will be written to the CSV files as the header, and the values will be logged
 * to the file over time.
 *
 * <p>To use this class, first add the SignalLogger as a class member > SignalLogger logger; //
 * Class member.
 *
 * <p>Then initialize the logger and provide it with the signal names and a file path. Then you need
 * to start the logging. > List<String> signalNames = Arrays.asList("signal_1", "signal_2"); >
 * logger = new SignalLogger(signalNames, "Logs/signal_data.csv"); > logger.startLogging(1); //
 * Write to CSV every 1s.
 *
 * <p>In your code, where you want to keep track of the signals, you can log the signals as it
 * follows: > List<Float> values = Arrays.asList(signal_1_value, signal_2_value); >
 * logger.logSignalValues(values);
 */
public class SignalLogger {
  private List<String> signalNames;
  private List<List<Float>> signalData;
  private String csvFilename;
  private Timer timer;
  private long startTime;
  private String header; // Store the header string

  public SignalLogger(List<String> signalNames, String csvFilename) {
    this.signalNames = signalNames;
    this.csvFilename = csvFilename;
    this.signalData = new ArrayList<>();
    for (int i = 0; i < signalNames.size(); i++) {
      signalData.add(new ArrayList<>());
    }
    this.timer = new Timer();

    // Create the header during initialization
    StringBuilder headerBuilder = new StringBuilder("Time (seconds)");
    for (String name : signalNames) {
      headerBuilder.append(",").append(name);
    }
    this.header = headerBuilder.toString() + "\n";
  }

  public void logSignalValues(List<Float> values) {
    if (values.size() != signalNames.size()) {
      throw new IllegalArgumentException("Number of values doesn't match signal names");
    }

    // Add values to the storage lists
    for (int i = 0; i < values.size(); i++) {
      signalData.get(i).add(values.get(i));
    }
  }

  /**
   * @param intervalMilliseconds Set to 10ms for an almost realtime signal.
   */
  public void startLogging(int intervalMilliseconds) {
    writeHeader();
    timer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            writeDataToCSV();
          }
        },
        0,
        intervalMilliseconds);
  }

  public void stopLogging() {
    timer.cancel();
  }

  private void writeHeader() {
    try (FileWriter writer = new FileWriter(csvFilename)) {
      writer.write(header);
    } catch (IOException e) {
      TE.error("Error writing CSV header: " + e.getMessage());
    }
  }

  private void writeDataToCSV() {
    try (FileWriter writer = new FileWriter(csvFilename, true)) {
      // Write data rows
      long currentTime = System.currentTimeMillis();
      for (int i = 0; i < signalData.get(0).size(); i++) {
        double elapsedSeconds = (currentTime - startTime) / 1000.0;
        StringBuilder row = new StringBuilder(String.format("%.3f", elapsedSeconds));
        for (List<Float> signalValues : signalData) {
          row.append(",").append(signalValues.get(i));
        }
        writer.write(row.toString() + "\n");
      }

      // Clear the stored data for the next interval
      for (List<Float> signalValues : signalData) {
        signalValues.clear();
      }

    } catch (IOException e) {
      TE.error("Error writing to CSV file: " + e.getMessage());
    }
  }
}
