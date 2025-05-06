package structures;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a PC member, reviewer, chair, or external actor (in other scenarios).
 */

public class Actor {
  public String id;
  public Map<String,Integer> roles=null;

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
    String mainRole=null;
    int maxCount=0;
    for (Map.Entry<String,Integer> e:roles.entrySet())
      if (e.getValue()>maxCount && !e.getKey().equalsIgnoreCase("any")) {
        maxCount=e.getValue();
        mainRole=e.getKey();
      }
    return mainRole;
  }
}
