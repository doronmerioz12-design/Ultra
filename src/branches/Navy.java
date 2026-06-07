package branches;

public class Navy extends AbstractBranchObserver {

    public Navy() {
        super("NavyMes", "Navy HQ",
                System.getenv().getOrDefault("NAVY_DB_USER", "Navy"),
                System.getenv().getOrDefault("NAVY_DB_PASSWORD", "Navy123"));
    }
}
