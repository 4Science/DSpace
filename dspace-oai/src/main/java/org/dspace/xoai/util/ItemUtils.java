/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
import com.lyncode.xoai.util.Base64Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.factory.UtilServiceFactory;
import org.dspace.app.util.service.MetadataExposureService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xoai.data.DSpaceItem;

/**
 * @author Lyncode Development Team (dspace at lyncode dot com)
 */
@SuppressWarnings("deprecation")
public class ItemUtils {
    private static final Logger log = LogManager.getLogger(ItemUtils.class);

    private static final MetadataExposureService metadataExposureService
            = UtilServiceFactory.getInstance().getMetadataExposureService();

    private static final ItemService itemService
            = ContentServiceFactory.getInstance().getItemService();

    private static final RelationshipService relationshipService
            = ContentServiceFactory.getInstance().getRelationshipService();

    private static final BitstreamService bitstreamService
            = ContentServiceFactory.getInstance().getBitstreamService();

    private static final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    private static final AuthorizeService authorizeService
            = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    private static final MetadataAuthorityService mam = ContentAuthorityServiceFactory
            .getInstance().getMetadataAuthorityService();

    private static final ChoiceAuthorityService choicheAuthManager = ContentAuthorityServiceFactory
            .getInstance().getChoiceAuthorityService();

    private static final HandleService handleService = HandleServiceFactory
            .getInstance().getHandleService();

    public static Integer MAX_DEEP = 2;
    public static String AUTHORITY = "authority";

    /**
     * Default constructor
     */
    private ItemUtils() {
    }

    public static Element getElement(List<Element> list, String name) {
        for (Element e : list) {
            if (name.equals(e.getName())) {
                return e;
            }
        }

        return null;
    }

    public static Element create(String name) {
        Element e = new Element();
        e.setName(name);
        return e;
    }

    public static Element.Field createValue(String name, String value) {
        Element.Field e = new Element.Field();
        e.setValue(value);
        e.setName(name);
        return e;
    }

    /***
     * Write metadata into a Element structure.
     *
     * @param schema The reference schema
     * @param val The metadata value
     * @return
     */
    private static Element writeMetadata(Element  schema,MetadataValue val) {
        return writeMetadata(schema, val, false);
    }

    /***
     * Write metadata into a Element structure.
     *
     * @param schema The reference schema
     * @param val The metadata value
     * @param forceEmptyQualifier Set to true to create a qualifier element
     *              with value "none" when qualifier is empty. Otherwise the qualifier element is not created.
     * @return
     */
    private static Element writeMetadata(Element schema, MetadataValue val, boolean forceEmptyQualifier) {

        Element valueElem = null;
        valueElem = schema;

        // Has element.. with XOAI one could have only schema and value
        if (val.getElement() != null && !val.getElement().equals("")) {
            Element element = getElement(schema.getElement(), val.getElement());
            if (element == null) {
                element = create(val.getElement());
                schema.getElement().add(element);
            }
            valueElem = element;

            // Qualified element?
            if (val.getQualifier() != null && !val.getQualifier().equals("")) {
                Element qualifier = getElement(element.getElement(), val.getQualifier());
                if (qualifier == null) {
                    qualifier = create(val.getQualifier());
                    element.getElement().add(qualifier);
                }
                valueElem = qualifier;
            } else if (forceEmptyQualifier) {
                Element qualifier = getElement(element.getElement(), "none");
                // if (qualifier == null)
                {
                    qualifier = create("none");
                    element.getElement().add(qualifier);
                }
                valueElem = qualifier;
            }
        }

        // Language?
        if (val.getLanguage() != null && !val.getLanguage().equals("")) {
            Element language = getElement(valueElem.getElement(), val.getLanguage());
            // remove single language
            // if (language == null)
            {
                language = create(val.getLanguage());
                valueElem.getElement().add(language);
            }
            valueElem = language;
        } else {
            Element language = getElement(valueElem.getElement(), "none");
            // remove single language
            // if (language == null)
            {
                language = create("none");
                valueElem.getElement().add(language);
            }
            valueElem = language;
        }

        valueElem.getField().add(createValue("value", val.getValue()));
        if (val.getAuthority() != null) {
            valueElem.getField().add(createValue("authority", val.getAuthority()));
            if (val.getConfidence() != Choices.CF_NOVALUE) {
                valueElem.getField().add(createValue("confidence", val.getConfidence() + ""));
            }
        }
        return valueElem;

    }

