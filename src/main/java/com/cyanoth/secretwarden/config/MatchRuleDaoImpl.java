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

@Scanned
@Component("MatchRuleDao")
public class MatchRuleDaoImpl implements MatchRuleDao {

    private final ActiveObjects ao;
    private final int MIN_RULE_NAME_CHARS = 3;
    private final int MAX_RULE_NAME_CHARS = 100;
    private final int MIN_PATTERN_CHARS = 3;
    private final int MAX_PATTERN_CHARS = 1000;

    @Autowired
    public MatchRuleDaoImpl(@ComponentImport ActiveObjects ao) {
        this.ao = ao;
    }

    /**
     * @param id The ID of the rule to get.
     * @return Get the matchRule by its ID. Can return null if it doesn't exist.
     */
    @Override
    @Nullable
    public MatchRuleEntity getRuleById(int id) {
        MatchRuleEntity[] entity = ao.find(MatchRuleEntity.class, Query.select().where("ID = ?", id));

        if (entity.length == 1) {
            return entity[0];
        } else {
            return null;
        }
    }

    /**
     * @return List of all matchRules
     */
    @Override
    public List<MatchRuleEntity> getAllRules() {
        return newArrayList(ao.find(MatchRuleEntity.class));
    }

    /**
     * Create a new matchRule
     * @param friendlyName The name of the rule that will be presented on the UI
     * @param regexPattern Pattern of the rule to match
     * @param ruleEnabled Whether the rule is enabled (true/false)
     * @return The created matchRule
     * @throws IllegalArgumentException Validation error
     */
    @Override
    public MatchRuleEntity createRule(String friendlyName, String regexPattern, boolean ruleEnabled) throws IllegalArgumentException {
        if (!validateFriendlyName(friendlyName))
            throw new IllegalArgumentException(String.format("Validation failed for the friendlyName of a new MatchRule! Ensure it is between %d-%d characters",
                    MIN_RULE_NAME_CHARS, MAX_RULE_NAME_CHARS));

        if (!validateRegexPattern(friendlyName))
            throw new IllegalArgumentException(String.format("Validation failed for the regexPattern of a new MatchRule! Ensure it is between %d-%d characters",
                    MIN_PATTERN_CHARS, MAX_PATTERN_CHARS));

        final MatchRuleEntity entity = ao.create(MatchRuleEntity.class);
        entity.setFriendlyName(friendlyName);
        entity.setRegexPattern(regexPattern);
        entity.setRuleEnabled(ruleEnabled);
        entity.save();
        ao.flush(entity); // Force caches to update
        return entity;
    }

    /**
     * Updates an existing matchRule
     * @param id The ID of the rule to update
     * @param friendlyName Name of the rule that will be presented on the UI
     * @param regexPattern Regex of the pattern to match
     * @param ruleEnabled Whether the rule should be enabled (True/False)
     * @return The updated rule
     * @throws IllegalArgumentException Validation error.
     */
    @Override
    public MatchRuleEntity updateRule(int id, String friendlyName, String regexPattern, Boolean ruleEnabled) throws IllegalArgumentException {
        MatchRuleEntity entity = ao.find(MatchRuleEntity.class, Query.select().where("ID = ?", id))[0];

        // null means the value isn't being updated (AJS Table) so reuse its original value
        friendlyName = (friendlyName == null) ? entity.getFriendlyName() : friendlyName;
        regexPattern = (regexPattern == null) ? entity.getRegexPattern() : regexPattern;
        ruleEnabled = (ruleEnabled == null) ? entity.isRuleEnabled() : ruleEnabled;

        if (!validateFriendlyName(friendlyName))
            throw new IllegalArgumentException(String.format("Validation failed for the friendlyName of a new MatchRule! Ensure it is between %d-%d characters",
                    MIN_RULE_NAME_CHARS, MAX_RULE_NAME_CHARS));

        if (!validateRegexPattern(friendlyName))
            throw new IllegalArgumentException(String.format("Validation failed for the regexPattern of a new MatchRule! Ensure it is between %d-%d characters",
                    MIN_PATTERN_CHARS, MAX_PATTERN_CHARS));

        entity.setFriendlyName(friendlyName);
        entity.setRegexPattern(regexPattern);
        entity.setRuleEnabled(ruleEnabled);
        entity.save();
        ao.flush(entity); // Force caches to update
        return entity;
    }

    /**
     * @param id Rule ID to delete
     */
    @Override
    public void deleteRule(int id) {
        // Couldn't can't figure out how ao.deleteWithSQL() actually works and the lack of documentation meant this find & delete instead.
        MatchRuleEntity[] matchRuleEntity = ao.find(MatchRuleEntity.class, Query.select().where("ID = ?", id));

        if (matchRuleEntity.length == 1) {
            ao.delete(matchRuleEntity[0]);
            ao.flushAll();
        } else {
            throw new IllegalArgumentException(String.format("Match Rule by the ID: %d was not found!", id ));
        }

    }

    /**
     * Ensure friendlyName given passes a set of validation rules
     * @param friendlyName Name of the rule to validate.
     * @return True - Validation Passed. False otherwise
     */
    private boolean validateFriendlyName(String friendlyName) {
        return (friendlyName.length() >= MIN_RULE_NAME_CHARS && friendlyName.length() <= MAX_RULE_NAME_CHARS);
    }

    /**
     * Ensure Regular Expression
     * @param regexPattern Pattern to validate.
     * @return True - Validation Passed. False otherwise.
     */
    private boolean validateRegexPattern(String regexPattern) {
        // Naive, but we trust the user inputs a valid Regular Expression Pattern.
        return (regexPattern.length() >= MIN_PATTERN_CHARS && regexPattern.length() <= MAX_PATTERN_CHARS);
    }


}
