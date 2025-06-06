/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import java.util.Locale;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.EPersonBuilder;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test class for supported languages
 *
 * @author Mykhaylo Boychuk (at 4science)
 */
public class LanguageSupportIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void checkDefaultLanguageAnonymousTest() throws Exception {
        getClient().perform(get("/api"))
                   .andExpect(header().stringValues("Content-Language","en"));
    }

    @Test
    @Ignore
    //TODO investigate way the language support introduce such issue
    public void checkEnabledMultipleLanguageSupportTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String[] supportedLanguage = {"uk","it"};
        configurationService.setProperty("webui.supported.locales",supportedLanguage);

        Locale it = new Locale("it");

        EPerson epersonUK = EPersonBuilder.createEPerson(context)
                           .withEmail("epersonUK@example.com")
                           .withPassword(password)
                           .withLanguage("uk")
                           .build();

        EPerson epersonFR = EPersonBuilder.createEPerson(context)
                           .withEmail("epersonFR@example.com")
                           .withPassword(password)
                           .withLanguage("fr")
                           .build();

        context.restoreAuthSystemState();

        String tokenEPersonUK = getAuthToken(epersonUK.getEmail(), password);
        String tokenEPersonFR = getAuthToken(epersonFR.getEmail(), password);

        getClient(tokenEPersonUK).perform(get("/api"))
                                 .andExpect(header().stringValues("Content-Language","uk, it"));

        getClient(tokenEPersonUK).perform(get("/api").locale(it))
                                 .andExpect(header().stringValues("Content-Language","uk, it"));

        getClient(tokenEPersonFR).perform(get("/api").locale(it))
                                 .andExpect(header().stringValues("Content-Language","en"));

        configurationService.setProperty("webui.supported.locales",null);
    }
}
