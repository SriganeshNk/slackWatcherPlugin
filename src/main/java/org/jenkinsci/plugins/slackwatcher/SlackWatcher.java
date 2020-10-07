package org.jenkinsci.plugins.slackwatcher;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.listeners.ItemListener;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * Notify whenever Job configuration changes.
 *
 * Sends slack notification to the list of recipients on following events:
 * onRenamed, onUpdated and onDeleted.
 *
 * @author sriganesh
 */
@Extension
public class SlackWatcher extends ItemListener implements Describable<SlackWatcher> {

    private final @Nonnull JobConfigSlackNotifier slacker;

    private static final Logger LOGGER = Logger.getLogger(SlackWatcher.class.getName());

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public SlackWatcher() {
        this(new JobConfigSlackNotifier());
    }

    public SlackWatcher(final JobConfigSlackNotifier jobConfigSlackNotifier) {

        if (jobConfigSlackNotifier == null)
            throw new IllegalArgumentException("No slack notifier provided");

        this.slacker = jobConfigSlackNotifier;
    }

    public Descriptor<SlackWatcher> getDescriptor() {
        return getDescriptorImpl();
    }

    public DescriptorImpl getDescriptorImpl() {
        return (DescriptorImpl) Jenkins.get().getDescriptorOrDie(SlackWatcher.class);
    }

    @Override
    public void onRenamed(Item item, String oldName, String newName) {

        if (!(item instanceof Job<?, ?>)) {
            LOGGER.log(Level.FINEST, item.getDisplayName() + " Not a job rename");
            return;
        }

        LOGGER.log(Level.FINEST, item.getDisplayName() + " Job renamed, sending notifications");

        getNotification()
                .message(
                        "renamed from " + oldName + " to <" + newName + "|" + ((Job<?, ?>) item).getAbsoluteUrl() + ">")
                .send(item);

    }

    @Override
    public void onUpdated(Item item) {

        if (!(item instanceof Job<?, ?>)) {
            LOGGER.log(Level.FINEST, item.getDisplayName() + " Not a job config update");
            return;
        }

        LOGGER.log(Level.FINEST, item.getDisplayName() + " Job config updated, sending notifications");

        getNotification().message("updated <" + item.getDisplayName() + "|" + ((Job<?, ?>) item).getAbsoluteUrl() + ">")
                .send(item);
    }

    @Override
    public void onDeleted(Item item) {

        if (!(item instanceof Job<?, ?>)) {
            LOGGER.log(Level.FINEST, item.getDisplayName() + " Not a job config update");
            return;
        }

        LOGGER.log(Level.FINEST, item.getDisplayName() + " Job deleted, sending notifications");

        getNotification().message("deleted <" + item.getDisplayName() + "|" + ((Job<?, ?>) item).getAbsoluteUrl() + ">")
                .color("danger").send(item);
    }

    private Notification.Builder getNotification() {
        return new Notification.Builder(slacker);
    }

    private static class Notification extends SlackNotification {

        private final @Nonnull Job<?, ?> job;

        public Notification(final Builder builder) {
            super(builder);
            job = builder.job;
        }

        @Override
        protected String getMessage() {
            return super.getMessage();
        }

        protected String getColor() {
            return super.getColor();
        }

        private static class Builder extends SlackNotification.Builder {

            private Job<?, ?> job;

            public Builder(final JobConfigSlackNotifier slacker) {

                super(slacker);
            }

            @Override
            public void send(final Object o) {
                LOGGER.log(Level.FINEST, "Actually sending messages");

                job = (Job<?, ?>) o;

                new Notification(this).send(job);
            }
        }
    }

    @Extension
    @Symbol("slackWatcher")
    public static final class DescriptorImpl extends Descriptor<SlackWatcher> {

        private boolean watcherEnabled;

        public DescriptorImpl() {
            try {
                load();
            } catch (NullPointerException e) {
                LOGGER.warning("unable to load the slack watcher plugin: " + e.getMessage());
            }
        }

        public String getDisplayName() {
            return "Notify Slack on Config changes";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) {
            req.bindJSON(this, formData);

            save();

            return true;
        }

        public boolean getWatcherEnabled() {
            return this.watcherEnabled;
        }

        @DataBoundSetter
        public void setWatcherEnabled(boolean watcherEnabled) {
            this.watcherEnabled = watcherEnabled;
        }
    }
}
