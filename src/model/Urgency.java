package model;

/**
 * Enumeration representing the operational priority/urgency level assigned to a decrypted
 * military intelligence message by the classifier.
 */
public enum Urgency {
    /** Minor tactical or administrative update requiring no immediate action. */
    LOW,
    /** Standard operational intelligence of moderate significance. */
    MEDIUM,
    /** Time-sensitive tactical or strategic intelligence requiring prompt review. */
    HIGH,
    /** Imminent threat or decisive operational intelligence requiring immediate command action. */
    CRITICAL,
    /** Default/fallback urgency classification for standard traffic. */
    ROUTINE
}
