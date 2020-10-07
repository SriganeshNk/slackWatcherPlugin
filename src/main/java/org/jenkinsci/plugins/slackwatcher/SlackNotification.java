package org.jenkinsci.plugins.slackwatcher;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;

import hudson.model.Job;
import net.sf.json.JSONArray;

/**
 * Slack notification for Jenkins.
 *
 * @author sriganesh
 */
public abstract class SlackNotification {

    private static final Logger LOGGER = Logger.getLogger(SlackNotification.class.getName());

    private static final String PLUGIN = "job-config-change-slack-notifier-plugin: ";

    final private String message;
    final private JSONArray attachments;

    final protected JobConfigSlackNotifier watcher;

    public SlackNotification(final Builder builder) {
        this.message = builder.message;
        this.attachments = builder.attachments;
        this.watcher = builder.watcher;
    }

    protected String getMessage() {
        return message;
    }

    protected JSONArray getAttachments() {
        return attachments;
    }

    public final void send(Job<?, ?> job) {
        try {
            final boolean sent = watcher.send(this, job);
            if (sent) {
                log(PLUGIN + "notified: " + this.getMessage());
            } else {
                log(PLUGIN + "unable to notify: " + this.getMessage());
            }
        } catch (AddressException ex) {
            log(PLUGIN + "unable to parse address", ex);
        } catch (MessagingException ex) {
            log(PLUGIN + "unable to notify", ex);
        }
    }

    private void log(String state) {
        LOGGER.log(Level.INFO, state);
    }

    private void log(String state, Throwable ex) {
        LOGGER.log(Level.INFO, state, ex);
    }

    public static abstract class Builder {

        final protected JobConfigSlackNotifier watcher;

        private String message = "";
        private JSONArray attachments;

        public Builder(final JobConfigSlackNotifier watcher) {
            this.watcher = watcher;
        }

        public Builder message(final String message) {
            this.message = message;
            return this;
        }

        public Builder attachments(final JSONArray attachments) {
            this.attachments = attachments;
            return this;
        }

        abstract public void send(final Object object);
    }
}
