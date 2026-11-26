package branches;

public class PMOFFICE extends AbstractBranchObserver {

    public PMOFFICE() {
        super("PMMes", "PM HQ",
                System.getenv().getOrDefault("PMOFFICE_DB_USER", "PMOFFICE"),
                System.getenv().getOrDefault("PMOFFICE_DB_PASSWORD", "change_me"));
    }
}
