package model;

/**
 * Type-safe value object encapsulating the output of a {@link classify.MessageClassifier}.
 * Pairs the target destination {@link Branch} with the assessed {@link Urgency} level,
 * and provides a fault-tolerant parser for pipe-delimited {@code "DESTINATION|URGENCY"} strings.
 */
public class ClassificationResult {

    private final Branch branch;
    private final Urgency urgency;

    /**
     * Constructs a classification result with the given destination branch and urgency level.
     *
     * @param branch  the target military branch
     * @param urgency the urgency priority level
     */
    public ClassificationResult(Branch branch, Urgency urgency) {
        this.branch = branch;
        this.urgency = urgency;
    }

    /**
     * Parses a pipe-delimited string (e.g., {@code "NAVY|HIGH"}) into a {@link ClassificationResult}.
     * Gracefully falls back to {@link Branch#ALL} and/or {@link Urgency#ROUTINE} if either token
     * is missing or does not match a valid enum constant.
     *
     * @param classifierOutput raw pipe-delimited string {@code "BRANCH|URGENCY"}
     * @return a validated {@link ClassificationResult} instance
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

    /**
     * Returns the destination military branch.
     *
     * @return the target {@link Branch}
     */
    public Branch getBranch() {
        return branch;
    }

    /**
     * Returns the assessed urgency level of the message.
     *
     * @return the {@link Urgency} level
     */
    public Urgency getUrgency() {
        return urgency;
    }

    @Override
    public String toString() {
        return branch + "|" + urgency;
    }
}
