package model;

/**
 * Type-safe wrapper for the classifier output.
 * Parses the "DESTINATION|URGENCY" string into proper enum values.
 */
public class ClassificationResult {

    private final Branch branch;
    private final Urgency urgency;

    public ClassificationResult(Branch branch, Urgency urgency) {
        this.branch = branch;
        this.urgency = urgency;
    }

    /**
     * Parses a pipe-delimited string like "NAVY|HIGH" into a ClassificationResult.
     * Falls back to ALL|ROUTINE if parsing fails.
     */
    public static ClassificationResult parse(String classifierOutput) {
        if (classifierOutput == null || !classifierOutput.contains("|")) {
            return new ClassificationResult(Branch.ALL, Urgency.ROUTINE);
        }

        String[] parts = classifierOutput.split("\\|", 2);

        Branch branch;
        try {
            branch = Branch.valueOf(parts[0].trim());
        } catch (IllegalArgumentException e) {
            branch = Branch.ALL;
        }

        Urgency urgency;
        try {
            urgency = Urgency.valueOf(parts[1].trim());
        } catch (IllegalArgumentException e) {
            urgency = Urgency.ROUTINE;
        }

        return new ClassificationResult(branch, urgency);
    }

    public Branch getBranch() {
        return branch;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    @Override
    public String toString() {
        return branch + "|" + urgency;
    }
}
