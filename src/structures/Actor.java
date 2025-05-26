package structures;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a PC member, reviewer, chair, or external actor (in other scenarios).
 */

public class Actor implements Comparable<Actor>{
  public static List<String> rolePriorities=null;
  
  public String id;
  public Map<String,Integer> roles=null;
  public LocalDateTime start;
  public LocalDateTime end;

  public Actor(String id) {
    this.id = id;
  }

  public void addRole(String role) {
    if (role==null)
      return;
    if (roles==null)
      roles=new HashMap<String,Integer>(5);
    Integer count=roles.get(role);
    if (count==null)
      roles.put(role,1);
    else
      roles.put(role,count+1);
  }

  public String getMainRole() {
    if (roles==null || roles.isEmpty())
      return null;
    if (rolePriorities!=null)
      for (int i=0; i<rolePriorities.size(); i++)
        if (roles.containsKey(rolePriorities.get(i)))
          return rolePriorities.get(i);
    String mainRole=null;
    int maxCount=0;
    for (Map.Entry<String,Integer> e:roles.entrySet())
      if (e.getValue()>maxCount && !e.getKey().equalsIgnoreCase("any")) {
        maxCount=e.getValue();
        mainRole=e.getKey();
      }
    return mainRole;
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
