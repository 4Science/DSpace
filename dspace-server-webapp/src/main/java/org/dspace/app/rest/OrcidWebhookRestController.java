/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.orcid.client.OrcidClient;
import org.dspace.orcid.service.OrcidSynchronizationService;
import org.dspace.orcid.service.OrcidWebhookService;
import org.dspace.orcid.webhook.OrcidWebhookAction;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller that handle the ORCID webhook callback.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@RequestMapping(value = "/api/" + RestModel.CRIS + "/orcid")
@RestController
public class OrcidWebhookRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrcidWebhookRestController.class);

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private OrcidClient orcidClient;

    @Autowired
    private ItemService itemService;

    @Autowired
    private OrcidSynchronizationService orcidSynchronizationService;

    @Autowired
    private OrcidWebhookService orcidWebhookService;

    @Autowired(required = false)
    private List<OrcidWebhookAction> orcidWebhookActions;

    @PostConstruct
    private void postConstruct() {
        if (orcidWebhookActions == null) {
            orcidWebhookActions = Collections.emptyList();
        }
    }

    @PostMapping(value = "/{orcid}/webhook/{token}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void webhook(HttpServletRequest request, @PathVariable(name = "orcid") String orcid,
        @PathVariable(name = "token") String token) {

        String storedToken = configurationService.getProperty("orcid.webhook.registration-token");
        if (!StringUtils.equals(token, storedToken)) {
            LOGGER.warn("Received a webhook callback with a wrong token: " + token);
            return;
        }

        Context context = ContextUtil.obtainContext(request);

        try {
            context.turnOffAuthorisationSystem();
            performWebhookActions(orcid, context);
        } catch (Exception ex) {
            LOGGER.error("An error occurs while processing the webhook call from ORCID", ex);
        } finally {
            context.restoreAuthSystemState();
        }

        try {
            context.complete();
        } catch (SQLException e) {
            LOGGER.error("An error occurs closing the DSpace context", e);
        }

    }

    private void performWebhookActions(String orcid, Context context) throws SQLException, AuthorizeException {
        Iterator<Item> iterator = orcidSynchronizationService.findProfilesByOrcid(context, orcid);
        if (IteratorUtils.isEmpty(iterator)) {
            LOGGER.warn("Received a webhook call from ORCID with an id not associated with any profile: " + orcid);
            orcidWebhookService.unregister(context, orcid);
            return;
        }

        while (iterator.hasNext()) {
            Item profile = iterator.next();
            orcidWebhookActions.forEach(plugin -> plugin.perform(context, profile, orcid));
            itemService.update(context, profile);
        }
    }

    public OrcidClient getOrcidClient() {
        return orcidClient;
    }

    public void setOrcidClient(OrcidClient orcidClient) {
        this.orcidClient = orcidClient;
    }

}
