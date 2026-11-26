package branches;

/**
 * Observer representing British Army Headquarters ({@link model.Branch#ARMY}).
 * Persists routed ground-forces intelligence into the {@code ArmyMes} database table.
 */
public class Army extends AbstractBranchObserver {

    /**
     * Constructs the Army HQ observer configured for the {@code ArmyMes} table.
     */
    public Army() {
        super("ArmyMes", "Army HQ",
                System.getenv().getOrDefault("ARMY_DB_USER", "Army"),
                System.getenv().getOrDefault("ARMY_DB_PASSWORD", "change_me"));
    }
}
