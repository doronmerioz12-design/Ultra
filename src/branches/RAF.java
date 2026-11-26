package branches;

public class RAF extends AbstractBranchObserver {

    public RAF() {
        super("RAFMes", "RAF HQ",
                System.getenv().getOrDefault("RAF_DB_USER", "RAF"),
                System.getenv().getOrDefault("RAF_DB_PASSWORD", "change_me"));
    }
}
