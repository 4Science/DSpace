/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.action;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import javax.mail.MessagingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.services.ConfigurationService;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class EmailAlertDOIAction<T extends DSpaceObject> {

    private static final Logger log = LogManager.getLogger(EmailAlertDOIAction.class);

    public static final String DOI_MAINTENANCE_ERROR = "doi_maintenance_error";

    final ConfigurationService configurationService;
    final T dspaceObject;
    final DSpaceObjectService<T> dspaceObjectService;
    final String emailTemplate;

    EmailAlertDOIAction(
        ConfigurationService configurationService,
        DSpaceObjectService<T> dspaceObjectService,
        T dspaceObject
    ) {
        this(configurationService, dspaceObjectService, dspaceObject, DOI_MAINTENANCE_ERROR);
    }

    EmailAlertDOIAction(
        ConfigurationService configurationService,
        DSpaceObjectService<T> dspaceObjectService,
        T dspaceObject,
        String emailTemplate
    ) {
        this.dspaceObject = dspaceObject;
        this.configurationService = configurationService;
        this.dspaceObjectService = dspaceObjectService;
        this.emailTemplate = emailTemplate;
    }

    /**
     * Send an alert email to the configured recipient when DOI operations encounter an error
     *
     * @param action - action being attempted (eg. reserve, register, update)
     * @param doi    - DOI for this operation
     * @param reason - failure reason or error message
     * @throws IOException
     */
    void sendAlertMail(String action, String doi, String reason) {
        String recipient = configurationService.getProperty("alert.recipient");

        try {
            if (recipient != null) {
                Email email = Email.getEmail(I18nUtil.getEmailFilename(Locale.getDefault(), emailTemplate));
                email.addRecipient(recipient);
                email.addArgument(action);
                email.addArgument(new Date());
                email.addArgument(dspaceObjectService.getTypeText(dspaceObject));
                email.addArgument(dspaceObject.getID().toString());
                email.addArgument(doi);
                email.addArgument(reason);
                email.send();

                //if (!quiet) {
                System.err.println("Email alert is sent.");
                //}
            }
        } catch (IOException | MessagingException e) {
            log.warn("Unable to send email alert", e);
//            if (!quiet) {
            System.err.println("Unable to send email alert.");
            //          }
        }
    }
}
