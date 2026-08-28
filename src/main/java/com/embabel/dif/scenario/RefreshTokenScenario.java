package com.embabel.dif.scenario;

import com.embabel.dif.domain.CandidateIntent;
import com.embabel.dif.domain.ChangeRequest;
import com.embabel.dif.domain.Evidence;
import com.embabel.dif.domain.EvidenceType;
import com.embabel.dif.domain.Intent;
import com.embabel.dif.domain.IntentType;
import com.embabel.dif.domain.Priority;
import com.embabel.dif.domain.Provenance;
import com.embabel.dif.domain.RepositoryAnalysis;
import com.embabel.dif.domain.SemanticProperty;
import com.embabel.dif.domain.SemanticSnapshot;

import java.util.List;
import java.util.Locale;

/**
 * Narrow first end-to-end scenario from the prototype spec.
 */
public final class RefreshTokenScenario {

    public static final String REQUEST_TEXT =
            "Add refresh-token rotation without changing existing login behavior.";

    private RefreshTokenScenario() {
    }

    public static ChangeRequest request() {
        return new ChangeRequest(REQUEST_TEXT);
    }

    public static boolean matches(ChangeRequest request) {
        var text = request.text().toLowerCase(Locale.ROOT);
        return text.contains("refresh-token rotation") || text.contains("refresh token rotation");
    }

    public static CandidateIntent candidateIntent() {
        var source = Provenance.of(EvidenceType.USER_STATEMENT, "CHANGE-REQUEST-42");
        return new CandidateIntent(
                List.of(
                        new Intent("INT-001", IntentType.REQUIREMENT, "Add refresh-token rotation.", Priority.REQUIRED, source),
                        new Intent("INT-002", IntentType.PRESERVATION, "Preserve Google login.", Priority.REQUIRED, source),
                        new Intent("INT-003", IntentType.PRESERVATION, "Preserve Apple login.", Priority.REQUIRED, source),
                        new Intent("INT-004", IntentType.PRESERVATION, "Preserve Authorization Code Flow.", Priority.REQUIRED, source),
                        new Intent("INT-005", IntentType.PRESERVATION, "Preserve sessionToken claim.", Priority.REQUIRED, source)
                ),
                List.of(
                        new Evidence("E1", EvidenceType.TEST, "JwtClaimsTest", "Existing test asserts sessionToken claim exists."),
                        new Evidence("E2", EvidenceType.SOURCE_CODE, "AuthTokenReader", "Mobile authentication code reads sessionToken."),
                        new Evidence("E3", EvidenceType.GIT_HISTORY, "7e93982", "Commit history says claim was added for mobile compatibility."),
                        new Evidence("E4", EvidenceType.USER_STATEMENT, "CHANGE-REQUEST-42", "User request says existing login behavior must not change."),
                        new Evidence("E5", EvidenceType.SOURCE_CODE, "oauth", "Google OAuth login exists."),
                        new Evidence("E6", EvidenceType.SOURCE_CODE, "oauth", "Apple OAuth login exists."),
                        new Evidence("E7", EvidenceType.SOURCE_CODE, "oauth", "Authorization Code Flow is used."),
                        new Evidence("E8", EvidenceType.TEST, "LoginIT", "Integration tests assert existing login behavior."),
                        new Evidence("E9", EvidenceType.RUNTIME, "tokens", "Refresh tokens currently remain valid after use.")
                )
        );
    }

    public static RepositoryAnalysis repositoryAnalysis() {
        return new RepositoryAnalysis(
                List.of(
                        "TokenService.java",
                        "RefreshTokenEntity.java",
                        "OAuth2AuthorizationRepository.java"
                ),
                List.of(
                        new Evidence("R1", EvidenceType.SOURCE_CODE, "oauth", "Google OAuth login exists."),
                        new Evidence("R2", EvidenceType.SOURCE_CODE, "oauth", "Apple OAuth login exists."),
                        new Evidence("R3", EvidenceType.SOURCE_CODE, "oauth", "Authorization Code Flow is used."),
                        new Evidence("R4", EvidenceType.SOURCE_CODE, "jwt", "JWT contains sessionToken."),
                        new Evidence("R5", EvidenceType.TEST, "LoginIT", "Integration tests assert existing login behavior."),
                        new Evidence("R6", EvidenceType.RUNTIME, "tokens", "Refresh tokens currently remain valid after use."),
                        new Evidence("R7", EvidenceType.SOURCE_CODE, "tokens", "token family identifier is present."),
                        new Evidence("R8", EvidenceType.SOURCE_CODE, "tokens", "consumed-token state is present."),
                        new Evidence("R9", EvidenceType.SOURCE_CODE, "tokens", "replay detection is present.")
                )
        );
    }

    public static SemanticSnapshot before() {
        return new SemanticSnapshot(List.of(
                new SemanticProperty("refresh-token.reusable", "true"),
                new SemanticProperty("refresh-token.rotates", "false"),
                new SemanticProperty("provider.GOOGLE", "present"),
                new SemanticProperty("provider.APPLE", "present"),
                new SemanticProperty("jwt.claim.sessionToken", "present")
        ));
    }

    public static SemanticSnapshot desired() {
        return new SemanticSnapshot(List.of(
                new SemanticProperty("refresh-token.reusable", "false"),
                new SemanticProperty("refresh-token.rotates", "true"),
                new SemanticProperty("provider.GOOGLE", "present"),
                new SemanticProperty("provider.APPLE", "present"),
                new SemanticProperty("jwt.claim.sessionToken", "present")
        ));
    }
}
