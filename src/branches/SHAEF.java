package branches;

public class SHAEF extends AbstractBranchObserver {

    public SHAEF() {
        super("SHAEFMes", "SHAEF HQ",
                System.getenv().getOrDefault("SHAEF_DB_USER", "SHAEF"),
                System.getenv().getOrDefault("SHAEF_DB_PASSWORD", "change_me"));
    }
}
