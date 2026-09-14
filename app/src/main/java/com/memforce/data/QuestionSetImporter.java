package com.memforce.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import com.memforce.db.MemForceDbHelper;
import com.memforce.importer.MergedQuestion;
import com.memforce.importer.QuestionSet;
import com.memforce.model.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Writes a parsed question set to the database, as described in
 * {@code docs/question-import-format.md}: every question is tagged with the union of the set level
 * and its own tags, tags that do not exist yet are created, and a question whose text already
 * exists gains the file's tags and alternative answers instead of being stored a second time.
 */
public class QuestionSetImporter {

    /** What an import would do, counted before anything is written. */
    public static final class ImportPlan {

        private final int questionsToCreate;
        private final int questionsToMerge;
        private final int tagsToCreate;

        ImportPlan(int questionsToCreate, int questionsToMerge, int tagsToCreate) {
            this.questionsToCreate = questionsToCreate;
            this.questionsToMerge = questionsToMerge;
            this.tagsToCreate = tagsToCreate;
        }

        public int getQuestionsToCreate() {
            return questionsToCreate;
        }

        /** Questions the app already has; they only gain the tags and alternative answers of this file. */
        public int getQuestionsToMerge() {
            return questionsToMerge;
        }

        public int getTagsToCreate() {
            return tagsToCreate;
        }
    }

    /** What an import did. */
    public static final class ImportResult {

        private final int questionsCreated;
        private final int questionsMerged;
        private final int tagsCreated;

        ImportResult(int questionsCreated, int questionsMerged, int tagsCreated) {
            this.questionsCreated = questionsCreated;
            this.questionsMerged = questionsMerged;
            this.tagsCreated = tagsCreated;
        }

        public int getQuestionsCreated() {
            return questionsCreated;
        }

        public int getQuestionsMerged() {
            return questionsMerged;
        }

        public int getTagsCreated() {
            return tagsCreated;
        }
    }

    private final MemForceDbHelper helper;
    private final QuestionDao questionDao;
    private final TagDao tagDao;

    public QuestionSetImporter(@NonNull Context context) {
        this.helper = MemForceDbHelper.getInstance(context);
        this.questionDao = new QuestionDao(context);
        this.tagDao = new TagDao(context);
    }

    /** Counts what {@link #apply(QuestionSet)} would do, without touching the database. */
    @NonNull
    public ImportPlan plan(@NonNull QuestionSet set) {
        List<MergedQuestion> questions = MergedQuestion.mergeAll(set);

        int toCreate = 0;
        for (MergedQuestion question : questions) {
            if (questionDao.findIdByName(question.getQuestion()) == null) {
                toCreate++;
            }
        }

        Set<String> tagsToCreate = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> known = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (MergedQuestion question : questions) {
            for (String tag : question.getTags()) {
                if (known.add(tag) && tagDao.findByName(tag) == null) {
                    tagsToCreate.add(tag);
                }
            }
        }
        return new ImportPlan(toCreate, questions.size() - toCreate, tagsToCreate.size());
    }

    /**
     * Imports the whole set in one transaction, so a failure leaves the database untouched.
     *
     * @throws IllegalStateException when a row cannot be stored; the transaction is rolled back
     */
    @NonNull
    public ImportResult apply(@NonNull QuestionSet set) {
        List<MergedQuestion> questions = MergedQuestion.mergeAll(set);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            Map<String, Long> tagIds = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            int created = 0;
            int merged = 0;
            int tagsCreated = 0;

            for (MergedQuestion question : questions) {
                List<Long> ids = new ArrayList<>();
                for (String tag : question.getTags()) {
                    Long id = tagIds.get(tag);
                    if (id == null) {
                        Tag existing = tagDao.findByName(tag);
                        if (existing != null) {
                            id = existing.getId();
                        } else {
                            id = tagDao.insert(tag);
                            if (id == -1L) {
                                throw new IllegalStateException("Cannot store the tag " + tag);
                            }
                            tagsCreated++;
                        }
                        tagIds.put(tag, id);
                    }
                    if (!ids.contains(id)) {
                        ids.add(id);
                    }
                }

                Long existingId = questionDao.findIdByName(question.getQuestion());
                if (existingId == null) {
                    if (questionDao.insert(question.getQuestion(), question.getAnswer(),
                            question.getAlternativeAnswers(), ids) == -1L) {
                        throw new IllegalStateException(
                                "Cannot store the question " + question.getQuestion());
                    }
                    created++;
                } else {
                    questionDao.addTags(existingId, ids);
                    if (question.getAnswer() != null) {
                        questionDao.fillMissingAnswer(existingId, question.getAnswer());
                    }
                    questionDao.addAlternativeAnswers(
                            existingId, question.getAlternativeAnswers());
                    merged++;
                }
            }

            db.setTransactionSuccessful();
            return new ImportResult(created, merged, tagsCreated);
        } finally {
            db.endTransaction();
        }
    }
}
