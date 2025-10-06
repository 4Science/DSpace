/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkaccesscontrol;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.dspace.authorize.ResourcePolicy.TYPE_CUSTOM;
import static org.dspace.authorize.ResourcePolicy.TYPE_INHERITED;
import static org.dspace.core.Constants.CONTENT_BUNDLE_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.bulkaccesscontrol.exception.BulkAccessControlException;
import org.dspace.app.bulkaccesscontrol.model.AccessCondition;
import org.dspace.app.bulkaccesscontrol.model.AccessConditionBitstream;
import org.dspace.app.bulkaccesscontrol.model.AccessConditionItem;
import org.dspace.app.bulkaccesscontrol.model.BulkAccessConditionConfiguration;
import org.dspace.app.bulkaccesscontrol.model.BulkAccessControlInput;
import org.dspace.app.bulkaccesscontrol.service.BulkAccessConditionConfigurationService;
import org.dspace.app.mediafilter.factory.MediaFilterServiceFactory;
import org.dspace.app.mediafilter.service.MediaFilterService;
import org.dspace.app.util.DSpaceObjectUtilsImpl;
import org.dspace.app.util.service.DSpaceObjectUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.submit.model.AccessConditionOption;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link DSpaceRunnable} to perform a bulk access control via json file.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 *
 */
public class BulkAccessControl extends DSpaceRunnable<BulkAccessControlScriptConfiguration<BulkAccessControl>> {

    private static final Log log = LogFactory.getLog(BulkAccessControl.class);

    private DSpaceObjectUtils dSpaceObjectUtils;

    private SearchService searchService;

    private ItemService itemService;

    private String filename;

    private List<String> uuids;

    private Context context;

    private BulkAccessConditionConfigurationService bulkAccessConditionConfigurationService;

    private ResourcePolicyService resourcePolicyService;

    protected EPersonService epersonService;

    private ConfigurationService configurationService;

    private MediaFilterService mediaFilterService;

    private GroupService groupService;

    private Map<String, AccessConditionOption> itemAccessConditions;

    private Map<String, AccessConditionOption> uploadAccessConditions;

    private final String ADD_MODE = "add";

    private final String REPLACE_MODE = "replace";

    private boolean help = false;

    protected String eperson = null;

    @Override
    @SuppressWarnings("unchecked")
    public void setup() throws ParseException {

        this.searchService = SearchUtils.getSearchService();
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
        this.epersonService = EPersonServiceFactory.getInstance().getEPersonService();
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        mediaFilterService = MediaFilterServiceFactory.getInstance().getMediaFilterService();
        mediaFilterService.setLogHandler(handler);
        this.bulkAccessConditionConfigurationService = new DSpace().getServiceManager().getServiceByName(
            "bulkAccessConditionConfigurationService", BulkAccessConditionConfigurationService.class);
        this.dSpaceObjectUtils = new DSpace().getServiceManager().getServiceByName(
            DSpaceObjectUtilsImpl.class.getName(), DSpaceObjectUtilsImpl.class);
        this.groupService = EPersonServiceFactory.getInstance().getGroupService();

        BulkAccessConditionConfiguration bulkAccessConditionConfiguration =
            bulkAccessConditionConfigurationService.getBulkAccessConditionConfiguration("default");

        itemAccessConditions = bulkAccessConditionConfiguration
            .getItemAccessConditionOptions()
            .stream()
            .collect(Collectors.toMap(AccessConditionOption::getName, Function.identity()));

        uploadAccessConditions = bulkAccessConditionConfiguration
            .getBitstreamAccessConditionOptions()
            .stream()
            .collect(Collectors.toMap(AccessConditionOption::getName, Function.identity()));

        help = commandLine.hasOption('h');
        filename = commandLine.getOptionValue('f');
        uuids = commandLine.hasOption('u') ? Arrays.asList(commandLine.getOptionValues('u')) : null;
    }

