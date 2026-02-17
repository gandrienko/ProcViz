import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class LogAnonymizer {

  public static void transformLog(String inputPath, String outputPath,
                                  String subMappingPath, String actorMappingPath) throws IOException {

    List<String[]> allLines = new ArrayList<>();
    Map<String, String> actorMap = new LinkedHashMap<>();
    Map<String, String> submissionMap = new LinkedHashMap<>();

    // --- PASS 1: Build Global Mappings ---
    try (BufferedReader reader = new BufferedReader(new FileReader(inputPath))) {
      String header = reader.readLine(); // Skip original header
      String line;
      int actorCounter = 1;
      int subCounter = 1;

      while ((line = reader.readLine()) != null) {
        // Split respecting quotes for parameters
        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        if (parts.length < 4) continue;

        String subId = parts[0].trim();
        String personId = parts[2].trim();
        String action = parts[3].trim();
        String params = parts.length > 4 ? parts[4].trim() : "";

        // 1) Skip lines where Action includes "submission"
        if (action.toLowerCase().contains("submission")) continue;

        // Store line for second pass
        allLines.add(parts);

        // 2) Map Submission ID
        if (!submissionMap.containsKey(subId)) {
          submissionMap.put(subId, String.format("Sub%03d", subCounter++));
        }

        // 3) Map Primary Actor (from "Person or anonymous ID" column)
        if (!personId.isEmpty() && !actorMap.containsKey(personId)) {
          actorMap.put(personId, String.format("A%04d", actorCounter++));
        }

        // 4) Scan Action AND Parameters for additional Actor IDs (e.g., 32-char hashes)
        // We scan both fields to ensure IDs mentioned in text are registered
        actorCounter = scanForIds(action, actorMap, actorCounter);
        actorCounter = scanForIds(params, actorMap, actorCounter);
      }
    }

    // Prepare actor keys for replacement (sort by length descending to avoid substring issues)
    List<String> sortedActorKeys = actorMap.keySet().stream()
        .sorted((a, b) -> Integer.compare(b.length(), a.length()))
        .collect(Collectors.toList());

    // --- PASS 2: Global Anonymization and Writing ---
    try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
      // New header as requested
      writer.println("Submission ID,Date (UTC),Actor,Action,Action parameters");

      for (String[] parts : allLines) {
        String subId = submissionMap.get(parts[0].trim());
        String date = parts[1].trim();
        String actor = actorMap.get(parts[2].trim());
        String action = parts[3].trim();
        String params = parts.length > 4 ? parts[4].trim() : "";

        // Replace all occurrences of registered actors in Action and Parameters
        for (String originalId : sortedActorKeys) {
          String replacement = actorMap.get(originalId);
          if (action.contains(originalId)) {
            action = action.replace(originalId, replacement);
          }
          if (params.contains(originalId)) {
            params = params.replace(originalId, replacement);
          }
        }

        writer.printf("%s,%s,%s,\"%s\",\"%s\"%n", subId, date, actor, action, params);
      }
    }

    // Write Mapping Tables
    writeMapping(subMappingPath, "Original SubID,Anonymized SubID", submissionMap);
    writeMapping(actorMappingPath, "Original Actor,Anonymized Actor", actorMap);
  }

  /**
   * Scans a string for 32-character hex IDs and adds them to the map if new.
   */
  private static int scanForIds(String text, Map<String, String> map, int counter) {
    if (text == null || text.isEmpty()) return counter;
    Matcher m = Pattern.compile("[a-f0-9]{32}").matcher(text);
    while (m.find()) {
      String id = m.group();
      if (!map.containsKey(id)) {
        map.put(id, String.format("A%04d", counter++));
      }
    }
    return counter;
  }

  private static void writeMapping(String path, String header, Map<String, String> map) throws IOException {
    try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
      writer.println(header);
      for (Map.Entry<String, String> entry : map.entrySet()) {
        writer.println(entry.getKey() + "," + entry.getValue());
      }
    }
  }

  public static void refineLog(String inputPath, String outputPath) throws IOException {
    // Pattern for emails: handles both "notification" and "invitation"
    Pattern emailPattern = Pattern.compile("emails (notification|invitation) to .*", Pattern.CASE_INSENSITIVE);

    // Pattern for role changes: Captures target role (group 2) and original role (group 3)
    Pattern rolePattern = Pattern.compile(
        "changes role of (.+?) to (external|secondary|primary) \\(was (external|secondary|primary)\\)",
        Pattern.CASE_INSENSITIVE
    );

    try (BufferedReader reader = new BufferedReader(new FileReader(inputPath));
         PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {

      String line = reader.readLine(); // Header
      if (line != null) writer.println(line);

      while ((line = reader.readLine()) != null) {
        if (line.trim().isEmpty()) continue;

        // FIX: If line ends with a comma and an opening quote, close it to prevent
        // the parser from swallowing subsequent lines.
        if (line.endsWith(",\"")) {
          line += "\"";
        }

        // Split by comma, respecting quotes
        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        if (parts.length < 4) {
          // If split fails due to formatting, write line as is or skip
          continue;
        }

        String subId = parts[0].trim();
        String date = parts[1].trim();
        String actor = parts[2].trim();
        String action = parts[3].trim().replace("\"", "");
        String params = (parts.length > 4) ? parts[4].trim().replace("\"", "") : "";

        // 1) Standardize Dates: Replace 'T' and truncate milliseconds
        // Handles: 2025-02-08T07:37:49.628000 -> 2025-02-08 07:37:49
        date = date.replace("T", " ");
        if (date.contains(".")) {
          date = date.substring(0, date.indexOf("."));
        }

        // 2) Replace "emails [type] to NNN" with generic form
        Matcher emailMatcher = emailPattern.matcher(action);
        if (emailMatcher.find()) {
          String emailType = emailMatcher.group(1); // notification or invitation
          if (emailType.equals("notification"))
            action = "emails " + emailType + " to author";
        }

        // 3) Transform "changes role" actions
        Matcher roleMatcher = rolePattern.matcher(action);
        if (roleMatcher.find()) {
          String roleTo = roleMatcher.group(2);   // e.g. secondary
          String roleFrom = roleMatcher.group(3); // e.g. external

          action = "changes role";
          params = roleFrom + " to " + roleTo;
        }

        // Write refined line back to CSV format
        // Fields are wrapped in quotes to handle any internal commas (like in batch assignments)
        writer.printf("%s,%s,%s,\"%s\",\"%s\"%n", subId, date, actor, action, params);
      }
    }
  }

  public static void separateActions(String inputPath, String outputPath) throws IOException {
    // Pattern 1: assigns [Actor] as [Role]
    Pattern assignPattern = Pattern.compile("assigns (A\\d{4}) as (external|secondary|primary)", Pattern.CASE_INSENSITIVE);

    // Pattern 2: emails invitation to [Actor]
    Pattern invitePattern = Pattern.compile("emails invitation to (A\\d{4})", Pattern.CASE_INSENSITIVE);

    // Pattern 3: set(s) decision to [Result]
    Pattern decisionPattern = Pattern.compile("sets? decision to (.*)", Pattern.CASE_INSENSITIVE);

    // Pattern 4: Action (Parenthetical Result/Mode)
    Pattern parenPattern = Pattern.compile("(.+?)\\s*\\((.+?)\\)", Pattern.CASE_INSENSITIVE);

    // State for deduplication
    String lastDecisionKey = "";

    try (BufferedReader reader = new BufferedReader(new FileReader(inputPath));
         PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {

      String header = reader.readLine();
      if (header != null) writer.println(header);

      String line;
      while ((line = reader.readLine()) != null) {
        // Split by comma, respecting quotes
        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        if (parts.length < 4) continue;

        String subId = parts[0].trim();
        String date  = parts[1].trim();
        String actor = parts[2].trim();
        String action = parts[3].trim().replace("\"", "");
        String params = (parts.length > 4) ? parts[4].trim().replace("\"", "") : "";

        // --- 1. Deduplication of "set decision" / "sets decision" ---
        Matcher decisionMatcher = decisionPattern.matcher(action);
        if (decisionMatcher.find()) {
          String decisionValue = decisionMatcher.group(1).trim();
          // Create a unique key for this event: SubID + Date + Actor + DecisionValue
          String currentKey = subId + "|" + date + "|" + actor + "|" + decisionValue;

          if (currentKey.equals(lastDecisionKey)) {
            continue; // Skip the second line in the pair
          }

          lastDecisionKey = currentKey;
          action = "sets decision";
          params = decisionValue;
        } else {
          // Reset decision key if the current line isn't a decision
          lastDecisionKey = "";

          // --- 2. "assigns [Actor] as [Role]" transformation ---
          Matcher assignMatcher = assignPattern.matcher(action);
          if (assignMatcher.find()) {
            String targetActor = assignMatcher.group(1);
            String role = assignMatcher.group(2);
            action = "assigns as " + role;
            params = targetActor;
          }
          // --- 3. Parentheses separation (e.g., "updates review (complete)") ---
          else {
            Matcher parenMatcher = parenPattern.matcher(action);
            if (parenMatcher.find()) {
              action = parenMatcher.group(1).trim();
              params = parenMatcher.group(2).trim();
            }
            else {
              Matcher inviteMatcher=invitePattern.matcher(action);
              if (inviteMatcher.find()) {
                action="emails invitation";
                params = inviteMatcher.group(1);
              }
            }
          }
        }

        // Write the cleaned and separated line
        writer.printf("%s,%s,%s,\"%s\",\"%s\"%n",
            subId, date, actor, action, params);
      }
    }
  }

  public static void main(String[] args) {
    String dirPath="c:\\CommonGISprojects\\events\\ProcessMining-Conf\\EV25\\";
    String origLogName="eurovis25f_submission_logs.csv";
    /*
    try {
      LogAnonymizer.transformLog(
          dirPath + origLogName,
          dirPath + "anonymized_log.csv",
          dirPath + "mapping_submissions.csv",
          dirPath + "mapping_actors.csv"
      );
    } catch (Exception ex) {
      System.out.println(ex.toString());
    }
    */
    try {
      //LogAnonymizer.refineLog(dirPath + "anonymized_log.csv",dirPath + "refined_log.csv");
      LogAnonymizer.separateActions(dirPath + "refined_log.csv",dirPath+"log_clean_actions.csv");
    } catch (Exception ex) {
      System.out.println(ex.toString());
    }
  }

//-------------------------------------------------------------------------
  public static void main_old(String[] args) {
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