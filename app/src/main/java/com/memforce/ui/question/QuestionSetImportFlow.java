package com.memforce.ui.question;

import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.memforce.R;
import com.memforce.data.QuestionSetImporter;
import com.memforce.importer.QuestionSet;
import com.memforce.importer.QuestionSetFormatException;
import com.memforce.importer.QuestionSetParser;
import com.memforce.importer.ValidationError;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Reads a question set file the user picked, shows what importing it would do, and writes it once
 * the user confirms. Reading and writing run off the main thread because the picked file may come
 * from a remote document provider.
 */
public final class QuestionSetImportFlow {

    public interface OnImported {
        void onImported();
    }

    private static final long MAX_FILE_BYTES = 1024L * 1024L;
    private static final int MAX_SHOWN_ERRORS = 10;
    private static final String THREAD_NAME = "question-set-import";

    private final AppCompatActivity activity;
    private final OnImported onImported;

    public QuestionSetImportFlow(@NonNull AppCompatActivity activity,
                                 @NonNull OnImported onImported) {
        this.activity = activity;
        this.onImported = onImported;
    }

    public void start(@NonNull Uri uri) {
        new Thread(() -> {
            try {
                QuestionSet set = QuestionSetParser.parse(read(uri));
                QuestionSetImporter.ImportPlan plan = new QuestionSetImporter(activity).plan(set);
                post(() -> confirm(set, plan), R.string.import_cancelled);
            } catch (ReadFailure failure) {
                post(() -> showFailure(activity.getString(failure.messageRes, failure.args)),
                        R.string.import_cancelled);
            } catch (QuestionSetFormatException e) {
                post(() -> showFailure(describe(e.getErrors())), R.string.import_cancelled);
            } catch (RuntimeException e) {
                post(() -> showFailure(activity.getString(R.string.import_error_unexpected)),
                        R.string.import_cancelled);
            }
        }, THREAD_NAME).start();
    }

    private String read(Uri uri) throws ReadFailure {
        try (InputStream input = activity.getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new ReadFailure(R.string.import_error_unreadable);
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = input.read(chunk)) != -1) {
                if (buffer.size() + read > MAX_FILE_BYTES) {
                    throw new ReadFailure(R.string.import_error_too_large,
                            MAX_FILE_BYTES / (1024 * 1024));
                }
                buffer.write(chunk, 0, read);
            }
            return buffer.toString(StandardCharsets.UTF_8.name());
        } catch (IOException | RuntimeException e) {
            throw new ReadFailure(R.string.import_error_unreadable);
        }
    }

    private void confirm(QuestionSet set, QuestionSetImporter.ImportPlan plan) {
        StringBuilder message = new StringBuilder(activity.getString(R.string.import_confirm_counts,
                plan.getQuestionsToCreate(), plan.getQuestionsToMerge(), plan.getTagsToCreate()));
        if (plan.getQuestionsToMerge() > 0) {
            message.append("\n\n").append(activity.getString(R.string.import_confirm_merge_hint));
        }
        message.append("\n\n").append(activity.getString(R.string.import_confirm_question));

        new MaterialAlertDialogBuilder(activity)
                .setTitle(set.getName() == null
                        ? activity.getString(R.string.import_title) : set.getName())
                .setMessage(message.toString())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_import, (dialog, which) -> apply(set))
                .show();
    }

    private void apply(QuestionSet set) {
        new Thread(() -> {
            try {
                QuestionSetImporter.ImportResult result = new QuestionSetImporter(activity).apply(set);
                post(() -> showResult(result), R.string.import_result_title);
            } catch (RuntimeException e) {
                post(() -> showFailure(activity.getString(R.string.import_error_unexpected)),
                        R.string.import_cancelled);
            }
        }, THREAD_NAME).start();
    }

    private void showResult(QuestionSetImporter.ImportResult result) {
        onImported.onImported();
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.import_result_title)
                .setMessage(activity.getString(R.string.import_result_message,
                        result.getQuestionsCreated(),
                        result.getQuestionsMerged(),
                        result.getTagsCreated()))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private String describe(List<ValidationError> errors) {
        StringBuilder message = new StringBuilder(activity.getString(R.string.import_error_intro));
        int shown = Math.min(errors.size(), MAX_SHOWN_ERRORS);
        for (int i = 0; i < shown; i++) {
            message.append("\n\n• ").append(describe(errors.get(i)));
        }
        if (errors.size() > shown) {
            message.append("\n\n").append(activity.getResources().getQuantityString(
                    R.plurals.import_error_more, errors.size() - shown, errors.size() - shown));
        }
        return message.toString();
    }

    private String describe(ValidationError error) {
        String location = error.getLocation();
        String detail = error.getDetail() == null ? "" : error.getDetail();
        switch (error.getReason()) {
            case MALFORMED_JSON:
                return activity.getString(R.string.import_error_malformed_json, detail);
            case NOT_AN_OBJECT:
                return activity.getString(R.string.import_error_not_object);
            case MISSING_FIELD:
                return activity.getString(R.string.import_error_missing, location);
            case UNKNOWN_FIELD:
                return activity.getString(R.string.import_error_unknown_field, location);
            case WRONG_TYPE:
                return activity.getString(R.string.import_error_wrong_type, location);
            case BLANK:
                return activity.getString(R.string.import_error_blank, location);
            case NOT_ONE_LINE:
                return activity.getString(R.string.import_error_not_one_line, location);
            case TOO_LONG:
                return activity.getResources().getQuantityString(
                        R.plurals.import_error_too_long, error.getLimit(), location, error.getLimit());
            case DUPLICATE_TAG:
                return activity.getString(R.string.import_error_duplicate_tag, location, detail);
            case DUPLICATE_ANSWER:
                return activity.getString(R.string.import_error_duplicate_answer, location, detail);
            case ALTERNATIVES_WITHOUT_ANSWER:
                return activity.getString(
                        R.string.import_error_alternatives_without_answer, location);
            case UNSUPPORTED_VERSION:
                return activity.getString(R.string.import_error_unsupported_version, detail,
                        TextUtils.join(", ", QuestionSetParser.SUPPORTED_FORMAT_VERSIONS));
            case NO_QUESTIONS:
                return activity.getString(R.string.import_error_no_questions);
            default:
                return location;
        }
    }

    private void showFailure(String message) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.import_error_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    /**
     * Runs an action on the screen that started the import.
     *
     * @param goneMessage shown as a toast instead when that screen is already gone, for instance
     *                    because the device was rotated while the file was being read
     */
    private void post(Runnable action, @StringRes int goneMessage) {
        activity.runOnUiThread(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                action.run();
            } else {
                Toast.makeText(activity.getApplicationContext(), goneMessage, Toast.LENGTH_LONG)
                        .show();
            }
        });
    }

    /** A file that cannot be turned into text at all, before any format rule applies. */
    private static final class ReadFailure extends Exception {

        private final int messageRes;
        private final Object[] args;

        ReadFailure(int messageRes, Object... args) {
            this.messageRes = messageRes;
            this.args = args;
        }
    }
}
