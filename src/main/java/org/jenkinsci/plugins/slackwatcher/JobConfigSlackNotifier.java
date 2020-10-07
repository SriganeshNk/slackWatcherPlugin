package org.jenkinsci.plugins.slackwatcher;

import hudson.ExtensionList;
import hudson.model.Job;
import hudson.plugins.jobConfigHistory.JobConfigHistory;
import hudson.util.Secret;
import java.util.logging.Logger;
import javax.mail.MessagingException;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;

import jenkins.model.Jenkins;
import jenkins.plugins.slack.SlackNotifier;
import jenkins.plugins.slack.SlackService;
import jenkins.plugins.slack.StandardSlackService;
import jenkins.plugins.slack.StandardSlackServiceBuilder;

import org.jenkinsci.plugins.plaincredentials.StringCredentials;

/**
 * Send slack notification.
 *
 * @author sriganesh
 */
public class JobConfigSlackNotifier {

    private static final Logger LOGGER = Logger.getLogger(JobConfigSlackNotifier.class.getName());

    /**
     * Send the notification
     *
     * @return sent message or null if notification was not sent
     */
    public boolean send(final SlackNotification notification, Job<?, ?> job) throws MessagingException {

        Jenkins jenkins = Jenkins.getInstanceOrNull();

        if (jenkins == null) {
            LOGGER.info("jenkins instance not initialized");
            return false;
        }

        SlackNotifier.DescriptorImpl sd = jenkins.getDescriptorByType(SlackNotifier.DescriptorImpl.class);

        if (sd == null) {
            LOGGER.info("no SlackNotifier plugin found");
            return false;
        }

        String token = null;

        SystemCredentialsProvider.ProviderImpl system = ExtensionList.lookup(CredentialsProvider.class)
                .get(SystemCredentialsProvider.ProviderImpl.class);

        if (system == null) {
            LOGGER.info("credentials provider not present");
            return false;
        }

        CredentialsStore sysStore = system.getStore(jenkins);

        if (sysStore == null) {
            LOGGER.info("credentials store not present");
            return false;
        }

        for (Domain d : sysStore.getDomains()) {
            for (Credentials c : sysStore.getCredentials(d)) {

                if (!(c instanceof StringCredentials))
                    continue;

                StringCredentials stringCredentials = (StringCredentials) c;

                if (stringCredentials.getId().equals(sd.getTokenCredentialId())) {
                    LOGGER.info("Found Slack Credentials. Preparing to send notification");
                    token = Secret.toString(stringCredentials.getSecret());
                }

            }
        }

        SlackService service = new StandardSlackService(
                new StandardSlackServiceBuilder().withBaseUrl(sd.getBaseUrl()).withTeamDomain(sd.getTeamDomain())
                        .withBotUser(sd.isBotUser()).withRoomId(sd.getRoom()).withReplyBroadcast(false)
                        .withIconEmoji(sd.getIconEmoji()).withUsername(sd.getUsername()).withPopulatedToken(token)
                        .withNotifyCommitters(false).withSlackUserIdResolver(sd.getSlackUserIdResolver()));

        LOGGER.info("Initialized Slack Standard Service, publishing messages now");

        ConfigHistory configHistory = new ConfigHistory((JobConfigHistory) jenkins.getPlugin("jobConfigHistory"));

        boolean postMessage = service.publish("<@" + Jenkins.getAuthentication().getName() + "> "
                + notification.getMessage() + "\n" + configHistory.lastChangeDiffUrl(job));

        LOGGER.info("message published " + postMessage);

        return postMessage;
    }
}
