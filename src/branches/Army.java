package branches;

public class Army extends AbstractBranchObserver {

    public Army() {
        super("ArmyMes", "Army HQ",
                System.getenv().getOrDefault("ARMY_DB_USER", "Army"),
                System.getenv().getOrDefault("ARMY_DB_PASSWORD", "Army123"));
    }
}
