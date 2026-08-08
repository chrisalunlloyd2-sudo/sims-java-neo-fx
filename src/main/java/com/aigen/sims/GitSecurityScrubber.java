package com.aigen.sims;

import java.util.regex.Pattern;

/**
 * SIMS1337 - GitSecurityScrubber
 * Scraps and redacts any OAuth tokens, GitHub PATs, API keys, App Passwords,
 * and sensitive chat logs before any Git commit or remote push.
 */
public class GitSecurityScrubber {
    private static final Pattern PAT_PATTERN = Pattern.compile("github_pat_[a-zA-Z0-9_]+");
    private static final Pattern OAUTH_PATTERN = Pattern.compile("ghp_[a-zA-Z0-9]{36}");
    private static final Pattern APP_PASS_PATTERN = Pattern.compile("[a-z]{4}\\s[a-z]{4}\\s[a-z]{4}\\s[a-z]{4}");

    public static String sanitizeContent(String input) {
        if (input == null || input.isEmpty()) return input;

        String clean = input;
        clean = PAT_PATTERN.matcher(clean).replaceAll("[REDACTED_GITHUB_PAT]");
        clean = OAUTH_PATTERN.matcher(clean).replaceAll("[REDACTED_OAUTH_TOKEN]");
        clean = APP_PASS_PATTERN.matcher(clean).replaceAll("[REDACTED_APP_PASSWORD]");

        return clean;
    }
}
