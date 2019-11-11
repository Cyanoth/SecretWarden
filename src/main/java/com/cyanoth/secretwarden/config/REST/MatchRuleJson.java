package com.cyanoth.secretwarden.config.REST;

import org.codehaus.jackson.annotate.JsonProperty;
import javax.validation.constraints.NotNull;

/**
 * Structure for the MatchRule config that will be passed as JSON to/from the API.
 */
public class MatchRuleJson {
    final Integer id;
    final String friendlyName;
    final String regexPattern;
    final Boolean enabled;

    MatchRuleJson(@JsonProperty("id") Integer id,
                  @JsonProperty("friendlyName") @NotNull String friendlyName,
                  @JsonProperty("regexPattern") @NotNull String regexPattern,
                  @JsonProperty("enabled") @NotNull Boolean enabled) {
        this.id = id;
        this.friendlyName = friendlyName;
        this.regexPattern = regexPattern;
        this.enabled = enabled;
    }

}
