package com.cyanoth.secretwarden.config;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Table;

@Table(value="PluginConfig")
@Preload
public interface PluginConfigEntity extends Entity {
    // boolean isn't a valid property in AO. So Booleans here are actually strings

    String getHasDefaultRulesAdded();
    void setHasDefaultRulesAdded(String defaultRulesAdded);

}
