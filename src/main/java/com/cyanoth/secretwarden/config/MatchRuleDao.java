package com.cyanoth.secretwarden.config;

import com.atlassian.activeobjects.tx.Transactional;
import net.java.ao.Implementation;

import java.util.List;

@Transactional
@Implementation(MatchRuleDaoImpl.class)
public interface MatchRuleDao {

    MatchRuleEntity getRuleById(int id);

    List<MatchRuleEntity> getAllRules();

    MatchRuleEntity createRule(String friendlyName, String regexPattern, boolean ruleEnabled) throws IllegalArgumentException;

    MatchRuleEntity updateRule(int id, String friendlyName, String regexPattern, Boolean ruleEnabled) throws IllegalArgumentException;

    void deleteRule(int id);

}
