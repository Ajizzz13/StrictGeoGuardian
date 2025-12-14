package com.zeroends.strictgeoguardian.model;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class VerificationResult {

    private final AuthStatus authStatus;
    private final boolean loggable;
    private final Component kickMessage;
    private final String logMessage;

    private VerificationResult(AuthStatus authStatus, boolean loggable, Component kickMessage, String logMessage) {
        this.authStatus = authStatus;
        this.loggable = loggable;
        this.kickMessage = kickMessage;
        this.logMessage = logMessage;
    }

    public boolean isAllowed() {
        return authStatus != AuthStatus.PENDING_VERIFICATION;
    }

    public AuthStatus getAuthStatus() {
        return authStatus;
    }

    public boolean isLoggable() {
        return loggable;
    }

    public Component getKickMessage() {
        return kickMessage;
    }

    public String getLogMessage() {
        return logMessage;
    }

    public static VerificationResult autoAllow(double score) {
        String msg = String.format("AUTO_ALLOW (Score: %.2f%%)", score);
        return new VerificationResult(AuthStatus.AUTHENTICATED, false, null, msg);
    }

    public static VerificationResult allowMonitor(double score) {
        String msg = String.format("ALLOW_MONITOR (Score: %.2f%%)", score);
        return new VerificationResult(AuthStatus.AUTHENTICATED, true, null, msg);
    }

    public static VerificationResult needsRegistration() {
        String msg = "NEEDS_REGISTRATION (First login)";
        return new VerificationResult(AuthStatus.NEEDS_REGISTRATION, true, null, msg);
    }

    public static VerificationResult needsLogin(String reason) {
        String msg = "NEEDS_LOGIN (" + reason + ")";
        return new VerificationResult(AuthStatus.NEEDS_LOGIN, true, null, msg);
    }

    public static VerificationResult autoBlockError(String error) {
        String msg = "AUTO_BLOCK (Internal Error: " + error + ")";
        Component kick = Component.text("An internal error occurred during verification. Please try again later.").color(NamedTextColor.RED);
        return new VerificationResult(AuthStatus.PENDING_VERIFICATION, true, kick, msg);
    }
}
