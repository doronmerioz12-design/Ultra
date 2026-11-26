package model;

/**
 * Enumeration of military command branches that can receive decrypted intelligence from ULTRA.
 */
public enum Branch {
    /** Royal Navy / Admiralty (maritime and U-boat operations). */
    NAVY,
    /** Royal Air Force (aerial operations, air defense, and bombing campaigns). */
    RAF,
    /** British Army (ground forces, troop movements, and land operations). */
    ARMY,
    /** Prime Minister's Office (high-level diplomatic and national strategic intelligence). */
    PM_OFFICE,
    /** Supreme Headquarters Allied Expeditionary Force (joint Allied command). */
    SHAEF,
    /** Broadcast target indicating the message should be delivered to all registered branches. */
    ALL
}
