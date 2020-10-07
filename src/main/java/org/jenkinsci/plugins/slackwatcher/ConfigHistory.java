package org.jenkinsci.plugins.slackwatcher;

import hudson.model.Job;
import hudson.plugins.jobConfigHistory.ConfigInfo;
import hudson.plugins.jobConfigHistory.JobConfigHistory;
import hudson.plugins.jobConfigHistory.JobConfigHistoryProjectAction;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * @author sriganesh
 */
public class ConfigHistory {

    private final JobConfigHistory plugin;

    private static final Logger LOGGER = Logger.getLogger(ConfigHistory.class.getName());

    public ConfigHistory(final JobConfigHistory plugin) {
        this.plugin = plugin;
    }

    public @CheckForNull String lastChangeDiffUrl(final @Nonnull Job<?, ?> job) {

        if (plugin == null) {
            LOGGER.warning("job config history plugin not available");
            return null;
        }

        final List<ConfigInfo> configs = storedConfigurations(job);
        if (configs == null || configs.size() < 2) {
            LOGGER.warning("job config history not available");
            return null;
        }

        return String.format("%sjobConfigHistory/showDiffFiles?timestamp1=%s&timestamp2=%s", job.getAbsoluteUrl(),
                configs.get(1).getDate(), configs.get(0).getDate());
    }

    private @CheckForNull List<ConfigInfo> storedConfigurations(final Job<?, ?> job) {

        final JobConfigHistoryProjectAction action = job.getAction(JobConfigHistoryProjectAction.class);

        if (action == null) {
            LOGGER.warning("No job config history action present");
            return null;
        }

        try {
            return action.getJobConfigs();
        } catch (IOException ex) {
            LOGGER.warning(ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }
}
