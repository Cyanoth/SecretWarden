package com.cyanoth.secretwarden.config.REST;

import com.atlassian.bitbucket.AuthorisationException;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.permission.PermissionValidationService;
import com.atlassian.bitbucket.rest.RestErrorMessage;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.cyanoth.secretwarden.config.*;
import com.cyanoth.secretwarden.scanners.pullrequest.PullRequestSecretScanResultCache;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * Exposed REST endpoints to modify global configuration of the plugin
 *
 * [1] https://developer.atlassian.com/server/framework/atlassian-sdk/rest-plugin-module/
 */
@Path("/globalconfig")
@Scanned
public class GlobalConfigApi {
    private static final Logger log = LoggerFactory.getLogger(GlobalConfigApi.class);
    private final PermissionValidationService permissionValidationService;
    private final PluginConfig pluginConfig;
    private final PullRequestSecretScanResultCache pullRequestSecretScanResultCache;

    @Autowired
    public GlobalConfigApi(@ComponentImport PermissionValidationService permissionValidationService,
                           final PluginConfig pluginConfig,
                           final PullRequestSecretScanResultCache pullRequestSecretScanResultCache) {

        this.permissionValidationService = permissionValidationService;
        this.pluginConfig = pluginConfig;
        this.pullRequestSecretScanResultCache = pullRequestSecretScanResultCache;
    }

    /**
     * GET a single matchRule by ID
     * @param id The unique rule ID to get
     * @return JSON representation of a match rule or 404 if not found.
     */
    @GET
    @Path("/match-secret-rule/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getMatchSecretRule(@PathParam("id") int id) {
        MatchRuleEntity rule = pluginConfig.getMatchRuleDao().getRuleById(id);
        if (rule == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            MatchRuleJson ruleForJson = new MatchRuleJson(rule.getID(), rule.getFriendlyName(), rule.getRegexPattern(), rule.isRuleEnabled());
            return Response.ok(new Gson().toJson(ruleForJson)).build();
        }
    }

    /**
     * GET all matchRules
     * @return JSON List of every MatchRule. Can be empty if no rules exist.
     */
    @GET
    @Path("/match-secret-rule")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getMatchSecretRules() {
        List<MatchRuleEntity> matchRules = pluginConfig.getMatchRuleDao().getAllRules();
        ArrayList<MatchRuleJson> rulesForJson = new ArrayList<>();
        for (MatchRuleEntity entity : matchRules) {
            rulesForJson.add(new MatchRuleJson(entity.getID(), entity.getFriendlyName(), entity.getRegexPattern(), entity.isRuleEnabled()));
        }

        return Response.ok(new Gson().toJson(rulesForJson)).build();
    }

