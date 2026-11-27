/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.util.Locale;
import javax.mail.MessagingException;
import javax.mail.Transport;

import org.dspace.AbstractUnitTest;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Perform some basic unit tests for Email Class
 *
 * @author adamo.fapohunda at 4science.com
 */
public class EmailTest extends AbstractUnitTest {

    private ConfigurationService configurationService;
    private Locale supportedLocale;

    @Before
    public void setup() {
        configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();
        supportedLocale = Locale.ENGLISH;
        // Ensure that mail is "sent".
        configurationService.setProperty("mail.server.disabled", "false");
    }

    @After
    public void tearDown() {
        configurationService.reloadConfig();
    }


    @Test
    public void testTemplateDisabledProperty_True() throws IOException, MessagingException {

        configurationService.setProperty("mail.template.request_item.admin.disabled", "true");
        Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "request_item.admin"));

        // Fill the message's placeholders.
        email.addArgument("bitstreamName");
        email.addArgument("handle");
        email.addArgument("Request token");
        email.addArgument("name");
        email.addArgument("email@email.com");

        // Mock Transport.send() using MockedStatic
        try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
            // Test the logic when the template is disabled
            email.send();
            // Verify that Transport.send() was never called because the template is disabled
            mockedTransport.verifyNoInteractions();
        }

    }

    @Test
    public void testTemplateDisabledProperty_False() throws IOException, MessagingException {

        configurationService.setProperty("mail.template.request_item.admin.disabled", "false");
        Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "request_item.admin"));

        email.addArgument("bitstreamName");
        email.addArgument("handle");
        email.addArgument("Request token");
        email.addArgument("name");
        email.addArgument("email@email.com");

        try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
            email.send();

            // Verify that Transport.send() was called
            mockedTransport.verify(() -> Transport.send(any()), times(1));
        }
    }

    @Test
    public void testTemplateDisabledProperty_Missing() throws IOException, MessagingException {

        // Ensure the property is NOT set at all
        configurationService.setProperty("mail.template.request_item.admin.disabled", null);
        Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "request_item.admin"));

        email.addArgument("bitstreamName");
        email.addArgument("handle");
        email.addArgument("Request token");
        email.addArgument("name");
        email.addArgument("email@email.com");

        try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
            email.send();

            // Verify that Transport.send() was called since the default behavior is enabled
            mockedTransport.verify(() -> Transport.send(any()), times(1));
        }
    }

}
