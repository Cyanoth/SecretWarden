package com.cyanoth.secretwarden.config;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table(value="MatchRules")
@Preload
public interface MatchRuleEntity extends Entity {

    @StringLength(value=100)
    String getFriendlyName();

    void setFriendlyName(String friendlyName);

    @StringLength(value=StringLength.UNLIMITED) // AO allows: 255 (default) or 450 or "Unlimited". Pre-validation restricts this to 1000
    String getRegexPattern();

    void setRegexPattern(String regexPattern);

    boolean isRuleEnabled();

    void setRuleEnabled(boolean ruleEnabled);

}


