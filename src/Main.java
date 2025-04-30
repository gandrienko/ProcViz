import data.LogLoader;
import structures.ProcessInstance;

import java.io.*;
import java.util.Collection;

public class Main {

    public static void main(String[] args) {
      LogLoader loader = new LogLoader();
      String logFilePath = "c:\\CommonGISprojects\\events\\ProcessMining-Conf\\conf25log.csv"; // Adjust path as needed

      try {
        loader.loadLog(logFilePath);

        System.out.println("Loaded action types:");
        for (String action : loader.getActionTypes()) {
            System.out.println(" - " + action);
        }

        Collection<ProcessInstance> processes=loader.getProcesses();

        System.out.println("Total number of processes loaded: " + processes.size());

      } catch (IOException e) {
        System.err.println("Error reading the log file: " + e.getMessage());
      }
    }
}
