package branches;

/**
 * Observer representing the Prime Minister's Office ({@link model.Branch#PM_OFFICE}).
 * Persists high-level strategic and diplomatic intelligence into the {@code PMMes} database table.
 */
public class PMOFFICE extends AbstractBranchObserver {

    /**
     * Constructs the PM Office observer configured for the {@code PMMes} table.
     */
    public PMOFFICE() {
        super("PMMes", "PM HQ",
                System.getenv().getOrDefault("PMOFFICE_DB_USER", "PMOFFICE"),
                System.getenv().getOrDefault("PMOFFICE_DB_PASSWORD", "change_me"));
    }
}
