package com.embabel.dif.canvas;

import com.embabel.dif.dif.IntentFolder;
import com.embabel.dif.dif.ObligationDeriver;
import com.embabel.dif.domain.MissingObligation;
import com.embabel.dif.domain.RepositoryAnalysis;
import com.embabel.dif.domain.SemanticModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Canvas → typed semantic model. Deterministic after the markdown is accepted.
 */
@Component
public class CanvasFolder {

    private final ReasonsCanvasParser parser;
    private final CanvasIntentMapper mapper;
    private final IntentFolder intentFolder;
    private final ObligationDeriver obligationDeriver;

    public CanvasFolder(
            ReasonsCanvasParser parser,
            CanvasIntentMapper mapper,
            IntentFolder intentFolder,
            ObligationDeriver obligationDeriver
    ) {
        this.parser = parser;
        this.mapper = mapper;
        this.intentFolder = intentFolder;
        this.obligationDeriver = obligationDeriver;
    }

    public ReasonsCanvas parse(String markdown) {
        return parser.parse(markdown);
    }

    public SemanticModel fold(String markdown) {
        return fold(parser.parse(markdown));
    }

    public SemanticModel fold(ReasonsCanvas canvas) {
        var model = intentFolder.fold(mapper.toCandidate(canvas));
        var missing = new ArrayList<MissingObligation>();
        missing.addAll(openOperations(canvas));
        missing.addAll(obligationDeriver.derive(model, new RepositoryAnalysis(List.of(), List.of())));
        return model.withMissingObligations(SemanticModel.canonicalObligations(missing));
    }

    private static List<MissingObligation> openOperations(ReasonsCanvas canvas) {
        return canvas.operations().stream()
                .filter(operation -> !operation.complete())
                .map(operation -> new MissingObligation(
                        operation.id() + " - " + operation.name(),
                        canvas.workId()
                ))
                .toList();
    }
}
