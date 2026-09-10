package com.memforce.importer;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Thrown when a question set file does not match the import format; carries every violation found. */
public class QuestionSetFormatException extends Exception {

    private final List<ValidationError> errors;

    QuestionSetFormatException(@NonNull List<ValidationError> errors) {
        super(errors.toString());
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    /** Every rule the file breaks, in the order they appear in the file. Never empty. */
    @NonNull
    public List<ValidationError> getErrors() {
        return errors;
    }
}
