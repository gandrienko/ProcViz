import data.LogLoader;
import data.StatusChecker;
import structures.*;
import viz.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.*;
import java.util.*;

import static data.LogLoader.transformCsv;

public class Main {

    public static void main(String[] args) {

      String configFile = (args.length > 0) ? args[0] : "paths.txt";
      String phasesFilePath = null;
      String actionsMappingFilePath = null;
      String actionsEncodingFilePath = null;
      String actorsMappingFilePath = null;
      String logFilePath = null;

      try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
        String line;
        while ((line = reader.readLine()) != null) {
          line = line.trim();
          if (line.isEmpty() || line.startsWith("#")) continue; // skip empty or comment lines

          String[] parts = line.split("=", 2);
          if (parts.length < 2) continue;

          String key = parts[0].trim();
          String value = parts[1].trim().replaceAll("^\"|\"$", ""); // remove surrounding quotes

          switch (key) {
            case "phasesFilePath":
              phasesFilePath = value;
              break;
            case "actionsMappingFilePath":
              actionsMappingFilePath = value;
              break;
            case "actionsEncodingFilePath":
              actionsEncodingFilePath = value;
              break;
            case "actorsMappingFilePath":
              actorsMappingFilePath = value;
              break;
            case "logFilePath":
              logFilePath = value;
              break;
            default:
              System.out.println("Unknown key: " + key);
          }
        }
      } catch (IOException e) {
        System.err.println("Error reading configuration file: " + e.getMessage());
        return;
      }

      // Print to verify values (or use them as needed)
      System.out.println("phasesFilePath = " + phasesFilePath);
      System.out.println("actionsMappingFilePath = " + actionsMappingFilePath);
      System.out.println("actionsEncodingFilePath = " + actionsEncodingFilePath);
      System.out.println("actorsMappingFilePath = " + actorsMappingFilePath);
      System.out.println("logFilePath = " + logFilePath);

      LogLoader loader = new LogLoader();
      GlobalProcess gProc=new GlobalProcess();

      try {
        loader.loadPhaseTimetable(phasesFilePath);
        loader.loadActionPhaseMapping(actionsMappingFilePath);
        loader.loadActionToRolesMapping(actorsMappingFilePath);
        loader.loadActionEncodings(actionsEncodingFilePath);
        loader.loadLog(logFilePath);

        gProc.actionTypes=loader.getActionTypes();
        gProc.phases=loader.getPhases();
        gProc.actorRoles=loader.getActorRoles();
        gProc.actors=loader.getActors();

        System.out.println("Loaded action types:");
        for (String action : gProc.actionTypes.keySet()) {
            System.out.println(" - " + action);
        }

        System.out.println("Loaded phases:");
        for (String phase : gProc.phases.keySet()) {
          System.out.println(" - " + phase);
        }

        System.out.println("Action Type to Phase Mapping:");
        for (Map.Entry<String, ActionType> entry : gProc.actionTypes.entrySet()) {
          System.out.println(entry.getKey() + " => " + entry.getValue().phaseName);
        }

        System.out.println("Action Type to Actor Roles Mapping:");
        for (Map.Entry<String, ActionType> entry : gProc.actionTypes.entrySet()) {
          System.out.println(entry.getKey() + " => " + entry.getValue().actorRole);
        }
        /*
        if (loader.getActorRoles()!=null && !loader.getActorRoles().isEmpty()) {
          System.out.println("Loaded roles of actors:");
          for (String role : gProc.actorRoles) {
            System.out.println("ROLE \"" + role+"\":");
            Set<Actor> actors=gProc.getActorsByRole(role);
            for (Actor actor:actors)
              System.out.println(actor.id);
          }
        }
        */

        gProc.processes =loader.getProcesses();

        System.out.println("Total number of processes loaded: " + gProc.processes.size());

      } catch (IOException e) {
        System.err.println("Error reading the log file: " + e.getMessage());
      }
      if (gProc.phases!=null && !gProc.phases.isEmpty()) {
        StatusChecker checker=new StatusChecker(gProc);
        checker.determinePhaseCompletenessDates();

        SelectionManager selectionManager=new SelectionManager();
        JPanel processPanel=new ProcessTimelineWithControls(gProc,selectionManager);

        JFrame frame = new JFrame("Process Timeline");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(processPanel, BorderLayout.CENTER);

        frame.pack();
        frame.setSize(frame.getWidth(), 850);
        frame.setVisible(true);


        // 1. Initialize the panels
        TimelineWithHeaderAndFooter completenessPanel = new TimelineWithHeaderAndFooter(
            new PhaseCompletenessOverviewPanel(gProc,selectionManager),false);

        ActionOverviewPanel overviewPanel = new ActionOverviewPanel(gProc, selectionManager);
        JPanel topContainer=new JPanel();
        topContainer.setLayout(new BorderLayout());
        topContainer.add(completenessPanel,BorderLayout.CENTER);
        topContainer.add(Box.createRigidArea(new Dimension(overviewPanel.getScrollbarWidth(), 1)),
            BorderLayout.EAST);


        // 2. Create the Split Pane
        // VERTICAL_SPLIT places the first component on top and the second on the bottom
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topContainer, overviewPanel);

        // 3. Configure Split Pane behavior
        splitPane.setDividerLocation(250); // Set initial height of the upper panel in pixels
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);

        // 4. Setup the Frame
        JFrame overviewFrame = new JFrame("Process Overview & Phase Completeness");
        overviewFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        overviewFrame.add(splitPane);
        overviewFrame.setSize(1000, 850); // Increased height slightly to accommodate both
        overviewFrame.setVisible(true);
      }
    }

  public static void transformAssignmentActionText() {
    transformAssignmentActionText(
        "c:\\CommonGISprojects\\events\\ProcessMining-Conf\\c25f_submission_logs.csv",
        "c:\\CommonGISprojects\\events\\ProcessMining-Conf\\c25f_log_transformed.csv");
  }

  public static void transformAssignmentActionText(String inputPath,String outputPath) {
    transformCsv(inputPath, outputPath);
    System.out.println("CSV transformation complete.");
  }
}
