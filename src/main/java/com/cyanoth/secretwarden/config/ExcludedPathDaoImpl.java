package com.cyanoth.secretwarden.config;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import net.java.ao.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Implementation of the ExcludedPathDAO.
 * Provides functions to access AO relating to excludedPaths (that is, patterns to exclude paths from scanning)
 */
@Scanned
@Component("ExcludedPathDao")
public class ExcludedPathDaoImpl implements ExcludedPathDao {

    private final ActiveObjects ao;
    private final int MIN_EXCLUDED_PATH_PATTERN_CHARS = 3;
    private final int MAX_EXCLUDED_PATH_PATTERN_CHARS = 255; // If you change me, add StringLength

    @Autowired
    public ExcludedPathDaoImpl(@ComponentImport ActiveObjects ao) {
        this.ao = ao;
    }

    /**
     * Gets a single excludedPath pattern by ID.
     * @param id The ID number of the excludedPath to get.
     * @return Matched ExcludedPathEntity from AO. Can be null if nothing is found.
     */
    @Override
    @Nullable
    public ExcludedPathEntity getExcludedPathById(int id) {
        ExcludedPathEntity[] entity = ao.find(ExcludedPathEntity.class, Query.select().where("ID = ?", id));

        if (entity.length == 1) {
            return entity[0];
        } else {
            return null;
        }
    }

    /**
     * @return A list of all excludedPaths. Can be empty if none exist
     */
    @Override
    @Nullable
    public List<ExcludedPathEntity> getAllExcludedPaths() {
        return newArrayList(ao.find(ExcludedPathEntity.class));
    }

    @Override
    public ExcludedPathEntity createExcludedPath(String excludedPathPattern) throws IllegalArgumentException {
        if (!validateExcludedPath(excludedPathPattern))
            throw new IllegalArgumentException(String.format("Validation failed for the excludedPath! Ensure it is between %d-%d characters",
                    MIN_EXCLUDED_PATH_PATTERN_CHARS, MAX_EXCLUDED_PATH_PATTERN_CHARS));

        final ExcludedPathEntity entity = ao.create(ExcludedPathEntity.class);
        entity.setExcludedPath(excludedPathPattern);
        entity.save();
        return entity;
    }

    /**
     * Updates an excludedPath AO
     * @param id The ID of the excludedPath to update
     * @param excludedPath Pattern of the excludedPath to update to.
     * @return The updated excludedPath entity
     * @throws IllegalArgumentException Validation failure.
     */
    @Override
    public ExcludedPathEntity updateExcludedPath(int id, String excludedPath) throws IllegalArgumentException {
        if (!validateExcludedPath(excludedPath))
            throw new IllegalArgumentException(String.format("Validation failed for the excludedPAth! Ensure it is between %d-%d characters",
                    MIN_EXCLUDED_PATH_PATTERN_CHARS, MAX_EXCLUDED_PATH_PATTERN_CHARS));

        ExcludedPathEntity entity = ao.find(ExcludedPathEntity.class, Query.select().where("ID = ?", id))[0];
        entity.setExcludedPath(excludedPath);
        entity.save();
        ao.flush(entity); // Force caches to update
        return entity;
    }

    /**
     * @param id The ID of the excludedPath to delete
     */
    @Override
    public void deleteExcludedPath(int id) {
        // Couldn't figure out how ao.deleteWithSQL() actually works and the lack of documentation meant this find & delete instead.
        ExcludedPathEntity[] excludedPathEntity = ao.find(ExcludedPathEntity.class, Query.select().where("ID = ?", id));

        if (excludedPathEntity.length == 1) {
            ao.delete(excludedPathEntity[0]);
            ao.flushAll();
        } else {
            throw new IllegalArgumentException(String.format("Excluded Path by the ID: %d was not found!", id ));
        }
    }

    /**
     * Check whether an excludedPath matches validation rules
     * @param excludedPath Excluded Path String to check
     * @return True - Validation passes. False otherwise
     */
    private boolean validateExcludedPath(String excludedPath) {
        return (excludedPath.length() >= MIN_EXCLUDED_PATH_PATTERN_CHARS && excludedPath.length() <= MAX_EXCLUDED_PATH_PATTERN_CHARS);
    }
}
