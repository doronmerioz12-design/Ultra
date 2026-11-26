package model;

import java.util.Arrays;
import java.util.Optional;

public enum CipherType {
    CAESAR,
    VIGENERE;

    public static Optional<CipherType> fromString(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(input.trim()))
                .findFirst();
    }

    public static String availableNames() {
        return String.join(" / ", Arrays.stream(values()).map(Enum::name).toList());
    }
}