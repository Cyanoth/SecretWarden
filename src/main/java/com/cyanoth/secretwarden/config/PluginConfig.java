package com.cyanoth.secretwarden.config;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.cyanoth.secretwarden.config.REST.MatchRuleJson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Plugin Configuration Component. Inject to permit modules to access plugin configuration.
 */
@Scanned
@Component("PluginConfig")
public class PluginConfig implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(PluginConfig.class);

    private final static String PLUGIN_KEY = "com.cyanoth.secretwarden";
    private final PluginConfigDao pluginConfigDao;
    private final MatchRuleDao matchRuleDao;
    private final ExcludedPathDao excludedPathDao;
    private final EventPublisher eventPublisher;

    @Autowired
    public PluginConfig(@ComponentImport EventPublisher eventPublisher,
                        PluginConfigDao pluginConfigDao,
                        MatchRuleDao matchRuleDao,
                        ExcludedPathDao excludedPathDao) {
        this.pluginConfigDao = pluginConfigDao;
        this.matchRuleDao = matchRuleDao;
        this.excludedPathDao = excludedPathDao;
        this.eventPublisher = eventPublisher;
    }

    /**
     * @return MatchRuleDao to manipulate or fetch Match Rules
     */
    public MatchRuleDao getMatchRuleDao() {
        return matchRuleDao;
    }

    /**
     * @return ExcludedPathDao to manipulate or fetch Excluded Paths
     */
    public ExcludedPathDao getExcludePathDao() {
        return excludedPathDao;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        eventPublisher.register(this);
    }

    @Override
    public void destroy() throws Exception {
        eventPublisher.unregister(this);
    }

    /**
     * When the plugin is installed or enabled in the UPM this will be invoked.
     * @param pluginEnabledEvent Event to listen for (pluginEnabled). Note this listens for any enable plugin.
     */
    @EventListener
    public void onPluginStarted(final PluginEnabledEvent pluginEnabledEvent)
    {
        String startUpPluginKey = pluginEnabledEvent.getPlugin().getKey();
        if (PLUGIN_KEY.equals(startUpPluginKey))
        {
            log.info("SecretWarden has been enabled!");
            PluginConfigEntity config = pluginConfigDao.getConfigOrCreate();

            // On plugin first load, the default rules won't be in AO. So add them
            // and flag we've added them.
            if (config.getHasDefaultRulesAdded().equals("false")) {
                createDefaultRuleset();
                config.setHasDefaultRulesAdded("true");
                pluginConfigDao.saveConfig(config);
            }
        }
    }

    /**
     * Converts defaults/secret_ruleset.json into AO for a default set of rules.
     */
    private void createDefaultRuleset()  {
        log.info("SecretWarden is recreating the default ruleset...");

        final InputStream inFile = MatchRuleJson.class.getResourceAsStream("/defaults/secret_ruleset.json");
        final JsonArray rules = new JsonParser()
                .parse(new InputStreamReader(inFile, StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonArray("rules");

        for (JsonElement rule : rules ) {
            final JsonObject ruleObj = rule.getAsJsonObject();
            try {
                log.debug(String.format(String.format("Creating a rule for: %s", ruleObj.get("friendly_name"))));
                matchRuleDao.createRule(ruleObj.get("friendly_name").getAsString(), ruleObj.get("regex_pattern").getAsString(), true);
            }
            catch (Exception e) {
                log.warn("An exception occurred trying to create a default rule.", e);
            }
        }
        log.info(String.format("SecretWarden created the default ruleset successfully"));
    }
}
