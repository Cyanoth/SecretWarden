package com.cyanoth.secretwarden.config.REST;

import org.codehaus.jackson.annotate.JsonProperty;
import javax.validation.constraints.NotNull;

/**
 * Structure for the ExcludePath config that will be passed as JSON to/from the API.
 */
class ExcludedPathJson {
    final Integer id;
    final String excludedPath;

    ExcludedPathJson(@JsonProperty("id") Integer id,
                     @JsonProperty("excludedPath") @NotNull String excludedPath) {
        this.id = id;
        this.excludedPath = excludedPath;
    }

}
