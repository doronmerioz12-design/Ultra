package branches;

/**
 * Observer representing Supreme Headquarters Allied Expeditionary Force ({@link model.Branch#SHAEF}).
 * Persists joint Allied theatre-level intelligence into the {@code SHAEFMes} database table.
 */
public class SHAEF extends AbstractBranchObserver {

    /**
     * Constructs the SHAEF HQ observer configured for the {@code SHAEFMes} table.
     */
    public SHAEF() {
        super("SHAEFMes", "SHAEF HQ",
                System.getenv().getOrDefault("SHAEF_DB_USER", "SHAEF"),
                System.getenv().getOrDefault("SHAEF_DB_PASSWORD", "change_me"));
    }
}
