/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.subscriptions;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;


/**
 * Implementation class of SubscriptionGenerator
 * which will handle the logic of sending the emails
 * in case of 'content' subscriptionType
 */
public class ContentGenerator {

    private final Logger log = LogManager.getLogger(ContentGenerator.class);
    private final ConfigurationService configurationService = DSpaceServicesFactory.getInstance()
                                                                                   .getConfigurationService();

    private Map<String, StreamDisseminationCrosswalk> entityType2Disseminator;

    public void notifyForSubscriptions(EPerson ePerson,
                                       List<SubscriptionItem> indexableComm,
                                       List<SubscriptionItem> indexableColl,
                                       Map<String, List<SubscriptionItem>> indexableEntityByType) {
        try {
            if (Objects.nonNull(ePerson)) {
                Locale supportedLocale = I18nUtil.getEPersonLocale(ePerson);
                Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "subscriptions_content"));
                email.addRecipient(ePerson.getEmail());
                String bodyCommunities = generateBodyMail("Community", indexableComm);
                String bodyCollections = generateBodyMail("Collection", indexableColl);
                if (bodyCommunities.equals(EMPTY) && bodyCollections.equals(EMPTY)) {
                    log.debug("subscription(s) of eperson {} do(es) not match any new items: nothing to send" +
                                  " - exit silently", ePerson::getID);
                    return;
                }
                email.addArgument(configurationService.getProperty("subscription.url"));
                email.addArgument(bodyCommunities);
                email.addArgument(bodyCollections);
                email.addArgument(
                    indexableEntityByType.entrySet().stream()
                                         .map(entry -> generateBodyMail(entry.getKey(), entry.getValue()))
                                         .collect(Collectors.joining("\n\n"))
                );
                email.send();
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            log.warn("Cannot email user eperson_id: {} eperson_email: {}", ePerson::getID, ePerson::getEmail);
        }
    }

    private String generateBodyMail(String type, List<SubscriptionItem> subscriptionItems) {
        if (subscriptionItems == null || subscriptionItems.isEmpty()) {
            return EMPTY;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(("\nYou have " + subscriptionItems.size() + " subscription(s) active to type " + type + "\n")
                          .getBytes(UTF_8));
            for (SubscriptionItem item : subscriptionItems) {
                out.write("\n".getBytes(UTF_8));
                out.write("List of new content for the\n".getBytes(UTF_8));
                out.write((type + " \"" + item.getName() + "\" - " + item.getUrl() + "\n")
                              .getBytes(UTF_8));

                for (Entry<String, String> entry : item.getItemUrlsByItemName().entrySet()) {
                    out.write("\n".getBytes(UTF_8));
                    out.write((entry.getKey() + " - " + entry.getValue()).getBytes(UTF_8));
                }
                //Optional.ofNullable(entityType2Disseminator.get(type))
                //        .orElseGet(() -> entityType2Disseminator.get("Item"))
                //        .disseminate(context, item, out);
            }
            out.close();
            return out.toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return EMPTY;
    }

    public void setEntityType2Disseminator(Map<String, StreamDisseminationCrosswalk> entityType2Disseminator) {
        this.entityType2Disseminator = entityType2Disseminator;
    }

}
