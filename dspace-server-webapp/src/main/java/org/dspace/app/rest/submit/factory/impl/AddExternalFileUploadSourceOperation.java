/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import static org.dspace.validation.ExternalFileUploadValidator.ERROR_KEY;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.bulkimport.util.ImportFileUtil;
import org.dspace.app.rest.model.step.ExternalFileUpload;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class AddExternalFileUploadSourceOperation extends AddPatchOperation<ExternalFileUpload> {

    protected static final String LOCAL_PREFIX = "file://";
    protected static final UrlValidator URL_VALIDATOR = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);

    private static final Logger log = LogManager.getLogger(AddExternalFileUploadSourceOperation.class);
    protected final ImportFileUtil fileUtil = new ImportFileUtil();

    @Override
    void add(Context context, HttpServletRequest currentRequest, InProgressSubmission source, String path,
             Object value) throws Exception {
        String filePath = (String) value;

        if (!URL_VALIDATOR.isValid(filePath) && !filePath.startsWith(LOCAL_PREFIX)) {
            filePath = LOCAL_PREFIX + filePath;
        }

        Optional<InputStream> inputStream$ = fileUtil.getInputStream(filePath);

        if (inputStream$.isEmpty()) {
            currentRequest.setAttribute(ERROR_KEY, "error.validation.filenotfound");
            return;
        }

        Bitstream bitstreamSource = null;
        BitstreamFormat bf = null;

        Item item = source.getItem();
        List<Bundle> bundles = null;
        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();
        BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
        try {
            // do we already have a bundle?
            bundles = itemService.getBundles(item, Constants.CONTENT_BUNDLE_NAME);

            InputStream inputStream = new BufferedInputStream(inputStream$.get());
            if (bundles.isEmpty()) {
                // set bundle's name to ORIGINAL
                bitstreamSource =
                    itemService.createSingleBitstream(context, inputStream, item, Constants.CONTENT_BUNDLE_NAME);
            } else {
                // we have a bundle already, just add bitstream
                bitstreamSource = bitstreamService.create(context, bundles.get(0), inputStream);
            }

            bitstreamSource.setName(context, extractFullName(filePath));
            bitstreamSource.setSource(context, filePath);

            // Identify the format
            bf = bitstreamFormatService.guessFormat(context, bitstreamSource);
            bitstreamSource.setFormat(context, bf);

            // Update to DB
            bitstreamService.update(context, bitstreamSource);
            itemService.update(context, item);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            currentRequest.setAttribute(ERROR_KEY, "error.validation.filenotprocessed");
        }
    }

    protected String extractFullName(String filePath) {
        int lastSlash = filePath.lastIndexOf("/");
        String fullname = filePath;
        if (lastSlash > 0) {
            fullname = filePath.substring(lastSlash + 1);
        }
        return fullname;
    }

    @Override
    protected Class<ExternalFileUpload[]> getArrayClassForEvaluation() {
        return ExternalFileUpload[].class;
    }

    @Override
    protected Class<ExternalFileUpload> getClassForEvaluation() {
        return ExternalFileUpload.class;
    }
}
