package com.embabel.dif.verifier;

import com.embabel.dif.domain.Intent;
import com.embabel.dif.domain.IntentType;
import com.embabel.dif.domain.SemanticModel;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Stable snapshot keys for canvas <em>S - Safeguards</em> only (provenance
 * {@code :S}). Norms ({@code :N}) stay out so a DTO rename cannot fail review.
 */
public final class SafeguardPaths {

    private static final Pattern LEAD_IN = Pattern.compile(
            "^(do not change|do not add|preserve:?\\s*)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");

    private SafeguardPaths() {
    }

    public static Set<String> fromCanvasSafeguards(SemanticModel model) {
        var paths = new LinkedHashSet<String>();
        for (Intent intent : model.intents()) {
            if (intent.type() != IntentType.PRESERVATION) {
                continue;
            }
            if (!intent.provenance().source().endsWith(":S")) {
                continue;
            }
            paths.add(path(intent.statement()));
        }
        return Set.copyOf(paths);
    }

    public static String path(String statement) {
        var body = LEAD_IN.matcher(statement.toLowerCase(Locale.ROOT).strip()).replaceFirst("");
        body = NON_SLUG.matcher(body).replaceAll("-").replaceAll("^-+|-+$", "");
        if (body.isBlank()) {
            body = "unspecified";
        }
        return "safeguard." + body;
    }
}
