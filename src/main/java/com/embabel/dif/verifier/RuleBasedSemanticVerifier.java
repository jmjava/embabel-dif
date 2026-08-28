package com.embabel.dif.verifier;

import com.embabel.dif.domain.ProposedChange;
import com.embabel.dif.domain.SemanticModel;
import com.embabel.dif.domain.SemanticProperty;
import com.embabel.dif.domain.SemanticSnapshot;
import com.embabel.dif.domain.SemanticVerification;
import com.embabel.dif.domain.TestExecution;
import com.embabel.dif.domain.VerificationResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Phase 4 stub with a working semantic diff and placeholder invariant checks.
 */
@Component
public class RuleBasedSemanticVerifier implements SemanticVerifier {

    public static final Set<String> REQUIRED_UNCHANGED = Set.of(
            "provider.GOOGLE",
            "provider.APPLE",
            "jwt.claim.sessionToken"
    );

    @Override
    public IntentDiff diff(SemanticSnapshot before, SemanticSnapshot desired) {
        var beforeMap = before.asMap();
        var desiredMap = desired.asMap();
        var paths = new LinkedHashSet<String>();
        paths.addAll(beforeMap.keySet());
        paths.addAll(desiredMap.keySet());

        var added = new ArrayList<SemanticProperty>();
        var removed = new ArrayList<SemanticProperty>();
        var unchanged = new ArrayList<SemanticProperty>();

        for (var path : paths.stream().sorted().toList()) {
            var previous = beforeMap.get(path);
            var next = desiredMap.get(path);
            if (previous == null && next != null) {
                added.add(new SemanticProperty(path, next));
            } else if (next == null) {
                removed.add(new SemanticProperty(path, previous));
            } else if (previous.equals(next)) {
                unchanged.add(new SemanticProperty(path, next));
            } else {
                removed.add(new SemanticProperty(path, previous));
                added.add(new SemanticProperty(path, next));
            }
        }

        added.sort(Comparator.comparing(SemanticProperty::path));
        removed.sort(Comparator.comparing(SemanticProperty::path));
        unchanged.sort(Comparator.comparing(SemanticProperty::path));
        return new IntentDiff(added, removed, unchanged);
    }

    @Override
    public SemanticVerification verify(
            SemanticModel model,
            ProposedChange change,
            TestExecution tests,
            IntentDiff intentDiff
    ) {
        var results = new ArrayList<VerificationResult>();
        if (tests != null && !tests.passed()) {
            results.add(VerificationResult.fail("tests", "TestExecution", "Required tests did not pass"));
        }
        if (!intentDiff.preserves(REQUIRED_UNCHANGED)) {
            results.add(VerificationResult.fail(
                    "INV-PRESERVE-LOGIN",
                    intentDiff.render(),
                    "Required login/JWT properties were not preserved"
            ));
        } else {
            results.add(VerificationResult.pass("INV-PRESERVE-LOGIN", intentDiff.render()));
        }
        for (var invariant : model.invariants()) {
            results.add(VerificationResult.pass(
                    invariant.id(),
                    "Phase 4 stub: invariant recorded for later deterministic check — " + invariant.description()
            ));
        }
        var failed = results.stream().anyMatch(result -> result.status().name().equals("FAIL"));
        return new SemanticVerification(!failed, results);
    }
}
