package com.cyanoth.secretwarden.scanners.pullrequest;

import com.atlassian.bitbucket.content.AbstractChangeCallback;
import com.atlassian.bitbucket.content.Change;
import com.atlassian.bitbucket.content.ChangeType;
import com.atlassian.bitbucket.pull.*;
import com.cyanoth.secretwarden.config.ExcludedPathEntity;
import com.cyanoth.secretwarden.config.MatchRuleEntity;
import com.cyanoth.secretwarden.structures.FoundSecretCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * INTERNAL! Class which streams differences (think different files) of a pull request before passing that information onto DiffMatcher
 */
class ChangeStreamer {
    private static final Logger log = LoggerFactory.getLogger(ChangeStreamer.class);

    private final PullRequestService pullRequestService;
    private final FoundSecretCollection totalFoundSecrets;
    private final HashMap<MatchRuleEntity, Pattern> matchRules;
    private final HashMap<ExcludedPathEntity, Pattern> excludedPaths;

    /**
     * INTERNAL! Class which streams differences (think different files) of a pull request before passing that information onto DiffMatcher
     * @param pullRequestService Initialised Bitbucket PullRequestService for PR operations (stream)
     */
    ChangeStreamer(PullRequestService pullRequestService,
                   HashMap<MatchRuleEntity, Pattern> matchRuleSet,
                   HashMap<ExcludedPathEntity, Pattern> excludedPaths) {
        this.pullRequestService = pullRequestService;
        this.matchRules = matchRuleSet;
        this.excludedPaths = excludedPaths;
        this.totalFoundSecrets = new FoundSecretCollection();
    }

    /**
     * Scan a pull-request for any changed source files. The changed source files then go onto DiffMatcher
     * where the changed code inside the source file will be scanned for secrets. DiffMatcher returns
     * any FoundSecret, all of which get collected into a single set and returned
     * @param pullRequest The pull request to stream changes and search for secrets
     * @return This object for chaining. Returns once the scan is complete.
     */
    public ChangeStreamer scan(PullRequest pullRequest) {
        scanPullRequestChangesForSecrets(pullRequest);
        return this;
    }

    @NotNull
    FoundSecretCollection getFoundSecrets() {
        return totalFoundSecrets;
    }

    @NotNull
    private void scanPullRequestChangesForSecrets(PullRequest pullRequest) {
        PullRequestChangesRequest changeRequest = new PullRequestChangesRequest.Builder(pullRequest).
                changeScope(PullRequestChangeScope.ALL)  // With Scope ALL, it becomes unnecessary to specify the since/until commit range
                .withComments(false)
                .build();

        pullRequestService.streamChanges(changeRequest, new AbstractChangeCallback() {
            @Override
            public boolean onChange(@Nonnull Change change) {
                if (change.getType() == ChangeType.ADD || change.getType() == ChangeType.MODIFY) {

                    boolean shouldScan = true;

                    for (Pattern excludedPath : excludedPaths.values()) {
                        if (excludedPath.matcher(change.getPath().toString()).find()) {
                            log.debug(String.format("Skipping scan of the file: %s because it matches an excluded path!", change.getPath().toString()));
                            shouldScan = false;
                        }
                    }

                    if (shouldScan)
                        totalFoundSecrets.merge(scanChangedFileDifferencesForSecrets(pullRequest, change.getPath().toString()));
                }
                return true; // _Really_ do not care about this value, but its super() requires returning something
            }
        });
    }

    @NotNull
    private FoundSecretCollection scanChangedFileDifferencesForSecrets(PullRequest pullRequest, String targetFilePath) {
        final PullRequestDiffRequest fileDifference = new PullRequestDiffRequest.Builder(pullRequest, targetFilePath)
                .withComments(false)
                .build();

        final DiffMatcher matchSecretCallback = new DiffMatcher(matchRules);
        pullRequestService.streamDiff(fileDifference, matchSecretCallback);
        return matchSecretCallback.getFoundSecrets();
    }

}