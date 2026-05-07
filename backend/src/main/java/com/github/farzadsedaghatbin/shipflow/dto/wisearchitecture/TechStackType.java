package com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture;

/**
 * Enum representing different technology stack types that can be detected in repositories.
 */
public enum TechStackType {
    // Mobile stacks
    MOBILE_KOTLIN("Mobile (Kotlin/Android)", "kotlin"),
    MOBILE_SWIFT("Mobile (Swift/iOS)", "swift"),
    MOBILE_REACT_NATIVE("Mobile (React Native)", "react-native"),
    MOBILE_FLUTTER("Mobile (Flutter/Dart)", "flutter"),

    // Backend stacks
    BACKEND_JAVA("Backend (Java/Spring)", "java"),
    BACKEND_NODE("Backend (Node.js)", "node"),
    BACKEND_PYTHON("Backend (Python)", "python"),
    BACKEND_GO("Backend (Go)", "go"),
    BACKEND_DOTNET("Backend (.NET/C#)", "dotnet"),

    // Web frontend stacks
    WEB_REACT("Web (React)", "react"),
    WEB_ANGULAR("Web (Angular)", "angular"),
    WEB_VUE("Web (Vue.js)", "vue"),
    WEB_NEXTJS("Web (Next.js)", "nextjs"),

    // Other
    UNKNOWN("Unknown", "unknown");

    private final String displayName;
    private final String identifier;

    TechStackType(String displayName, String identifier) {
        this.displayName = displayName;
        this.identifier = identifier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIdentifier() {
        return identifier;
    }

    /**
     * Get the category of this stack (MOBILE, BACKEND, WEB, or UNKNOWN).
     */
    public String getCategory() {
        if (name().startsWith("MOBILE_")) {
            return "MOBILE";
        } else if (name().startsWith("BACKEND_")) {
            return "BACKEND";
        } else if (name().startsWith("WEB_")) {
            return "WEB";
        }
        return "UNKNOWN";
    }
}
