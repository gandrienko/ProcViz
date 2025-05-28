package structures;

public class ActionType {
  public String typeName=null; //e.g. "accepts invitation", "updates review", "sets decision", ...
  public String phaseName=null; //e.g. "Assignment to External Reviewers", "Reviewing", "Discussion"
  public String actorRole=null; //e.g. "PC Member", "External Reviewer", "PC Member"
  public String targetType=null; //e.g. ""Actor", "Status", "Outcome"
  public String targetRole=null; //in case of targetType="Actor" it is the role of the target actor
  public String code=null;
}
