/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import static java.util.Objects.nonNull;

import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.DataUnpaywall;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.unpaywall.model.Unpaywall;
import org.dspace.unpaywall.service.UnpaywallService;
import org.dspace.web.ContextUtil;

/**
 * Unpaywall submission step.
 */
public class UnpaywallStep extends AbstractProcessingStep {

    private final UnpaywallService unpaywallService = ContentServiceFactory.getInstance().getUnpaywallService();

    private final ConfigurationService configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    @Override
    public DataUnpaywall getData(
            SubmissionService submissionService,
            InProgressSubmission obj,
            SubmissionStepConfig config
    ) throws Exception {
        Context context = ContextUtil.obtainCurrentRequestContext();
        String doiMetadata = configurationService.getProperty("unpaywall.metadata.doi");
        String metadataFirstValue =
                itemService.getMetadataFirstValue(obj.getItem(), new MetadataFieldName(doiMetadata), Item.ANY);
        if (nonNull(metadataFirstValue)) {
            Unpaywall unpaywall =
                    unpaywallService.getUnpaywallCall(context, metadataFirstValue, obj.getItem().getID());
            return new DataUnpaywall(unpaywall);
        }
        return null;
    }

    @Override
    public void doPatchProcessing(
            Context context,
            HttpServletRequest currentRequest,
            InProgressSubmission source,
            Operation op,
            SubmissionStepConfig stepConf
    ) throws Exception {
    }
}
