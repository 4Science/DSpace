/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import static java.lang.Boolean.TRUE;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.DataUnpaywall;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.ListenerProcessingStep;
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
import org.dspace.unpaywall.service.UnpaywallService;
import org.dspace.web.ContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unpaywall submission step.
 */
public class UnpaywallStep extends AbstractProcessingStep implements ListenerProcessingStep {

    private final Logger logger = LoggerFactory.getLogger(UnpaywallStep.class);

    private final static String REFRESH_OPERATION = "refresh";
    private final static String ACCEPT_OPERATION = "accept";

    private final UnpaywallService unpaywallService = ContentServiceFactory.getInstance().getUnpaywallService();

    private final ConfigurationService configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    @Override
    public void doPreProcessing(Context context, InProgressSubmission wsi) {
        // nothing to do
    }

    @Override
    public void doPostProcessing(Context context, InProgressSubmission wsi) {
        final Item item = wsi.getItem();
        final Optional<String> doiValue = getDoiValue(item);
        if (doiValue.isPresent()) {
            unpaywallService.initUnpaywallCallIfNeeded(context, doiValue.get(), item.getID());
        }
    }

    @Override
    public DataUnpaywall getData(
            SubmissionService submissionService,
            InProgressSubmission obj,
            SubmissionStepConfig config
    ) throws Exception {
        Context context = ContextUtil.obtainCurrentRequestContext();
        return getDoiValue(obj.getItem())
                .map(doi -> unpaywallService.findUnpaywall(context, doi, obj.getItem().getID()))
                .filter(Optional::isPresent)
                .map(unpaywall -> new DataUnpaywall(unpaywall.get()))
                .orElse(null);
    }

    @Override
    public void doPatchProcessing(
            Context context,
            HttpServletRequest currentRequest,
            InProgressSubmission source,
            Operation operation,
            SubmissionStepConfig stepConf
    ) throws Exception {
        if (operation.getPath().contains(ACCEPT_OPERATION) && TRUE.equals(operation.getValue())) {
            getDoiValue(source.getItem())
                .flatMap(doi -> this.unpaywallService.findUnpaywall(context, doi, source.getItem().getID()))
                .ifPresentOrElse(
                    unpaywall -> this.unpaywallService.downloadResource(context, unpaywall, source.getItem()),
                    () -> new UnprocessableEntityException(
                        "Cannot find any unpaywall related to item: " + source.getItem().getID()
                    )
                );
        } else if (operation.getPath().contains(REFRESH_OPERATION)) {
            getDoiValue(source.getItem())
                .ifPresent(doi -> {
                    if (isRefreshRequired(operation)) {
                        unpaywallService.initUnpaywallCall(context, doi, source.getItem().getID());
                    } else {
                        unpaywallService.initUnpaywallCallIfNeeded(context, doi, source.getItem().getID());
                    }
                });
        }
    }

    private static boolean isRefreshRequired(Operation operation) {
        return operation.getPath().endsWith(REFRESH_OPERATION) && TRUE.equals(operation.getValue());
    }

    private Optional<String> getDoiValue(Item item) {
        String doiMetadata = configurationService.getProperty("unpaywall.metadata.doi");
        String doiValue = itemService.getMetadataFirstValue(item, new MetadataFieldName(doiMetadata), Item.ANY);
        return Optional.ofNullable(doiValue);
    }
}
