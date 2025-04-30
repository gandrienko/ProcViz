package structures;

/**
 * Represents a PC member, reviewer, chair, or external actor (in other scenarios).
 */

public class Actor {
  public String id;
  public ActorRole role;

  public Actor(String id) {
    this.id = id;
  }
}