    /**
     * Creates a new matchRule.
     * @param rule JSON Representation of the rule.
     * @return 200 - Rule Created. 400 - Validation Error. 401 - Forbidden (requires global admin) 500 - Unhandled Exception
     */
    @POST
    @Path("/match-secret-rule")
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response createMatchRule(MatchRuleJson rule) {
        try {
            this.permissionValidationService.validateForGlobal(Permission.ADMIN);
            log.debug("SecretWarden: Creating a new MatchRule");
            MatchRuleEntity result = pluginConfig.getMatchRuleDao().createRule(rule.friendlyName, rule.regexPattern, true);
            return Response.ok("{ \"id\": \"" + result.getID() + "\" }").build();
        } catch (AuthorisationException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch(IllegalArgumentException e) {
            log.warn("Failed to validate MatchRule", e);
            return Response.status(Response.Status.BAD_REQUEST).entity(new RestErrorMessage(e.toString())).build();
        } catch (Exception e) { // Catch-all here so we don't send StackTraces to the Client on unhandled Exceptions
            log.error("An exception occurred updating a MatchSecretRule", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new RestErrorMessage("Internal Server Error - " +
                    "Check Server Logs for Details")).build();
        }
    }

    /**
     * Updates a matchRule.
     * @param rule JSON Representation of the rule.
     * @return 200 - Rule Created. 400 - Validation Error. 401 - Forbidden (requires global admin) 500 - Unhandled Exception
     */
    @PUT
    @Path("/match-secret-rule/{id}")
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response updateMatchRule(@PathParam("id") int id, MatchRuleJson rule) {
        try {
            this.permissionValidationService.validateForGlobal(Permission.ADMIN);

            log.debug(String.format("SecretWarden: Updating an Existing Match Rule: %d", rule.id));

            pluginConfig.getMatchRuleDao().updateRule(rule.id, rule.friendlyName, rule.regexPattern, rule.enabled);
            return Response.ok("{}").build(); //AJS Restful Table response >requires< JSON response as the OK
        } catch (AuthorisationException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch(IllegalArgumentException e) {
            log.warn("Failed to validate MatchRule", e);
            return Response.status(Response.Status.BAD_REQUEST).entity(new RestErrorMessage(e.toString())).build();
        } catch (Exception e) { // Catch-all here so we don't send StackTraces to the Client on unhandled Exceptions
            log.error("An exception occurred updating a MatchSecretRule", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Deletes a MatchRule by ID
     * @param id The ID of the MatchRule to delete
     * @return 200 - Rule Deleted. 401 - Forbidden. 404 - Rule Not Found. 500 - Unhandled Exception
     */
    @DELETE
    @Path("/match-secret-rule/{id}")
    public Response deleteMatchRule(@PathParam("id") int id) {
        try {
            this.permissionValidationService.validateForGlobal(Permission.ADMIN);

            log.debug(String.format("SecretWarden: Deleting a Match Rule: %d", id));
            pluginConfig.getMatchRuleDao().deleteRule(id);
            return Response.ok("{}").build();
        } catch (AuthorisationException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch(IllegalArgumentException e) {
            log.warn(String.format("Could not find the Excluded rule by the id: %d", id), e);
            return Response.status(Response.Status.NOT_FOUND).entity(new RestErrorMessage(e.toString())).build();
        } catch (Exception e) { // Catch-all here so we don't send StackTraces to the Client on unhandled Exceptions
            log.error("An exception occurred updating a MatchSecretRule", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Gets a single excluded path in JSON by ID.
     * @param id The ID of the excluded path
     * @return JSON representation of an excluded path. 404 if not found
     */
    @GET
    @Path("/excluded-path/{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getExcludedPath(@PathParam("id") int id) {
        ExcludedPathEntity entity = pluginConfig.getExcludePathDao().getExcludedPathById(id);
        if (entity == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            ExcludedPathJson excludedPathForJson = new ExcludedPathJson(entity.getID(), entity.getExcludedPath());
            return Response.ok(new Gson().toJson(excludedPathForJson)).build();
        }
    }

    /**
     * GET all excludedPaths
     * @return List of JSON objects containing all the excludedPaths. Can be empty if none exist.
     */
    @GET
    @Path("/excluded-path")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getExcludedPaths() {
        List<ExcludedPathEntity>  excludedPaths = pluginConfig.getExcludePathDao().getAllExcludedPaths();
        ArrayList<ExcludedPathJson> excludedPathsForJson = new ArrayList<>();
        for (ExcludedPathEntity entity : excludedPaths) {
            excludedPathsForJson.add(new ExcludedPathJson(entity.getID(), entity.getExcludedPath()));
        }

        return Response.ok(new Gson().toJson(excludedPathsForJson)).build();
    }

    /**
     * Creates or updates a excludedPath.
     * @param excludedPath JSON representation of excludedPath to create
     * @return 200 - Rule Created. 400 - Validation Error. 401 - Forbidden (requires global admin) 500 - Unhandled Exception
     */
    @POST
    @Path("/excluded-path")
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response createExcludedPath(ExcludedPathJson excludedPath) {
        try {
            this.permissionValidationService.validateForGlobal(Permission.ADMIN);
            log.debug("SecretWarden: Creating a new excluded path");
            ExcludedPathEntity result = pluginConfig.getExcludePathDao().createExcludedPath(excludedPath.excludedPath);
            return Response.ok("{ \"id\": \"" + result.getID() + "\" }").build();
        } catch (AuthorisationException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch(IllegalArgumentException e) {
            log.warn("Failed to validate MatchRule", e);
            return Response.status(Response.Status.BAD_REQUEST).entity(new RestErrorMessage(e.toString())).build();
        } catch (Exception e) { // Catch-all here so we don't send StackTraces to the Client on unhandled Exceptions
            log.error("An exception occurred creating a Excluded Path", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new RestErrorMessage("Internal Server Error - " +
                    "Check Server Logs for Details")).build();
        }
    }

    /**
     * Creates or updates a excludedPath.
     * @param excludedPath JSON representation of excludedPath to create
     * @return 200 - Rule Created. 400 - Validation Error. 401 - Forbidden (requires global admin) 500 - Unhandled Exception
     */
    @PUT
    @Path("/excluded-path/{id}")
    @Consumes({ MediaType.APPLICATION_JSON })
    public Response updateExcludedPath(@PathParam("id") int id, ExcludedPathJson excludedPath) {
        try {
            this.permissionValidationService.validateForGlobal(Permission.ADMIN);
            log.debug(String.format("SecretWarden: Updating an Existing Excluded Path: %d", excludedPath.id));
            pluginConfig.getExcludePathDao().updateExcludedPath(excludedPath.id, excludedPath.excludedPath);
            return Response.ok("{}").build(); //AJS Restful Table response >requires< JSON response as the OK
        } catch (AuthorisationException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch(IllegalArgumentException e) {
            log.warn("Failed to validate MatchRule", e);
            return Response.status(Response.Status.BAD_REQUEST).entity(new RestErrorMessage(e.toString())).build();
        } catch (Exception e) { // Catch-all here so we don't send StackTraces to the Client on unhandled Exceptions
            log.error("An exception occurred updating a Excluded Path", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new RestErrorMessage("Internal Server Error -" +
                    " Check Server Logs for Details")).build();
        }
    }

    /**
     * Deletes an ExcludedPath by ID.
     * @param id The ExcludedPath to Delete
     * @return 200 - Deleted. 401 - Forbidden. 404 - Invalid ID. 500 - Unhandled Exception
     */
    @DELETE
    @Path("/excluded-path/{id}")
    public Response deleteExcludedPath(@PathParam("id") int id) {
        try {
            this.permissionValidationService.validateForGlobal(Permission.ADMIN);

            log.debug(String.format("SecretWarden: Deleting a Excluded Path: %d", id));
            pluginConfig.getExcludePathDao().deleteExcludedPath(id);
            return Response.ok("{}").build();
        } catch (AuthorisationException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch(IllegalArgumentException e) {
            log.warn(String.format("Could not find the Match rule by the id: %d", id), e);
            return Response.status(Response.Status.NOT_FOUND).entity(new RestErrorMessage(e.toString())).build();
        } catch (Exception e) { // Catch-all here so we don't send StackTraces to the Client on unhandled Exceptions
            log.error("An exception occurred deleting an Exlucded Path", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new RestErrorMessage("Internal Server Error - " +
                    "Check Server Logs for Details")).build();
        }
    }

    /**
     * @return Response 200 if the secret scan result cache has been cleared. HTTP error otherwise.
     */
    @PUT
    @Path("/clear-result-cache")
    @Produces({ MediaType.TEXT_PLAIN })
    public Response clearResultCache() {
        try {
            this.permissionValidationService.validateForGlobal(Permission.ADMIN);
            try {
                log.debug("Clearing SecretWarden Result Cache...");
                pullRequestSecretScanResultCache.clear();
                return Response.ok("The secret result cache has been cleared!").build();
            }
            catch (Exception e) {
                log.error("Failed to clear SecretWarden result cache! An error occurred.", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new RestErrorMessage("The secret result cache has NOT been cleared. " +
                        "An error occurred, see server-logs for more information")).build();
            }
        } catch (AuthorisationException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }
}
