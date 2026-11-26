package model;

import java.time.LocalDateTime;

/**
 * Immutable domain model representing an intercepted encrypted radio transmission
 * retrieved from the {@code INPUT} database table.
 */
public class Message {
    private final int id;
    private final String rawCiphertext;
    private final String originFrequency;
    private final LocalDateTime interceptedAt;
    private final String cipherType;

    /**
     * Constructs a new intercepted {@link Message} record.
     *
     * @param id              unique database primary key from the {@code INPUT} table
     * @param rawCiphertext   the uncracked encrypted text string
     * @param originFrequency the radio frequency on which the signal was captured
     * @param cipherType      the cipher algorithm identifier (e.g., {@code "CAESAR"} or {@code "VIGENERE"})
     */
    public Message(int id, String rawCiphertext, String originFrequency, String cipherType) {
        this.id = id;
        this.rawCiphertext = rawCiphertext;
        this.originFrequency = originFrequency;
        this.interceptedAt = LocalDateTime.now();
        this.cipherType = cipherType;
    }

    /**
     * Returns the unique database identifier of this message.
     *
     * @return the message ID
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the raw encrypted ciphertext string.
     *
     * @return the raw ciphertext
     */
    public String getRawCiphertext() {
        return rawCiphertext;
    }

    /**
     * Returns the origin radio frequency label.
     *
     * @return the origin frequency
     */
    public String getOriginFrequency() {
        return originFrequency;
    }

    /**
     * Returns the timestamp when this message object was instantiated.
     *
     * @return the interception timestamp
     */
    public LocalDateTime getInterceptedAt() {
        return interceptedAt;
    }

    /**
     * Returns the suspected cipher family name (e.g., {@code "CAESAR"} or {@code "VIGENERE"}).
     *
     * @return the cipher type identifier
     */
    public String getCipherType() {
        return cipherType;
    }
}
