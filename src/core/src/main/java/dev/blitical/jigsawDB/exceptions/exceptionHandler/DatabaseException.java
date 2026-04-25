package dev.blitical.jigsawDB.exceptions.exceptionHandler;

import dev.blitical.jigsawDB.exceptions.exceptionHandler.JigsawDBException.Severity;

import java.util.Random;

public class DatabaseException extends RuntimeException {

    private static final String[] FUN_THINGS = {
            "Good luck, you need it!",
            "Just ChatGPT it vro...",
            "It might just be a skill issue...",
            "Let me throw this error again because I want to :D",
            "Error: Laugh at this developer!!!"
    };

    private final String prefix;
    private final String message;
    private final JigsawDBException annotation;

    public DatabaseException(String prefix, String message) {
        super();
        this.prefix = prefix;
        this.message = message;
        annotation = getClass().getAnnotation(JigsawDBException.class);
    }

    @Override
    public String getMessage() {
        return buildMessage();
    }

    private String buildMessage() {
        if (annotation == null) {
            return prefix + " (MEDIUM): " + message;
        }

        boolean displayFunThing = false;
        StringBuilder error = new StringBuilder(prefix)
                .append(" (").append(annotation.severity()).append("):\n")
                .append(message);

        if (!annotation.documentationURL().isBlank()) {
            error.append("\n Documentation Reference: ")
                    .append(annotation.documentationURL());
        }

        if (annotation.fixes().length != 0) {
            error.append("\nYou can try: ");
            for (String attempt : annotation.fixes()) {
                error.append("\n  - ").append(attempt);
            }
            displayFunThing = true;
        }

        if (!annotation.correct().isEmpty()) {
            error.append("\nCORRECT METHOD:\n").append(annotation.correct());
            displayFunThing = true;
        }

        if (!annotation.incorrect().isEmpty()) {
            error.append("\nINCORRECT METHOD:\n").append(annotation.correct());
            displayFunThing = true;
        }

        if (displayFunThing) {
            error.append("\n").append(FUN_THINGS[new Random().nextInt(FUN_THINGS.length)]);
        }
        return error.toString();
    }

    public final Severity getSeverity() {
        return annotation.severity();
    }

    public final String getDocumentationURL() {
        return annotation.documentationURL();
    }

    public final String[] getRecommendedActions() {
        return annotation.fixes();
    }

    public final String correctMethod() {
        return annotation.correct();
    }

    public final String incorrectMethod() {
        return annotation.incorrect();
    }
}