    @Override
    public void internalRun() throws Exception {

        if (help) {
            printHelp();
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.setTimeZone(TimeZone.getTimeZone("UTC"));
        BulkAccessControlInput accessControl;
        context = new Context(Context.Mode.BATCH_EDIT);
        setEPerson(context);

        if (!isAuthorized(context)) {
            handler.logError("Current user is not eligible to execute script bulk-access-control");
            throw new AuthorizeException("Current user is not eligible to execute script bulk-access-control");
        }

        if (uuids == null || uuids.size() == 0) {
            handler.logError("A target uuid must be provided with at least on uuid (run with -h flag for details)");
            throw new IllegalArgumentException("At least one target uuid must be provided");
        }

        InputStream inputStream = handler.getFileStream(context, filename)
            .orElseThrow(() -> new IllegalArgumentException("Error reading file, the file couldn't be "
                + "found for filename: " + filename));

        try {
            accessControl = mapper.readValue(inputStream, BulkAccessControlInput.class);
        } catch (IOException e) {
            handler.logError("Error parsing json file " + e.getMessage());
            throw new IllegalArgumentException("Error parsing json file", e);
        }
        try {
            validate(accessControl);
            updateItemsAndBitstreamsPolices(accessControl);
            context.complete();
        } catch (Exception e) {
            handler.handleException(e);
            context.abort();
        }
    }

    /**
     * check the validation of mapped json data, it must
     * provide item or bitstream information or both of them
     * and check the validation of item node if provided,
     * and check the validation of bitstream node if provided.
     *
     * @param accessControl mapped json data
     * @throws SQLException if something goes wrong in the database
     * @throws BulkAccessControlException if accessControl is invalid
     */
    private void validate(BulkAccessControlInput accessControl) throws SQLException {

        AccessConditionItem item = accessControl.getItem();
        AccessConditionBitstream bitstream = accessControl.getBitstream();

        boolean itemPresent = Objects.nonNull(item);
        boolean bitstreamPresent = Objects.nonNull(bitstream);

        if (!itemPresent && !bitstreamPresent) {
            String error = "Item or Bitstream node must be provided";
            handler.logError(error);
            throw new BulkAccessControlException(error);
        }

        if (itemPresent) {
            validateItemNode(item);
        }

        if (bitstreamPresent) {
            validateBitstreamNode(bitstream);
        }

    }

    /**
     * check the validation of item node, the item mode
     * must be provided with value 'add' or 'replace'
     * if mode equals to add so the information
     * of accessCondition must be provided,
     * also checking that accessConditions information are valid.
     *
     * @param item the item node
     * @throws BulkAccessControlException if item node is invalid
     */
    private void validateItemNode(AccessConditionItem item) {
        String mode = item.getMode();
        List<AccessCondition> accessConditions = item.getAccessConditions();

        if (StringUtils.isEmpty(mode)) {
            handler.logError("item mode node must be provided");
            throw new BulkAccessControlException("item mode node must be provided");
        } else if (!(StringUtils.equalsAny(mode, ADD_MODE, REPLACE_MODE))) {
            handler.logError("wrong value for item mode<" + mode + ">");
            throw new BulkAccessControlException("wrong value for item mode<" + mode + ">");
        } else if (ADD_MODE.equals(mode) && isEmpty(accessConditions)) {
            handler.logError("accessConditions of item must be provided with mode<" + ADD_MODE + ">");
            throw new BulkAccessControlException(
                "accessConditions of item must be provided with mode<" + ADD_MODE + ">");
        }

        for (AccessCondition accessCondition : accessConditions) {
            validateItemAccessCondition(accessCondition);
        }
    }

    /**
     * check the validation of bitstream node, the bitstream mode
     * must be provided with value 'add' or 'replace'
     * if mode equals to add so the information of accessConditions
     * must be provided,
     * also checking that constraint information is valid,
     * also checking that accessConditions information are valid.
     *
     * @param bitstream the bitstream node
     * @throws SQLException if something goes wrong in the database
     * @throws BulkAccessControlException if bitstream node is invalid
     */
    private void validateBitstreamNode(AccessConditionBitstream bitstream) throws SQLException {
        String mode = bitstream.getMode();
        List<AccessCondition> accessConditions = bitstream.getAccessConditions();

        if (StringUtils.isEmpty(mode)) {
            handler.logError("bitstream mode node must be provided");
            throw new BulkAccessControlException("bitstream mode node must be provided");
        } else if (!(StringUtils.equalsAny(mode, ADD_MODE, REPLACE_MODE))) {
            handler.logError("wrong value for bitstream mode<" + mode + ">");
            throw new BulkAccessControlException("wrong value for bitstream mode<" + mode + ">");
        } else if (ADD_MODE.equals(mode) && isEmpty(accessConditions)) {
            handler.logError("accessConditions of bitstream must be provided with mode<" + ADD_MODE + ">");
            throw new BulkAccessControlException(
                "accessConditions of bitstream must be provided with mode<" + ADD_MODE + ">");
        }

        validateConstraint(bitstream);

        for (AccessCondition accessCondition : bitstream.getAccessConditions()) {
            validateBitstreamAccessCondition(accessCondition);
        }
    }

    /**
     * check the validation of constraint node if provided,
     * constraint isn't supported when multiple uuids are provided
     * or when uuid isn't an Item
     *
     * @param bitstream the bitstream node
     * @throws SQLException if something goes wrong in the database
     * @throws BulkAccessControlException if constraint node is invalid
     */
    private void validateConstraint(AccessConditionBitstream bitstream) throws SQLException {
        if (uuids.size() > 1  && containsConstraints(bitstream)) {
            handler.logError("constraint isn't supported when multiple uuids are provided");
            throw new BulkAccessControlException("constraint isn't supported when multiple uuids are provided");
        } else if (uuids.size() == 1 && containsConstraints(bitstream)) {
            DSpaceObject dso =
                dSpaceObjectUtils.findDSpaceObject(context, UUID.fromString(uuids.get(0)));

            if (dso == null) {
                String msg = "Unable to find Dspace Object";
                handler.logError(msg);
                throw new BulkAccessControlException(msg);
            }

            if (!isValidType(dso)) {
                String msg = "constraint is not supported when uuid isn't an Dspace Object";
                handler.logError(msg);
                throw new BulkAccessControlException(msg);
            }
        }

        if (bitstream.getConstraints() != null && bitstream.getConstraints().getUuid() != null) {
            for (String uuid : bitstream.getConstraints().getUuid()) {
                DSpaceObject b =
                    dSpaceObjectUtils.findDSpaceObject(context, UUID.fromString(uuid));

                if (b == null) {
                    String msg = "Unable to find bistream " + uuid;
                    handler.logError(msg);
                    throw new BulkAccessControlException(msg);
                }
            }
        }
    }


    /**
     * check the validation of access condition,
     * the access condition name must equal to one of configured access conditions,
     * then call {@link AccessConditionOption#validateResourcePolicy(
     * Context, String, Date, Date)} if exception happens so, it's invalid.
     *
     * @param accessCondition the accessCondition
     * @throws BulkAccessControlException if the accessCondition is invalid
     */
    private void validateItemAccessCondition(AccessCondition accessCondition) {
        String conditionName = accessCondition.getName();

        boolean isItemAccessCondition = itemAccessConditions.containsKey(conditionName);

        if (!isItemAccessCondition) {
            String errorMessage = "Invalid Item access condition: <" + conditionName + ">";
            handler.logError(errorMessage);
            throw new BulkAccessControlException(errorMessage);
        }

        try {
            AccessConditionOption option = itemAccessConditions.get(conditionName);
            option.validateResourcePolicy(
                context,
                conditionName,
                accessCondition.getStartDate(),
                accessCondition.getEndDate()
            );
        } catch (Exception e) {
            handler.handleException(e);
        }
    }


    private void validateBitstreamAccessCondition(AccessCondition accessCondition) {
        String conditionName = accessCondition.getName();

        boolean isBitstreamAccessCondition = uploadAccessConditions.containsKey(conditionName);

        if (!isBitstreamAccessCondition) {
            String errorMessage = "Invalid Bitstream access condition <" + conditionName + ">";
            handler.logError(errorMessage);
            throw new BulkAccessControlException(errorMessage);
        }

        try {
            AccessConditionOption option = uploadAccessConditions.get(conditionName);
            option.validateResourcePolicy(
                context,
                conditionName,
                accessCondition.getStartDate(),
                accessCondition.getEndDate()
            );

        } catch (Exception e) {
            handler.handleException(e);
        }
    }

    /**
     * find all items of provided {@link #uuids} from solr,
     * then update the resource policies of items
     * or bitstreams of items (only bitstreams of ORIGINAL bundles)
     * and derivative bitstreams, or both of them.
     *
     * @param accessControl the access control input
     * @throws SQLException if something goes wrong in the database
     * @throws SearchServiceException if a search error occurs
     * @throws AuthorizeException if an authorization error occurs
     */
    private void updateItemsAndBitstreamsPolices(BulkAccessControlInput accessControl)
        throws SQLException, SearchServiceException, AuthorizeException {

        int counter = 0;
        int start = 0;
        int limit = 20;

        String query = buildSolrQuery(uuids);

        Iterator<Item> itemIterator = findItems(query, start, limit);

        while (itemIterator.hasNext()) {

            Item item = context.reloadEntity(itemIterator.next());

            if (Objects.nonNull(accessControl.getItem())) {
                updateItemPolicies(item, accessControl);
            }

            if (Objects.nonNull(accessControl.getBitstream())) {
                updateBitstreamsPolicies(item, accessControl.getBitstream());
            }

            context.commit();
            context.uncacheEntity(item);
            counter++;

            if (counter == limit) {
                counter = 0;
                start += limit;
                itemIterator = findItems(query, start, limit);
            }
        }
    }

    private String buildSolrQuery(List<String> uuids) throws SQLException {
        String [] query = new String[uuids.size()];

        for (int i = 0 ; i < query.length ; i++) {
            DSpaceObject dso = dSpaceObjectUtils.findDSpaceObject(context, UUID.fromString(uuids.get(i)));

            if (dso.getType() == Constants.COMMUNITY) {
                query[i] = "location.comm:" + dso.getID();
            } else if (dso.getType() == Constants.COLLECTION) {
                query[i] = "location.coll:" + dso.getID();
            } else if (dso.getType() == Constants.ITEM) {
                query[i] = "search.resourceid:" + dso.getID();
            }
        }
        return StringUtils.joinWith(" OR ", query);
    }

    private boolean isValidType(DSpaceObject dso) {
        return dso != null &&
            (
                Constants.COMMUNITY == dso.getType() ||
                Constants.COLLECTION == dso.getType() ||
                Constants.ITEM == dso.getType()
            );
    }

    private Iterator<Item> findItems(String query, int start, int limit)
        throws SearchServiceException {

        DiscoverQuery discoverQuery = buildDiscoveryQuery(query, start, limit);

        return searchService.search(context, discoverQuery)
                            .getIndexableObjects()
                            .stream()
                            .map(indexableObject ->
                                ((IndexableItem) indexableObject).getIndexedObject())
                            .collect(Collectors.toList())
                            .iterator();
    }

    private DiscoverQuery buildDiscoveryQuery(String query, int start, int limit) {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.setQuery(query);
        discoverQuery.setStart(start);
        discoverQuery.setMaxResults(limit);
        discoverQuery.setSortField("search.resourceid", DiscoverQuery.SORT_ORDER.asc);
        return discoverQuery;
    }

    /**
     * update the item resource policies,
     * when mode equals to 'replace' will remove
     * all current resource polices of types 'TYPE_CUSTOM'
     * and 'TYPE_INHERITED' then, set the new resource policies.
     *
     * @param item the item
     * @param accessControl the access control input
     * @throws SQLException if something goes wrong in the database
     * @throws AuthorizeException if an authorization error occurs
     */
    private void updateItemPolicies(Item item, BulkAccessControlInput accessControl)
        throws SQLException, AuthorizeException {

        AccessConditionItem acItem = accessControl.getItem();

        if (REPLACE_MODE.equals(acItem.getMode())) {
            removeReadPolicies(item, TYPE_CUSTOM);
            removeReadPolicies(item, TYPE_INHERITED);
        }

        setItemPolicies(item, accessControl);
        logInfo(acItem.getAccessConditions(), acItem.getMode(), item);
    }

    /**
     * create the new resource policies of item.
     * then, call {@link ItemService#adjustItemPolicies(
     * Context, Item, Collection)} to adjust item's default policies.
     *
     * @param item the item
     * @param accessControl the access control input
     * @throws SQLException if something goes wrong in the database
     * @throws AuthorizeException if an authorization error occurs
     */
    private void setItemPolicies(Item item, BulkAccessControlInput accessControl)
        throws SQLException, AuthorizeException {

        accessControl
            .getItem()
            .getAccessConditions()
            .forEach(accessCondition -> createResourcePolicy(item, accessCondition,
                itemAccessConditions.get(accessCondition.getName())));

        itemService.adjustItemPolicies(context, item, item.getOwningCollection(), false);
    }

    /**
     * Applies access conditions to bitstreams filtered by bundle and bitstream constraints.
     *
     * @param item the item containing the bundles
     * @param bundleNames list of bundle names to filter (e.g., "ORIGINAL") or null to allow all
     * @param bundleConstraints list of bundle UUIDs to filter or null
     * @param bitstreamConstraints list of bitstream UUIDs to filter or null
     * @param mode the access control mode
     * @param accessConditions the access conditions to apply
     */
    private void applyAccessConditionsToBitstreams(Item item,
                                                   List<String> bundleNames,
                                                   List<String> bundleConstraints,
                                                   List<String> bitstreamConstraints,
                                                   String mode,
                                                   List<AccessCondition> accessConditions) {

        // look over all the bundles and force initialization of bitstreams collection
        // to avoid lazy initialization exception
        long count = item.getBundles()
                         .stream()
                         .mapToLong(bundle ->
                                      bundle.getBitstreams().size())
                         .sum();

        Stream<Bundle> bundleStream = item.getBundles().stream();

        if (bundleNames != null) {
            bundleStream = bundleStream.filter(b -> bundleNames.contains(b.getName()));
        }

        if (bundleConstraints != null && !bundleConstraints.isEmpty()) {
            bundleStream = bundleStream.filter(b -> bundleConstraints.contains(b.getID().toString()));
        }


        bundleStream.map(bundle ->
                             Map.entry(
                                 bundle,
                                 bundle.getBitstreams().stream()
                                       .filter(bitstream ->
                                                   bitstreamConstraints == null ||
                                                   bitstreamConstraints.isEmpty() ||
                                                   bitstreamConstraints.contains(bitstream.getID().toString())
                                       )
                                     .collect(Collectors.toList())
                             )
                    )
                    .forEach(entry -> {
                        // if we are going to apply the constraint to all bitstreams
                        updateBundlePolicy(context, entry.getKey(), mode, bitstreamConstraints, accessConditions);
                        entry.getValue().forEach(bitstream -> {
                            try {
                                updateBitstreamPolicies(bitstream, item, mode, accessConditions);
                            } catch (RuntimeException e) {
                                handler.logError("Cannot update policies on bitstream " + bitstream.getID(), e);
                            }
                        });
                    });
    }

    private void updateBundlePolicy(Context context, Bundle bundle, String mode, List<String> bitstreamConstraints,
                                    List<AccessCondition> accessConditions) {
        if (
            REPLACE_MODE.equals(mode) &&
                (bitstreamConstraints == null || bitstreamConstraints.isEmpty())
        ) {
            // we need to replace the policy of the related bundle!
            removeReadPolicies(bundle, TYPE_CUSTOM);
            removeReadPolicies(bundle, TYPE_INHERITED);
        }
        accessConditions.stream()
                        .map(ac -> Map.entry(ac, uploadAccessConditions.get(ac.getName())))
                        .filter(entry -> withoutAccessCondition(context, bundle, entry.getKey(), entry.getValue()))
                        .forEach(
                            entry ->
                                    createResourcePolicy(
                                        bundle,
                                        entry.getKey(),
                                        entry.getValue()
                                    )
            );
    }

    private boolean withoutAccessCondition(
        Context context, Bundle bundle, AccessCondition accessCondition, AccessConditionOption option
    ) {
        try {
            List<ResourcePolicy> resourcePolicies =
                resourcePolicyService.find(
                    context,
                    bundle,
                    groupService.findByName(context, option.getGroupName()),
                    Constants.READ
                );
            return resourcePolicies.stream().noneMatch(
                rp ->
                        Objects.equals(accessCondition.getStartDate(), rp.getStartDate()) &&
                        Objects.equals(accessCondition.getEndDate(), rp.getEndDate())

            );
        } catch (SQLException e) {
            handler.logError("Cannot retrieve resource policy for bundle " + bundle.getID(), e);
        }
        return false;
    }

    private void applyAccessConditionsToBitstreams(Item item,
                                                   AccessConditionBitstream accessConditionBitstream,
                                                   String mode,
                                                   List<AccessCondition> accessConditions) {
        List<String> bitstreamConstraints = accessConditionBitstream.getConstraints() != null
            ? accessConditionBitstream.getConstraints().getUuid()
            : null;
        applyAccessConditionsToBitstreams(item,
                                          List.of(CONTENT_BUNDLE_NAME),
                                          null,
                                          bitstreamConstraints,
                                          mode,
                                          accessConditions);
    }

    /**
     * update the resource policies of all item's bitstreams
     * or bitstreams specified into constraint node,
     * and derivative bitstreams.
     *
     * <strong>NOTE:</strong> only bitstreams of ORIGINAL bundles
     *
     * @param item the item contains bitstreams
     * @param conditionBitstream the bitstream condition in input
     */
    private void updateBitstreamsPolicies(Item item, AccessConditionBitstream conditionBitstream) {
        applyAccessConditionsToBitstreams(item,
                                          conditionBitstream,
                                          conditionBitstream.getMode(),
                                          conditionBitstream.getAccessConditions());
    }


    /**
     * check that the bitstream node is existed,
     * and contains constraint node,
     * and constraint contains uuids.
     *
     * @param bitstream the bitstream node
     * @return true when uuids of constraint of bitstream is not empty,
     * otherwise false
     */
    private boolean containsConstraints(AccessConditionBitstream bitstream) {
        return Objects.nonNull(bitstream) &&
            Objects.nonNull(bitstream.getConstraints()) &&
            isNotEmpty(bitstream.getConstraints().getUuid());
    }

    /**
     * update the bitstream resource policies,
     * when mode equals to replace will remove
     * all current resource polices of types 'TYPE_CUSTOM'
     * and 'TYPE_INHERITED' then, set the new resource policies.
     *
     * @param bitstream the bitstream
     * @param item the item of bitstream
     * @param mode the bulk access control mode
     * @param accessConditions the list of access condition in input
     * @throws RuntimeException if something goes wrong in the database
     * or an authorization error occurs
     */
    private void updateBitstreamPolicies(Bitstream bitstream, Item item, String mode,
                                         List<AccessCondition> accessConditions) {

        if (REPLACE_MODE.equals(mode)) {
            removeReadPolicies(bitstream, TYPE_CUSTOM);
            removeReadPolicies(bitstream, TYPE_INHERITED);
        }

        try {
            setBitstreamPolicies(bitstream, item, accessConditions);
            logInfo(accessConditions, mode, bitstream);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * remove dspace object's read policies.
     *
     * @param dso the dspace object
     * @param type resource policy type
     * @throws BulkAccessControlException if something goes wrong
     * in the database or an authorization error occurs
     */
    private void removeReadPolicies(DSpaceObject dso, String type) {
        try {
            resourcePolicyService.removePolicies(context, dso, type, Constants.READ);
        } catch (SQLException | AuthorizeException e) {
            throw new BulkAccessControlException(e);
        }
    }

    /**
     * create the new resource policies of bitstream.
     * then, call {@link ItemService#adjustItemPolicies(
     * Context, Item, Collection)} to adjust bitstream's default policies.
     * and also update the resource policies of its derivative bitstreams.
     *
     * @param bitstream the bitstream
     * @param item the item of bitstream
     * @param accessConditions the access conditions list in input
     * @throws SQLException if something goes wrong in the database
     * @throws AuthorizeException if an authorization error occurs
     */
    private void setBitstreamPolicies(Bitstream bitstream, Item item, List<AccessCondition> accessConditions)
        throws SQLException, AuthorizeException {

        accessConditions
                     .forEach(accessCondition -> createResourcePolicy(bitstream, accessCondition,
                         uploadAccessConditions.get(accessCondition.getName())));

        itemService.adjustBitstreamPolicies(context, item, item.getOwningCollection(), bitstream);
        mediaFilterService.updatePoliciesOfDerivativeBitstreams(context, item, bitstream);
    }

    /**
     * create the resource policy from the information
     * comes from the access condition.
     *
     * @param obj the dspace object
     * @param accessCondition the access condition
     * @param accessConditionOption the access condition option
     * @throws BulkAccessControlException if an exception occurs
     */
    private void createResourcePolicy(DSpaceObject obj, AccessCondition accessCondition,
                                      AccessConditionOption accessConditionOption) {

        String name = accessCondition.getName();
        String description = accessCondition.getDescription();
        Date startDate = accessCondition.getStartDate();
        Date endDate = accessCondition.getEndDate();

        try {
            accessConditionOption.createResourcePolicy(context, obj, name, description, startDate, endDate);
        } catch (Exception e) {
            throw new BulkAccessControlException(e);
        }
    }

    /**
     * Set the eperson in the context
     *
     * @param context the context
     * @throws SQLException if database error
     */
    protected void setEPerson(Context context) throws SQLException {
        EPerson myEPerson = epersonService.find(context, this.getEpersonIdentifier());

        if (myEPerson == null) {
            handler.logError("EPerson cannot be found: " + this.getEpersonIdentifier());
            throw new UnsupportedOperationException("EPerson cannot be found: " + this.getEpersonIdentifier());
        }

        context.setCurrentUser(myEPerson);
    }

    private void logInfo(List<AccessCondition> accessConditions, String mode, DSpaceObject dso) {
        String type = dso.getClass().getSimpleName();

        if (REPLACE_MODE.equals(mode) && isEmpty(accessConditions)) {
            handler.logInfo("Cleaning " + type + " {" + dso.getID() + "} policies");
            handler.logInfo("Inheriting policies from owning Collection in " + type + " {" + dso.getID() + "}");
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append(mode.equals(ADD_MODE) ? "Adding " : "Replacing ")
               .append(type)
               .append(" {")
               .append(dso.getID())
               .append("} policy")
               .append(mode.equals(ADD_MODE) ? " with " : " to ")
               .append("access conditions:");

        AppendAccessConditionsInfo(message, accessConditions);

        handler.logInfo(message.toString());

        if (REPLACE_MODE.equals(mode) && isAppendModeEnabled()) {
            handler.logInfo("Inheriting policies from owning Collection in " + type + " {" + dso.getID() + "}");
        }
    }

    private void AppendAccessConditionsInfo(StringBuilder message, List<AccessCondition> accessConditions) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        message.append("{");

        for (int i = 0; i <  accessConditions.size(); i++) {
            message.append(accessConditions.get(i).getName());

            Optional.ofNullable(accessConditions.get(i).getStartDate())
                    .ifPresent(date -> message.append(", start_date=" + dateFormat.format(date)));

            Optional.ofNullable(accessConditions.get(i).getEndDate())
                    .ifPresent(date -> message.append(", end_date=" + dateFormat.format(date)));

            if (i != accessConditions.size() - 1) {
                message.append(", ");
            }
        }

        message.append("}");
    }

    private boolean isAppendModeEnabled() {
        return configurationService.getBooleanProperty("core.authorization.installitem.inheritance-read.append-mode");
    }

    protected boolean isAuthorized(Context context) {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public BulkAccessControlScriptConfiguration<BulkAccessControl> getScriptConfiguration() {
        return new DSpace().getServiceManager()
                           .getServiceByName("bulk-access-control", BulkAccessControlScriptConfiguration.class);
    }

}
