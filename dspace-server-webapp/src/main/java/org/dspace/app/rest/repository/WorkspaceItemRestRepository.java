/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureService;
import org.dspace.app.rest.authorization.AuthorizationRestUtil;
import org.dspace.app.rest.authorization.impl.ItemCorrectionFeature;
import org.dspace.app.rest.converter.WorkspaceItemConverter;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.ExtractMetadataStepException;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.handler.service.UriListHandlerService;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.UploadableStep;
import org.dspace.app.rest.utils.Utils;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.EPersonServiceImpl;
import org.dspace.event.Event;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.exception.FileMultipleOccurencesException;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.service.ImportService;
import org.dspace.services.ConfigurationService;
import org.dspace.submit.factory.SubmissionServiceFactory;
import org.dspace.submit.service.SubmissionConfigService;
import org.dspace.util.UUIDUtils;
import org.dspace.validation.service.ValidationService;
import org.dspace.versioning.ItemCorrectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;


/**
 * This is the repository responsible to manage WorkspaceItem Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Pasquale Cavallo (pasquale.cavallo at 4science.it)
 */
@Component(WorkspaceItemRest.CATEGORY + "." + WorkspaceItemRest.PLURAL_NAME)
public class WorkspaceItemRestRepository extends DSpaceRestRepository<WorkspaceItemRest, Integer>
    implements ReloadableEntityObjectRepository<WorkspaceItem, Integer> {

    public static final String OPERATION_PATH_SECTIONS = ValidationService.OPERATION_PATH_SECTIONS;

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(WorkspaceItemRestRepository.class);

    @Autowired
    WorkspaceItemService wis;

    @Autowired
    ItemService itemService;

    @Autowired
    BitstreamService bitstreamService;

    @Autowired
    BitstreamFormatService bitstreamFormatService;

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    ItemCorrectionService itemCorrectionService;

    @Autowired
    WorkspaceItemConverter workspaceItemConverter;

    @Autowired
    SubmissionService submissionService;

    @Autowired
    EPersonServiceImpl epersonService;

    @Autowired
    CollectionService collectionService;

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    ImportService importService;

    @Autowired
    AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    AuthorizationRestUtil authorizationRestUtil;

    @Autowired
    private UriListHandlerService uriListHandlerService;

    private SubmissionConfigService submissionConfigService;

    public WorkspaceItemRestRepository() throws SubmissionConfigReaderException {
        submissionConfigService = SubmissionServiceFactory.getInstance().getSubmissionConfigService();
    }

    @PreAuthorize("hasPermission(#id, 'WORKSPACEITEM', 'READ')")
    @Override
    public WorkspaceItemRest findOne(Context context, Integer id) {
        WorkspaceItem witem = null;
        try {
            witem = wis.find(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (witem == null) {
            return null;
        }
        return converter.toRest(witem, utils.obtainProjection());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public Page<WorkspaceItemRest> findAll(Context context, Pageable pageable) {
        try {
            long total = wis.countTotal(context);
            List<WorkspaceItem> witems = wis.findAll(context, pageable.getPageSize(),
                    Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(witems, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @PreAuthorize("hasPermission(#submitterID, 'EPERSON', 'READ')")
    @SearchRestMethod(name = "findBySubmitter")
    public Page<WorkspaceItemRest> findBySubmitter(@Parameter(value = "uuid", required = true) UUID submitterID,
            Pageable pageable) {
        try {
            Context context = obtainContext();
            EPerson ep = epersonService.find(context, submitterID);
            long total = wis.countByEPerson(context, ep);
            List<WorkspaceItem> witems = wis.findByEPerson(context, ep, pageable.getPageSize(),
                    Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(witems, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    protected WorkspaceItemRest createAndReturn(Context context) throws SQLException, AuthorizeException {

        HttpServletRequest httpServletRequest = getRequestService().getCurrentRequest().getHttpServletRequest();
        String itemUUID = httpServletRequest.getParameter("item");
        String relationship = httpServletRequest.getParameter("relationship");

        WorkspaceItem source;


        if ((StringUtils.isNotBlank(itemUUID) && StringUtils.isNotBlank(relationship))) {

            UUID itemId = UUIDUtils.fromString(itemUUID);

            if (itemId != null && !isAuthorizedToCorrect(context, itemId)) {
                throw new RESTAuthorizationException("The user is not allowed to correct the given item");
            }

            try {
                source = itemCorrectionService.createWorkspaceItemAndRelationshipByItem(context, itemId, relationship);
            } catch (AuthorizeException e) {
                throw new RESTAuthorizationException(e);
            } catch (Exception e) {
                throw new UnprocessableEntityException(e.getMessage());
            }

        } else if (StringUtils.isNotBlank(itemUUID)) {
            try {
                source = itemCorrectionService.createWorkspaceItemByItem(context, UUIDUtils.fromString(itemUUID));
            } catch (Exception e) {
                throw new UnprocessableEntityException(e.getMessage());
            }
        } else if (StringUtils.isNotBlank(relationship)) {
            throw new UnprocessableEntityException("Cannot create a relationship without a given item");
        } else {
            source = submissionService.createWorkspaceItem(context, getRequestService().getCurrentRequest());
        }

        return converter.toRest(source, utils.obtainProjection());
    }

    @Override
    public Class<WorkspaceItemRest> getDomainClass() {
        return WorkspaceItemRest.class;
    }

    @PreAuthorize("hasPermission(#id, 'WORKSPACEITEM', 'WRITE')")
    @Override
    public WorkspaceItemRest upload(HttpServletRequest request, String apiCategory, String model, Integer id,
                                    MultipartFile file) throws SQLException {

        Context context = obtainContext();
        WorkspaceItemRest wsi = findOne(context, id);
        WorkspaceItem source = wis.find(context, id);

        List<ErrorRest> errors = submissionService.uploadFileToInprogressSubmission(context, request, wsi, source,
                file);
        wsi = converter.toRest(source, utils.obtainProjection());

        if (!errors.isEmpty()) {
            wsi.getErrors().addAll(errors);
        }

        context.commit();
        return wsi;
    }

    @PreAuthorize("hasPermission(#id, 'WORKSPACEITEM', 'WRITE')")
    @Override
    public void patch(Context context, HttpServletRequest request, String apiCategory, String model, Integer id,
                      Patch patch) throws SQLException, AuthorizeException {
        List<Operation> operations = patch.getOperations();
        WorkspaceItemRest wsi = findOne(context, id);
        if (wsi == null) {
            throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + id + " not found");
        }
        WorkspaceItem source = wis.find(context, id);
        for (Operation op : operations) {
            //the value in the position 0 is a null value
            String[] path = op.getPath().substring(1).split("/", 3);
            if (OPERATION_PATH_SECTIONS.equals(path[0])) {
                String section = path[1];
                try {
                    submissionService.evaluatePatchToInprogressSubmission(context, request, source, wsi, section, op);
                } catch (ExtractMetadataStepException e) {
                    log.warn(e.getMessage(), e);
                }
            } else {
                throw new DSpaceBadRequestException(
                    "Patch path operation need to starts with '" + OPERATION_PATH_SECTIONS + "'");
            }
        }
        wis.update(context, source);
    }

    @PreAuthorize("hasPermission(#id, 'WORKSPACEITEM', 'DELETE')")
    @Override
    protected void delete(Context context, Integer id) throws AuthorizeException {
        WorkspaceItem witem = null;
        try {
            witem = wis.find(context, id);
            if (witem == null) {
                throw new ResourceNotFoundException(
                        WorkspaceItemRest.CATEGORY + "." + WorkspaceItemRest.NAME +
                            " with id: " + id + " not found");
            }
            wis.deleteAll(context, witem);
            context.addEvent(new Event(Event.DELETE, Constants.ITEM, witem.getItem().getID(), null,
                itemService.getIdentifiers(context, witem.getItem())));
        } catch (SQLException | IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public Iterable<WorkspaceItemRest> upload(Context context, HttpServletRequest request,
            List<MultipartFile> uploadfiles)
        throws SQLException, FileNotFoundException, IOException, AuthorizeException {
        List<WorkspaceItemRest> results = new ArrayList<>();

        String uuid = request.getParameter("owningCollection");
        if (StringUtils.isBlank(uuid)) {
            uuid = configurationService.getProperty("submission.default.collection");
        }
        Collection collection = null;
        if (StringUtils.isNotBlank(uuid)) {
            collection = collectionService.find(context, UUID.fromString(uuid));
        } else {
            collection = collectionService.findAuthorizedOptimized(context, Constants.ADD).get(0);
        }

        SubmissionConfig submissionConfig =
            submissionConfigService.getSubmissionConfigByCollection(collection);
        List<WorkspaceItem> result = null;
        List<ImportRecord> records = new ArrayList<>();
        try {
            for (MultipartFile mpFile : uploadfiles) {
                File file = Utils.getFile(mpFile, "upload-loader", "filedataloader");
                try {
                    List<ImportRecord> recordsFound = importService.getRecords(file, mpFile.getOriginalFilename());
                    if (recordsFound != null && !recordsFound.isEmpty()) {
                        records.addAll(recordsFound);
                        break;
                    }
                } catch (Exception e) {
                    log.error("Error processing data", e);
                    throw e;
                } finally {
                    file.delete();
                }
            }
        } catch (FileMultipleOccurencesException e) {
            throw new UnprocessableEntityException("Too many entries in file");
        } catch (Exception e) {
            log.error("Error importing metadata", e);
        }
        result = new ArrayList<>(records.size());
        for (ImportRecord importRecord : records) {
            WorkspaceItem source = submissionService.
                createWorkspaceItem(context, getRequestService().getCurrentRequest());

            merge(context, importRecord, source);

            result.add(source);
        }

        //perform upload of bitstream if there is exact one result and convert workspaceitem to entity rest
        if (!result.isEmpty()) {
            for (WorkspaceItem wi : result) {
                List<ErrorRest> errors = new ArrayList<ErrorRest>();
                wi.setMultipleFiles(uploadfiles.size() > 1);
                //load bitstream into bundle ORIGINAL only if there is one result (approximately this is the
                // right behaviour for pdf file but not for other bibliographic format e.g. bibtex)
                if (result.size() == 1) {
                    ClassLoader loader = this.getClass().getClassLoader();
                    for (int i = 0; i < submissionConfig.getNumberOfSteps(); i++) {
                        SubmissionStepConfig stepConfig = submissionConfig.getStep(i);
                        try {
                            Class<?> stepClass = loader.loadClass(stepConfig.getProcessingClassName());
                            Object stepInstance = stepClass.getConstructor().newInstance();
                            if (UploadableStep.class.isAssignableFrom(stepClass)) {
                                UploadableStep uploadableStep = (UploadableStep) stepInstance;
                                for (MultipartFile mpFile : uploadfiles) {
                                    ErrorRest err =
                                        uploadableStep.upload(context, submissionService, stepConfig, wi, mpFile);
                                    if (err != null) {
                                        errors.add(err);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                }
                WorkspaceItemRest wsi = converter.toRest(wi, utils.obtainProjection());
                if (result.size() == 1) {
                    if (!errors.isEmpty()) {
                        wsi.getErrors().addAll(errors);
                    }
                }
                results.add(wsi);
            }
        }
        return results;
    }

    @Override
    protected WorkspaceItemRest createAndReturn(Context context, List<String> stringList)
        throws AuthorizeException, SQLException, RepositoryMethodNotImplementedException {

        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        WorkspaceItem workspaceItem = uriListHandlerService.handle(context, req, stringList, WorkspaceItem.class);
        return converter.toRest(workspaceItem, utils.obtainProjection());
    }

    /**
     * This is a search method that will return the WorkspaceItemRest object found through the UUID of an item. It'll
     * find the Item through the given UUID and try to resolve the WorkspaceItem relevant for that item and return it.
     * It'll return a 401/403 if the current user isn't allowed to view the WorkspaceItem.
     * It'll return a 204 if nothing was found
     * @param itemUuid  The UUID for the Item to be used
     * @param pageable  The pageable if present
     * @return          The resulting WorkspaceItem object
     */
    @SearchRestMethod(name = "item")
    public WorkspaceItemRest findByItemUuid(@Parameter(value = "uuid", required = true) UUID itemUuid,
                                            Pageable pageable) {
        try {
            Context context = obtainContext();
            Item item = itemService.find(context, itemUuid);
            WorkspaceItem workspaceItem = wis.findByItem(context, item);
            if (workspaceItem == null) {
                return null;
            }
            if (!authorizeService.authorizeActionBoolean(context, workspaceItem.getItem(), Constants.READ)) {
                throw new AccessDeniedException("The current user does not have rights to view the WorkflowItem");
            }
            return converter.toRest(workspaceItem, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public WorkspaceItem findDomainObjectByPk(Context context, Integer id) throws SQLException {
        return wis.find(context, id);
    }

    @Override
    public Class<Integer> getPKClass() {
        return Integer.class;
    }

    private boolean isAuthorizedToCorrect(Context context, UUID itemId) throws SQLException {

        AuthorizationFeature itemCorrectionFeature = authorizationFeatureService.find(ItemCorrectionFeature.NAME);
        if (itemCorrectionFeature == null) {
            throw new IllegalStateException(
                "No AuthorizationFeature configured with name " + ItemCorrectionFeature.NAME);
        }

        try {
            return itemCorrectionFeature.isAuthorized(context, findItemRestById(context, itemId.toString()));
        } catch (SearchServiceException e) {
            throw new RuntimeException(e);
        }
    }

    private BaseObjectRest<?> findItemRestById(Context context, String itemId) throws SQLException {
        String objectId = ItemCorrectionFeature.NAME + "_" + ItemRest.CATEGORY + "." + ItemRest.NAME + "_" + itemId;
        return authorizationRestUtil.getObject(context, objectId);
    }

    private void merge(Context context, ImportRecord record, WorkspaceItem item) throws SQLException {
        for (MetadataValue metadataValue : itemService.getMetadata(
            item.getItem(), Item.ANY, Item.ANY, Item.ANY, Item.ANY)) {
            itemService.clearMetadata(context, item.getItem(),
                metadataValue.getMetadataField().getMetadataSchema().getNamespace(),
                metadataValue.getMetadataField().getElement(),
                metadataValue.getMetadataField().getQualifier(),
                metadataValue.getLanguage());
        }
        if (record != null && record.getValueList() != null) {
            for (MetadatumDTO metadataValue : record.getValueList()) {
                itemService.addMetadata(context, item.getItem(), metadataValue.getSchema(),
                    metadataValue.getElement(), metadataValue.getQualifier(), null,
                    metadataValue.getValue());
            }
        }
    }

    public void setSubmissionService(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }
}
