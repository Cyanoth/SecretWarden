package com.cyanoth.secretwarden.config;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import net.java.ao.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Scanned
@Component("PluginConfigDao")
public class PluginConfigDaoImpl implements PluginConfigDao {
    private static final Logger log = LoggerFactory.getLogger(PluginConfigDaoImpl.class);

    private final ActiveObjects ao;

    @Autowired
    public PluginConfigDaoImpl(@ComponentImport ActiveObjects ao) {
        this.ao = ao;
    }

    /**
     * Creates an empty AO configuration entry
     * @return Created Config.
     */
    private PluginConfigEntity createConfig() {
        PluginConfigEntity pluginConfigEntity = ao.create(PluginConfigEntity.class);
        pluginConfigEntity.setHasDefaultRulesAdded("false");
        pluginConfigEntity.save();
        ao.flushAll();
        return pluginConfigEntity;
    }

    /**
     * Checks whether the config AO has been created, if so return that. Otherwise create it.
     * @return Config AO.
     */
    @Override
    public PluginConfigEntity getConfigOrCreate() {
        PluginConfigEntity[] config = ao.find(PluginConfigEntity.class, Query.select().where("ID = ?", 1));
        if (config.length == 0) {
            createConfig();
            config = ao.find(PluginConfigEntity.class, Query.select().where("ID = ?", 1));
        }
        return config[0];
    }

    /**
     * @return Config AO.
     */
    @Override
    public PluginConfigEntity getConfig() {
        return ao.find(PluginConfigEntity.class, Query.select().where("ID = ?", 1))[0];
    }

    /**
     * Save updated configuration and flush caches.
     * @param entity The new config.
     */
    @Override
    public void saveConfig(PluginConfigEntity entity) {
        entity.save();
        ao.flushAll();
    }
}