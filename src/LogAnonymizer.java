import java.io.*;
import java.util.*;

public class LogAnonymizer {

  public static void main(String[] args) {
    String dir="c:\\CommonGISprojects\\events\\ProcessMining-Conf\\new\\";
    String inputFile = "c25f_log_transformed.csv";
    String outputFile = "c25_log_anonymized.csv";
    String submissionMapFile = "submission_id_map.csv";
    String actorMapFile = "actor_id_map.csv";

    Map<String, String> submissionMap = new LinkedHashMap<>();
    Map<String, String> actorMap = new LinkedHashMap<>();

    Random rndSub=new Random(), rndActor=new Random();
    HashSet<Integer> usedSubIds=new HashSet<Integer>(300),
        usedActorIds=new HashSet<Integer>(300);

    try {
      BufferedReader reader = new BufferedReader(new FileReader(dir+inputFile));
      BufferedWriter writer = new BufferedWriter(new FileWriter(dir+outputFile));

      String header = reader.readLine();
      writer.write(header);
      writer.newLine();

      String line;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split(",", -1); // handle empty trailing fields

        if (parts.length < 4) continue;

        // Process Submission ID
        String originalSubId = parts[0].trim(), newSubId=submissionMap.get(originalSubId);
        if (newSubId==null) {
          int num=rndSub.nextInt(9998)+1;
          while (usedSubIds.contains(num))
            num=rndSub.nextInt(9998)+1;
          newSubId = String.format("Sub%04d", num);
          submissionMap.put(originalSubId,newSubId);
        }

        // Process Actor ID (Person or anonymous ID)
        String originalActorId = parts[2].trim();
        String newActorId = actorMap.get(originalActorId);
        if (newActorId==null) {
          int num=rndActor.nextInt(9998)+1;
          while (usedActorIds.contains(num))
            num=rndActor.nextInt(9998)+1;
          newActorId=String.format("A%04d", num);
          actorMap.put(originalActorId,newActorId);
        }
        String param="", newParam="";
        if (parts.length>4) {
          // Process Action Parameter (may be actor ID)
          param = parts[4].trim();
          newParam = param;
          if (isLikelyHash(param)) {
            newParam=actorMap.get(param);
            if (newParam==null) {
              int num=rndActor.nextInt(9998)+1;
              while (usedActorIds.contains(num))
                num=rndActor.nextInt(9998)+1;
              newParam=String.format("A%04d", num);
              actorMap.put(param,newParam);
            }
          }
        }

        // Write transformed line
        writer.write(String.join(",", newSubId, parts[1], newActorId, parts[3], newParam));
        writer.newLine();
      }
      writer.close();

    } catch (IOException e) {
      System.err.println("Error processing file: " + e.getMessage());
    }

    // Write submission ID map
    try (BufferedWriter subWriter = new BufferedWriter(new FileWriter(dir+submissionMapFile))) {
      subWriter.write("Original,Anonymized");
      subWriter.newLine();
      for (Map.Entry<String, String> entry : submissionMap.entrySet()) {
        subWriter.write(entry.getKey() + "," + entry.getValue());
        subWriter.newLine();
      }
      subWriter.close();
    } catch (IOException e) {
      System.err.println("Error writing submission ID map: " + e.getMessage());
    }

    // Write actor ID map
    try (BufferedWriter actorWriter = new BufferedWriter(new FileWriter(dir+actorMapFile))) {
      actorWriter.write("Original,Anonymized");
      actorWriter.newLine();
      for (Map.Entry<String, String> entry : actorMap.entrySet()) {
        actorWriter.write(entry.getKey() + "," + entry.getValue());
        actorWriter.newLine();
      }
      actorWriter.close();
    } catch (IOException e) {
      System.err.println("Error writing actor ID map: " + e.getMessage());
    }

    System.out.println("Anonymization complete.");
  }

  public static boolean isLikelyHash(String s) {
    if (s==null || s.length()<32 || s.contains(" "))
      return false;
    return s != null && s.matches("^[a-fA-F0-9]{32}$");
  }
}