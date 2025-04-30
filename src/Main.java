import data.LogLoader;
import structures.Actor;
import structures.ProcessInstance;

import java.io.*;
import java.util.Collection;
import java.util.*;

public class Main {

    public static void main(String[] args) {
      LogLoader loader = new LogLoader();
      String phasesFilePath="c:\\CommonGISprojects\\events\\ProcessMining-Conf\\Timeline25.csv";
      String actionsMappingFilePath="c:\\CommonGISprojects\\events\\ProcessMining-Conf\\actions2phases.csv";
      String actorsMappingFilePath="c:\\CommonGISprojects\\events\\ProcessMining-Conf\\actions2roles.csv";
      String logFilePath = "c:\\CommonGISprojects\\events\\ProcessMining-Conf\\conf25log.csv";

      try {
        loader.loadPhaseTimetable(phasesFilePath);
        loader.loadActionPhaseMapping(actionsMappingFilePath);
        loader.loadActionToRolesMapping(actorsMappingFilePath);
        loader.loadLog(logFilePath);

        System.out.println("Loaded action types:");
        for (String action : loader.getActionTypes()) {
            System.out.println(" - " + action);
        }

        System.out.println("Loaded phases: " + loader.getPhases().keySet());
        for (String phase : loader.getPhases().keySet()) {
          System.out.println(" - " + phase);
        }

        System.out.println("Action Type to Phase Mapping:");
        for (Map.Entry<String, String> entry : loader.getActionToPhaseMap().entrySet()) {
          System.out.println(entry.getKey() + " => " + entry.getValue());
        }

        System.out.println("Action Type to Actor Roles Mapping:");
        for (Map.Entry<String, String> entry : loader.getActionToActorMap().entrySet()) {
          System.out.println(entry.getKey() + " => " + entry.getValue());
        }

        if (loader.getActorRoles()!=null && !loader.getActorRoles().isEmpty()) {
          System.out.println("Loaded roles of actors:");
          for (String role : loader.getActorRoles()) {
            System.out.println("ROLE \"" + role+"\":");
            Set<Actor> actors=loader.getActorsByRole(role);
            for (Actor actor:actors)
              System.out.println(actor.id);
          }
        }

        Collection<ProcessInstance> processes=loader.getProcesses();

        System.out.println("Total number of processes loaded: " + processes.size());

      } catch (IOException e) {
        System.err.println("Error reading the log file: " + e.getMessage());
      }
    }
}
