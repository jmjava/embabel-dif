package com.embabel.dif.dif;

import com.embabel.dif.domain.CandidateIntent;
import com.embabel.dif.domain.SemanticModel;

/**
 * Converts accepted candidate facts into a canonical semantic model.
 * The fold is deterministic after candidates are accepted.
 * Grow the rule catalog behind this seam; see docs/FOLD_ITERATION.md.
 */
public interface IntentFolder {

    SemanticModel fold(CandidateIntent candidate);
}
