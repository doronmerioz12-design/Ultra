package branches;

/**
 * Observer representing Royal Navy / Admiralty Headquarters ({@link model.Branch#NAVY}).
 * Persists routed naval and maritime intelligence into the {@code NavyMes} database table.
 */
public class Navy extends AbstractBranchObserver {

    /**
     * Constructs the Navy HQ observer configured for the {@code NavyMes} table.
     */
    public Navy() {
        super("NavyMes", "Navy HQ",
                System.getenv().getOrDefault("NAVY_DB_USER", "Navy"),
                System.getenv().getOrDefault("NAVY_DB_PASSWORD", "change_me"));
    }
}
