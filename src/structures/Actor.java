package structures;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a PC member, reviewer, chair, or external actor (in other scenarios).
 */

public class Actor implements Comparable<Actor>{
  public static List<String> rolePriorities=null;
  
  public String id;
  public String generalRole=null;

  // Assignments of actor roles in different process instances; key: ID of a process instance; value: role
  public Map<String,String> processRoleAssignments=new LinkedHashMap<>();

  public LocalDateTime start;
  public LocalDateTime end;

  public Actor(String id) {
    this.id = id;
  }

  public boolean hasProcessRole(String role) {
    return processRoleAssignments.containsValue(role);
  }

  public int compareTo(Actor a) {
    if (a==null) return -1;
    if (start==null)
      if (a.start==null)
        return id.compareTo(a.id);
      else
        return 1;
    if (a.start.equals(start))
      if (a.end.equals(end))
        return id.compareTo(a.id);
      else
        return (end.isBefore(a.end))?-1:1;
    return (start.isBefore(a.start))?-1:1;
  }
}
