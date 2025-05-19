import data.LogLoader;
import structures.ActionType;
import structures.Actor;
import structures.GlobalProcess;
import viz.ProcessTimelinePanel;
import viz.TimelinePanel;
import viz.TimelineTextsPanel;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {
      LogLoader loader = new LogLoader();
      /**/
      String phasesFilePath="c:\\CommonGISprojects\\events\\ProcessMining-Conf\\Timeline25.csv";
      String actionsMappingFilePath="c:\\CommonGISprojects\\events\\ProcessMining-Conf\\actions2phases.csv";
      String actorsMappingFilePath="c:\\CommonGISprojects\\events\\ProcessMining-Conf\\actions2roles.csv";
      String logFilePath = "c:\\CommonGISprojects\\events\\ProcessMining-Conf\\conf25log.csv";
      /**/
      /*
      String phasesFilePath="c:\\CommonGISprojects\\events\\ProcessMining-Conf\\synthetic\\Timeline25.csv";
      String actionsMappingFilePath="c:\\CommonGISprojects\\events\\ProcessMining-Conf\\synthetic\\actions2phases.csv";
      String actorsMappingFilePath="c:\\CommonGISprojects\\events\\ProcessMining-Conf\\synthetic\\actions2roles.csv";
      String logFilePath = "c:\\CommonGISprojects\\events\\ProcessMining-Conf\\synthetic\\object_centric_log.csv";
      /**/

      GlobalProcess gProc=new GlobalProcess();

      try {
        loader.loadPhaseTimetable(phasesFilePath);
        loader.loadActionPhaseMapping(actionsMappingFilePath);
        loader.loadActionToRolesMapping(actorsMappingFilePath);
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
        ProcessTimelinePanel processMainPanel = new ProcessTimelinePanel(gProc);

        JFrame frame = new JFrame("Process Timeline");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(processMainPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);

        JPanel topPanel=new TimelineTextsPanel(processMainPanel,TimelineTextsPanel.SHOW_TITLES);
        JPanel processPanel=new JPanel();
        processPanel.setLayout(new BorderLayout());
        processPanel.add(topPanel,BorderLayout.NORTH);
        processPanel.add(scrollPane, BorderLayout.CENTER);

        frame.add(processPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.add(new JLabel("Interaction controls coming soon..."));
        frame.add(controlPanel, BorderLayout.SOUTH);

        frame.pack();
        frame.setSize(frame.getWidth()+20, 850);
        frame.setVisible(true);
      }
    }
}
