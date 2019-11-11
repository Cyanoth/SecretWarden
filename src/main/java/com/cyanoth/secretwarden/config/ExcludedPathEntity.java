package com.cyanoth.secretwarden.config;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table(value="ExcludedPaths")
@Preload
public interface ExcludedPathEntity extends Entity {

    @StringLength(value = 255)
    String getExcludedPath();

    void setExcludedPath(String excludedPath);
}
