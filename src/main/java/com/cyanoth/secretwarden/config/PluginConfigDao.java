package com.cyanoth.secretwarden.config;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import net.java.ao.Implementation;
import org.springframework.beans.factory.annotation.Autowired;

@Transactional
@Implementation(PluginConfigDaoImpl.class)
public interface PluginConfigDao {

    public PluginConfigEntity getConfigOrCreate();

    public PluginConfigEntity getConfig();

    public void saveConfig(PluginConfigEntity entity);

}
