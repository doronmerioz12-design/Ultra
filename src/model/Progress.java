package model;

/**
 * Enumeration representing the processing lifecycle states of an intercepted message
 * in the {@code INPUT} queue table.
 */
public enum Progress {
    /** Newly intercepted message waiting in the queue to be processed. */
    PENDING,
    /** Message has been claimed by a worker batch and is currently being decrypted/classified. */
    IN_PROGRESS,
    /** Message was successfully decrypted, classified, and routed to its destination branch(es). */
    DONE,
    /** Processing encountered an unrecoverable error (e.g., unsupported cipher type). */
    FAILED
}