    private static Element createBundlesElement(Context context, Item item) throws SQLException {
        Element bundles = create("bundles");

        List<Bundle> bs;

        bs = item.getBundles();
        for (Bundle b : bs) {
            Element bundle = create("bundle");
            bundles.getElement().add(bundle);
            bundle.getField().add(createValue("name", b.getName()));

            Element bitstreams = create("bitstreams");
            bundle.getElement().add(bitstreams);
            List<Bitstream> bits = b.getBitstreams();
            for (Bitstream bit : bits) {
                // Check if bitstream is null and log the error
                if (bit == null) {
                    log.error("Null bitstream found, check item uuid: " + item.getID());
                    break;
                }
                boolean primary = false;
                // Check if current bitstream is in original bundle + 1 of the 2 following
                // Bitstream = primary bitstream in bundle -> true
                // No primary bitstream found in bundle-> only the first one gets flagged as "primary"
                if (b.getName() != null && b.getName().equals("ORIGINAL") && (b.getPrimaryBitstream() != null
                        && b.getPrimaryBitstream().getID() == bit.getID()
                        || b.getPrimaryBitstream() == null && bit.getID() == bits.get(0).getID())) {
                    primary = true;
                }

                Element bitstream = create("bitstream");
                bitstreams.getElement().add(bitstream);

                String baseUrl = configurationService.getProperty("oai.bitstream.baseUrl");
                String url = baseUrl + "/bitstreams/" + bit.getID().toString() + "/download";

                String cks = bit.getChecksum();
                String cka = bit.getChecksumAlgorithm();
                String oname = bit.getSource();
                String name = bit.getName();
                String description = bit.getDescription();

                if (name != null) {
                    bitstream.getField().add(createValue("name", name));
                }
                if (oname != null) {
                    bitstream.getField().add(createValue("originalName", name));
                }
                if (description != null) {
                    bitstream.getField().add(createValue("description", description));
                }
                // Add bitstream embargo information (READ policy present, for Anonymous group with a start date)
                addResourcePolicyInformation(context, bit, bitstream);

                bitstream.getField().add(createValue("format", bit.getFormat(context).getMIMEType()));
                bitstream.getField().add(createValue("size", "" + bit.getSizeBytes()));
                bitstream.getField().add(createValue("url", url));
                bitstream.getField().add(createValue("checksum", cks));
                bitstream.getField().add(createValue("checksumAlgorithm", cka));
                bitstream.getField().add(createValue("sid", bit.getSequenceID() + ""));
                // Add primary bitstream field to allow locating easily the primary bitstream information
                bitstream.getField().add(createValue("primary", primary + ""));
            }
        }

        return bundles;
    }

    /**
     * This method will add metadata information about associated resource policies for a give bitstream.
     * It will parse of relevant policies and add metadata information
     * @param context
     * @param bitstream the bitstream object
     * @param bitstreamEl the bitstream metadata object to add resource policy information to
     * @throws SQLException
     */
    private static void addResourcePolicyInformation(Context context, Bitstream bitstream, Element bitstreamEl)
            throws SQLException {
        // Pre-filter access policies by DSO (bitstream) and Action (READ)
        List<ResourcePolicy> policies = authorizeService.getPoliciesActionFilter(context, bitstream, Constants.READ);

        // Create resourcePolicies container
        Element resourcePolicies = create("resourcePolicies");

        for (ResourcePolicy policy : policies) {
            String groupName = policy.getGroup() != null ? policy.getGroup().getName() : null;
            String user = policy.getEPerson() != null ? policy.getEPerson().getName() : null;
            String action = Constants.actionText[policy.getAction()];
            Date startDate = policy.getStartDate();
            Date endDate = policy.getEndDate();

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

            Element resourcePolicyEl = create("resourcePolicy");
            resourcePolicyEl.getField().add(createValue("group", groupName));
            resourcePolicyEl.getField().add(createValue("user", user));
            resourcePolicyEl.getField().add(createValue("action", action));
            // Only add start-date if group is different to anonymous, or there is an active embargo
            if (startDate != null && startDate.after(new Date())) {
                resourcePolicyEl.getField().add(createValue("start-date", formatter.format(startDate)));
            }
            if (endDate != null) {
                resourcePolicyEl.getField().add(createValue("end-date", formatter.format(endDate)));
            }
            // Add resourcePolicy to list of resourcePolicies
            resourcePolicies.getElement().add(resourcePolicyEl);
        }
        // Add list of resource policies to the corresponding Bitstream XML Element
        bitstreamEl.getElement().add(resourcePolicies);
    }

