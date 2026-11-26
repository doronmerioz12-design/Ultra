package branches;

/**
 * Observer representing Royal Air Force Headquarters ({@link model.Branch#RAF}).
 * Persists routed aerial and Luftwaffe intelligence into the {@code RAFMes} database table.
 */
public class RAF extends AbstractBranchObserver {

    /**
     * Constructs the RAF HQ observer configured for the {@code RAFMes} table.
     */
    public RAF() {
        super("RAFMes", "RAF HQ",
                System.getenv().getOrDefault("RAF_DB_USER", "RAF"),
                System.getenv().getOrDefault("RAF_DB_PASSWORD", "change_me"));
    }
}