    private static Element createLicenseElement(Context context, Item item)
            throws SQLException, AuthorizeException, IOException {
        Element license = create("license");
        List<Bundle> licBundles;
        licBundles = itemService.getBundles(item, Constants.LICENSE_BUNDLE_NAME);
        if (!licBundles.isEmpty()) {
            Bundle licBundle = licBundles.get(0);
            List<Bitstream> licBits = licBundle.getBitstreams();
            if (!licBits.isEmpty()) {
                Bitstream licBit = licBits.get(0);
                if (authorizeService.authorizeActionBoolean(context, licBit, Constants.READ)) {
                    InputStream in;

                    in = bitstreamService.retrieve(context, licBit);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    Utils.bufferedCopy(in, out);
                    license.getField().add(createValue("bin", Base64Utils.encode(out.toString())));
                } else {
                    log.info("Missing READ rights for license bitstream. Did not include license bitstream for item: "
                            + item.getID() + ".");
                }
            }
        }
        return license;
    }

    /**
     * This method will add all sub-elements to a top element, like: dc, or dcterms, ...     *
     * @param schema         Element argument passed by reference that will be changed
     * @param val            Metadatavalue that will be processed
     * @throws SQLException
     */
    private static void fillSchemaElement(Element schema, MetadataValue val) throws SQLException {
        MetadataField field = val.getMetadataField();
        Element valueElem = schema;

        // Has element.. with XOAI one could have only schema and value
        if (field.getElement() != null && !field.getElement().equals("")) {
            Element element = getElement(schema.getElement(), field.getElement());
            if (element == null) {
                element = create(field.getElement());
                schema.getElement().add(element);
            }
            valueElem = element;

            // Qualified element?
            if (field.getQualifier() != null && !field.getQualifier().equals("")) {
                Element qualifier = getElement(element.getElement(), field.getQualifier());
                if (qualifier == null) {
                    qualifier = create(field.getQualifier());
                    element.getElement().add(qualifier);
                }
                valueElem = qualifier;
            }
        }

        // Language?
        if (val.getLanguage() != null && !val.getLanguage().equals("")) {
            Element language = getElement(valueElem.getElement(), val.getLanguage());
            if (language == null) {
                language = create(val.getLanguage());
                valueElem.getElement().add(language);
            }
            valueElem = language;
        } else {
            Element language = getElement(valueElem.getElement(), "none");
            if (language == null) {
                language = create("none");
                valueElem.getElement().add(language);
            }
            valueElem = language;
        }

        valueElem.getField().add(createValue("value", val.getValue()));
        if (val.getAuthority() != null) {
            valueElem.getField().add(createValue("authority", val.getAuthority()));
            if (val.getConfidence() != Choices.CF_NOVALUE) {
                valueElem.getField().add(createValue("confidence", val.getConfidence() + ""));
            }
        }
    }

    /**
     * Utility method to retrieve a structured XML in XOAI format
     * @param context
     * @param item
     * @return Structured XML Metadata in XOAI format
     */
    public static Metadata retrieveMetadata(Context context, Item item) {
        Metadata metadata;

        // read all metadata into Metadata Object
        metadata = new Metadata();

        List<MetadataValue> vals = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        for (MetadataValue val : vals) {
            MetadataField field = val.getMetadataField();
            try {
                // Don't expose fields that are hidden by configuration
                if (metadataExposureService.isHidden(context, field.getMetadataSchema().getName(), field.getElement(),
                        field.getQualifier())) {
                    continue;
                }

                Element schema = getElement(metadata.getElement(), field.getMetadataSchema().getName());
                if (schema == null) {
                    schema = create(field.getMetadataSchema().getName());
                    metadata.getElement().add(schema);
                }

                fillSchemaElement(schema, val);
            } catch (SQLException se) {
                throw new RuntimeException(se);
            }
        }

        // Done! Metadata has been read!
        // Now adding bitstream info
        try {
            Element bundles = createBundlesElement(context, item);
            metadata.getElement().add(bundles);
        } catch (SQLException e) {
            log.warn(e.getMessage(), e);
        }

        // Other info
        Element other = create("others");

        other.getField().add(createValue("handle", item.getHandle()));
        other.getField().add(createValue("identifier", DSpaceItem.buildIdentifier(item.getHandle())));
        other.getField().add(createValue("lastModifyDate", item.getLastModified().toString()));
        metadata.getElement().add(other);

        // Repository Info
        Element repository = create("repository");
        repository.getField().add(createValue("url", configurationService.getProperty("dspace.ui.url")));
        repository.getField().add(createValue("name", configurationService.getProperty("dspace.name")));
        repository.getField().add(createValue("mail", configurationService.getProperty("mail.admin")));
        metadata.getElement().add(repository);

        // Licensing info
        try {
            Element license = createLicenseElement(context, item);
            metadata.getElement().add(license);
        } catch (AuthorizeException | IOException | SQLException e) {
            log.warn(e.getMessage(), e);
        }

        return metadata;
    }
}
