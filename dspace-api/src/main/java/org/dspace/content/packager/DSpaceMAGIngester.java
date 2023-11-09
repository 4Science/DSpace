/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.packager;

import static java.util.Comparator.comparingInt;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.dspace.content.MetadataSchemaEnum.DC;
import static org.dspace.content.authority.Choices.CF_ACCEPTED;
import static org.dspace.core.Constants.ITEM;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.util.MapConverters;
import org.dspace.util.SimpleMapConverter;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.jdom2.Element;

/**
 * Packager plugin to ingest a MAG package that contains Item definition with attachments.
 */
public class DSpaceMAGIngester extends AbstractPackageIngester {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(DSpaceMAGIngester.class);
    private final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    private final BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance()
            .getBitstreamFormatService();
    private final BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private final WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance()
            .getWorkspaceItemService();
    private final MapConverters mapConverters = new DSpace().getSingletonService(MapConverters.class);

    private final static String ORIGINAL = "ORIGINAL";
    private final static String THUMBNAIL = "THUMBNAIL";
    private final static String MAG = "MAG";
    private final static String XML_FORMAT = ".xml";
    private final static String MAG_DIR = "MagXML";

    @Override
    public DSpaceObject ingest(Context context, DSpaceObject parent,
                               File pkgFile, PackageParameters params, String license)
            throws PackageValidationException, CrosswalkException,
            AuthorizeException, SQLException, IOException, WorkflowException {

        log.info(LogHelper.getHeader(context, "package_parse",
                "Parsing package for ingest, file=" + pkgFile.getName()));

        String sourceDir = unzip(pkgFile);
        MAGManifest manifest = parsePackage(context, sourceDir, params);

        if (manifest == null) {
            throw new PackageValidationException(
                    "No Manifest found (extension=" + XML_FORMAT + ").  Package is unacceptable!");
        }

        return ingestObject(context, parent, manifest, sourceDir, params, license);
    }

    @Override
    public DSpaceObject replace(Context context, DSpaceObject dsoToReplace,
                                File pkgFile, PackageParameters params)
            throws PackageValidationException, CrosswalkException, AuthorizeException, SQLException,
            IOException, WorkflowException {

        DSpaceObject dso;

        log.info(LogHelper.getHeader(context, "package_parse",
                "Parsing package for replace, file=" + pkgFile.getName()));

        String sourceDir = unzip(pkgFile);
        MAGManifest manifest = parsePackage(context, sourceDir, params);
        if (manifest == null) {
            throw new PackageValidationException(
                    "No MAG Manifest found.  Package is unacceptable!");
        }

        if (isNull(dsoToReplace)) {
            Optional<Item> existingItem = getExistingItem(context, manifest);
            if (existingItem.isPresent()) {
                dsoToReplace = existingItem.get();
            }
        }

        if (dsoToReplace == null) {
            dso = ingestObject(context, null, manifest, sourceDir, params, null);
            if (dso != null) {
                log.info(LogHelper.getHeader(context, "package_replace",
                        "Created new Object, type="
                                + Constants.typeText[dso.getType()]
                                + ", handle=" + dso.getHandle() + ", dbID="
                                + String.valueOf(dso.getID())));
            }
        } else {
            dso = replaceObject(context, dsoToReplace, manifest, sourceDir, params, null);
            log.info(LogHelper.getHeader(context, "package_replace",
                    "Replaced Object, type="
                            + Constants.typeText[dso.getType()]
                            + ", handle=" + dso.getHandle() + ", dbID="
                            + String.valueOf(dso.getID())));
        }
        return dso;
    }

    @Override
    public String getParameterHelp() {
        return "* manifestOnly=[boolean]      " +
                "Specify true if the ingest package consists of just a MAG manifest (mag.xml), without any content " +
                "files (defaults to false)." +
                "\n\n" +
                "* validate=[boolean]      " +
                "If true, enable XML validation of MAG file using schemas in document (default is true).";
    }

    private String unzip(File pkgFile) throws IOException {
        // does the zip file exist and can we write to the temp directory
        if (!pkgFile.canRead()) {
            log.error("Zip file '" + pkgFile.getAbsolutePath() + "' does not exist, or is not readable.");
        }

        String zipDir = Files.createTempDirectory(pkgFile.getName() + "-")
                .toFile().getAbsolutePath() + File.separator;
        String magDir = zipDir;
        ZipFile zf = new ZipFile(pkgFile);
        ZipEntry entry;
        Enumeration<? extends ZipEntry> entries = zf.entries();
        try {
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                if (entry.isDirectory()) {
                    if (!new File(zipDir + entry.getName()).mkdirs()) {
                        log.error("Unable to create contents directory: " + zipDir + entry.getName());
                    }
                } else {
                    String entryName = entry.getName();
                    File outFile = new File(zipDir + entryName);
                    // Verify that this file will be extracted into our zipDir (and not somewhere else!)
                    if (!outFile.toPath().normalize().startsWith(zipDir)) {
                        throw new IOException("Bad zip entry: '" + entryName
                                                  + "' in file '" + zipDir + "'!"
                                                  + " Cannot process this file.");
                    } else {
                        log.info("Extracting file: " + entryName);

                        int index = entryName.lastIndexOf(File.separator);
                        if (index > 0) {
                            File dir = new File(zipDir + entryName.substring(0, index));
                            if (!dir.exists() && !dir.mkdirs()) {
                                log.error("Unable to create directory: " + dir.getAbsolutePath());
                            }
                        }

                        //Entries could have too many directories, and we need to adjust the sourcedir
                        // file1.zip (MAGArchive / item1 / ...
                        //            MAGArchive / item2 / ...
                        // or
                        // file2.zip (item1 / ...
                        //            item2 / ...

                        //regex supports either windows or *nix file paths
                        String[] entryChunks = entryName.split("/|\\\\");
                        if (entryChunks.length > 2) {
                            // found the first level
                            if (StringUtils.equals(magDir, zipDir)) {
                                magDir = zipDir + entryChunks[0];
                            }
                        }

                        byte[] buffer = new byte[1024];
                        int len;
                        InputStream in = zf.getInputStream(entry);
                        BufferedOutputStream out = new BufferedOutputStream(
                            new FileOutputStream(outFile));
                        while ((len = in.read(buffer)) >= 0) {
                            out.write(buffer, 0, len);
                        }
                        in.close();
                        out.close();
                    }
                }
            }
        } finally {
            //Close zip file
            zf.close();
        }

        return magDir;
    }

    private MAGManifest parsePackage(Context context, String sourceDir, PackageParameters params)
            throws IOException, MetadataValidationException {

        boolean validate = params.getBooleanProperty("validate", false);

        boolean hasMagDir = FileUtils.listFiles(new File(sourceDir), new String[] {"xml"}, true)
                .stream()
                .anyMatch(file -> MAG_DIR.equals(file.getParent()));
        if (hasMagDir) {
            sourceDir = sourceDir + File.separator + MAG_DIR;
        }
        Optional<File> manifestFile = FileUtils.listFiles(new File(sourceDir), new String[] {"xml"}, false)
                .stream()
                .filter(file -> StringUtils.endsWith(file.getName(), ".xml")).findFirst();
        if (manifestFile.isEmpty()) {
            throw new RuntimeException("The MAG manifest does not exist into " + sourceDir);
        }

        return MAGManifest.create(new FileInputStream(manifestFile.get()), validate);
    }

    private DSpaceObject ingestObject(Context context, DSpaceObject parent, MAGManifest manifest,
            String sourceDir, PackageParameters params, String license)
                    throws IOException, SQLException, AuthorizeException, CrosswalkException,
                    PackageValidationException, WorkflowException {
        Optional<Item> existingItem = getExistingItem(context, manifest);

        Item item;
        try {
            UUID existingItemUUID = existingItem.map(DSpaceObject::getID).orElse(null);
            item = (Item) PackageUtils.createDSpaceObject(context, parent, ITEM, null, existingItemUUID, params);
        } catch (SQLException exception) {
            throw new PackageValidationException("Exception while ingesting " + sourceDir, exception);
        }

        if (item == null) {
            throw new PackageValidationException(
                    "Unable to initialize object specified by package (type='"
                            + ITEM + "' and parent='"
                            + parent.getHandle() + "').");
        }

        WorkspaceItem wsi = workspaceItemService.findByItem(context, item);

        Collection collection = item.getOwningCollection();
        if (collection == null) {
            if (wsi != null) {
                collection = wsi.getCollection();
            }
        }

        addMetadata(context, item, manifest);
        addBitstreams(context, item, manifest, sourceDir, params);
        addManifestBitstream(context, item, manifest);
        addLicense(context, item, license, collection);

        if (wsi != null) {
            PackageUtils.finishCreateItem(context, wsi, item.getHandle(), params);
        }

        PackageUtils.updateDSpaceObject(context, item);
        return item;
    }

    private Optional<Item> getExistingItem(Context context, MAGManifest manifest)
            throws PackageValidationException, MetadataValidationException, SQLException, AuthorizeException {
        String number = inventoryNumber(manifest);
        Iterator<Item> existingItems = itemService
                .findUnfilteredByMetadataField(context, DC.getName(), "identifier", "inventorynumber", number);
        return existingItems.hasNext() ? Optional.of(existingItems.next()) : Optional.empty();
    }

    private void addMetadata(Context context, Item item, MAGManifest manifest) throws SQLException {
        addMetadataByXPath(context, item, manifest, "/mag:metadigit/mag:bib/dc:creator",
                "dc", "contributor", "author");
        addMetadataByXPath(context, item, manifest, "/mag:metadigit/mag:bib/dc:publisher",
                "dc", "publisher", null);
        addMetadataByXPath(context, item, manifest, "/mag:metadigit/mag:bib/dc:subject",
                "dc", "subject", null);
        addMetadataByXPath(context, item, manifest, "/mag:metadigit/mag:bib/dc:description",
                "dc", "description", null);
        addMetadataByXPath(context, item, manifest, "/mag:metadigit/mag:bib/dc:contributor",
                "dc", "contributor", "contributor");
        addMetadataByXPath(context, item, manifest, "/mag:metadigit/mag:bib/dc:type",
                "dc", "type", null);
        addMetadataByXPath(context, item, manifest, "/mag:metadigit/mag:bib/dc:format",
                "dc", "format", null);
        addMetadataByXPath(context, item, manifest, "/mag:metadigit/mag:bib/dc:coverage",
                "dc", "relation", "place");
        addMetadataByXPath(context, item, manifest, "/mag:metadigit/mag:bib/dc:source",
                "dc", "source", "content");
        addMetadataByXPath(context, item, manifest, "/mag:metadigit/mag:bib/dc:rights",
                "dc", "rights", null);
        addMetadataByXPath(context, item, manifest, "/mag:metadigit/mag:bib/mag:holdings/mag:library",
                "dc", "rights", "holder");
        addMetadataByXPath(context, item, manifest, "/mag:metadigit/mag:bib/mag:holdings/mag:inventory_number",
                "dc", "identifier", "inventorynumber");
        addMetadataByXPath(context, item, manifest, "/mag:metadigit/mag:bib/mag:holdings/mag:shelfmark",
                "dc", "identifier", "shelfmark");
        addMetadataByXPath(context, item, manifest, "/mag:metadigit/mag:bib/dc:relation",
                "dc", "relation", "ispartof");
        addMetadataByXPath(context, item, manifest, "/mag:metadigit/mag:bib/dc:relation",
                "dc", "relation", "references");
        addTitleMetadata(context, item, manifest);
        addDateMetadata(context, item, manifest);
        addLanguageMetadata(context, item, manifest);
    }

    private void addLanguageMetadata(Context context, Item item, MAGManifest manifest) throws SQLException {
        Optional<SimpleMapConverter> converter = mapConverters.getConverter("iso639");
        if (converter.isPresent()) {
            List<Element> elements = manifest.getElementsByXPath("/mag:metadigit/mag:bib/dc:language", true);
            if (nonNull(elements)) {
                for (Element tag : elements) {
                    if (isNotBlank(tag.getValue())) {
                        String value = tag.getValue().toLowerCase();
                        String isoLanguage = converter.get().getValue(value);
                        if (isNotBlank(isoLanguage) && !isoLanguage.equalsIgnoreCase(value)) {
                            itemService.addMetadata(context, item, "dc", "language", "iso", null,
                                    isoLanguage, null, CF_ACCEPTED);
                        }
                    }
                }
            }
        }
    }

    private void addDateMetadata(Context context, Item item, MAGManifest manifest) throws SQLException {
        String dateRegex = "^(\\d{4})(-(\\d{2})(-(\\d{2}))?)?$"; // (YYYY) OR (YYYY-mm) OR (YYYY-mm-DD)
        Pattern datePattern = Pattern.compile(dateRegex);

        List<Element> elements = manifest.getElementsByXPath("/mag:metadigit/mag:bib/dc:date", true);
        if (nonNull(elements)) {

            for (Element tag : elements) {
                String value = tag.getValue();

                if (isNotBlank(value)) {
                    Matcher dateMatcher = datePattern.matcher(value);
                    String element = dateMatcher.matches() ? "date" : "coverage";
                    String qualifier = dateMatcher.matches() ? "issued" : "temporal";
                    itemService.addMetadata(context, item, "dc", element, qualifier, null,
                            value, null, CF_ACCEPTED);
                }
            }
        }
    }

    private void addTitleMetadata(Context context, Item item, MAGManifest manifest) throws SQLException {
        List<Element> elements = manifest.getElementsByXPath("/mag:metadigit/mag:bib/dc:title", true);
        if (nonNull(elements)) {

            for (int i = 0; i < elements.size(); i++) {
                Element tag = elements.get(i);
                if (isNotBlank(tag.getValue())) {
                    // First title goes as dc.title, other titles as dc.title.alternative
                    String qualifier = i == 0 ? null : "alternative";
                    itemService.addMetadata(context, item, "dc", "title", qualifier, null,
                            tag.getValue(), null, CF_ACCEPTED);
                }
            }
        }
    }

    private void addMetadataByXPath(Context context, Item item, MAGManifest manifest, String path,
                                    String schema, String element, String qualifier) throws SQLException {
        List<Element> elements = manifest.getElementsByXPath(path, true);
        if (nonNull(elements)) {

            for (Element tag : elements) {
                if (isNotBlank(tag.getValue())) {
                    itemService.addMetadata(context, item, schema, element, qualifier, null,
                            tag.getValue(), null, CF_ACCEPTED);
                }
            }
        }
    }

    private DSpaceObject replaceObject(Context context, DSpaceObject dso, MAGManifest manifest,
            String sourceDir, PackageParameters params, String license)
                    throws IOException, SQLException, AuthorizeException, CrosswalkException {

        if (log.isDebugEnabled()) {
            log.debug("Object to be replaced (handle=" + dso.getHandle()
                    + ") is " + Constants.typeText[dso.getType()] + " id="
                    + dso.getID());
        }

        PackageUtils.removeAllBitstreams(context, dso);
        PackageUtils.clearAllMetadata(context, dso);

        if (dso.getType() == ITEM) {
            Item item = (Item) dso;

            addMetadata(context, item, manifest);
            addBitstreams(context, item, manifest, sourceDir, params);
            addManifestBitstream(context, item, manifest);

            Collection owningCollection = getOwningCollection(context, dso, item);
            addLicense(context, item, license, owningCollection);
        }
        PackageUtils.updateDSpaceObject(context, dso);
        return dso;
    }

    private void addOriginalBitstreamMetadata(Context context, Element fileElement,
                                              Bitstream bitstream, MAGManifest manifest)
            throws MetadataValidationException, SQLException {
        addMetricMetadata(context, bitstream, manifest, "samplingfrequencyunit", "2", "inch", "TIFF600");
        addMetricMetadata(context, bitstream, manifest, "samplingfrequencyplane", "2", "object plane", "TIFF600");
        addMetricMetadata(context, bitstream, manifest, "xsamplingfrequency", "TIFF600");
        addMetricMetadata(context, bitstream, manifest, "ysamplingfrequency", "TIFF600");
        addMetricMetadata(context, bitstream, manifest, "photometricinterpretation",
                "mix", "colorSpace", null, "TIFF600");
        addMetricMetadata(context, bitstream, manifest, "bitpersample", "mix", "bitsPerSampleValue", null, "TIFF600");
        addImgGroupMetadata(context, bitstream, manifest, "mag:format/niso:compression",
                "mix", "compressionScheme", null, "TIFF600");
        addImgGroupMetadata(context, bitstream, manifest,
                "mag:scanning/niso:devicesource", "mix", "captureDevice", null, "TIFF600");
        addImgGroupMetadata(context, bitstream, manifest, "mag:scanning/niso:scanningsystem/niso:scanner_manufacturer",
                "mix", "scannerManufacturer", null, "TIFF600");
        addImgGroupMetadata(context, bitstream, manifest,
                "mag:scanning/niso:scanningsystem/niso:scanner_model", "mix", "scannerModelName",
                null, "TIFF600");
        addImgGroupMetadata(context, bitstream, manifest,
                "mag:scanning/niso:scanningsystem/niso:capture_software", "mix", "scanningSoftwareName",
                null, "TIFF600");
        addIiifTocMetadata(context, bitstream, manifest, fileElement);
    }

    private void addIiifTocMetadata(Context context, Bitstream bitstream, MAGManifest manifest, Element fileElement)
            throws SQLException {
        String nomenclature = getNomenclature(manifest, fileElement);
        Integer sequenceNumber = getSequenceNumber(manifest, fileElement);
        List<Element> strus = manifest.getElementsByXPath("/mag:metadigit/mag:stru", true);

        if (isNotBlank(nomenclature) && nonNull(sequenceNumber) && nonNull(strus)) {
            for (Element stru : strus) {

                Integer start = null;
                Integer stop = null;
                Element startTag = manifest.getElementByXPath("element/start", true, stru);
                Element stopTag = manifest.getElementByXPath("element/stop", true, stru);
                if (nonNull(startTag) && nonNull(startTag.getAttributeValue("sequence_number"))) {
                    start = Integer.valueOf(startTag.getAttributeValue("sequence_number"));
                }
                if (nonNull(stopTag) && nonNull(stopTag.getAttributeValue("sequence_number"))) {
                    stop = Integer.valueOf(stopTag.getAttributeValue("sequence_number"));
                }

                if ((isNull(start) || sequenceNumber >= start) && (isNull(stop) || sequenceNumber <= stop)) {
                    Element struNomenclatureTag = manifest.getElementByXPath("nomenclature", true, stru);

                    if (nonNull(struNomenclatureTag) && isNotBlank(struNomenclatureTag.getValue())) {
                        String value = "Indice del Documento|||"
                                + struNomenclatureTag.getValue() + "|||" + nomenclature;
                        bitstreamService.addMetadata(context, bitstream, "iiif", "toc", null, null,
                                value, null, CF_ACCEPTED);
                    }
                }
            }
        }
    }

    private void addThumbnailBitstreamMetadata(Context context, Element originalFileElement,
                                               Bitstream bitstream, MAGManifest manifest)
            throws MetadataValidationException, SQLException {
        addMetricMetadata(context, bitstream, manifest, "samplingfrequencyunit", "2", "inch", "JPEG150");
        addMetricMetadata(context, bitstream, manifest, "samplingfrequencyplane", "2", "object plane", "JPEG150");
        addMetricMetadata(context, bitstream, manifest, "xsamplingfrequency", "JPEG150");
        addMetricMetadata(context, bitstream, manifest, "ysamplingfrequency", "JPEG150");
        addMetricMetadata(context, bitstream, manifest, "photometricinterpretation",
                "mix", "colorSpace", null, "JPEG150");
        addMetricMetadata(context, bitstream, manifest, "bitpersample", "mix", "bitsPerSampleValue", null, "JPEG150");
        addImgGroupMetadata(context, bitstream, manifest, "mag:format/niso:compression",
                "mix", "compressionScheme", null, "TIFF600");
    }

    private void addImgGroupMetadata(Context context, Bitstream bitstream, MAGManifest manifest, String path,
                                     String schema, String element, String qualifier, String imgGroupId)
            throws MetadataValidationException, SQLException {
        Optional<Element> tiffImgGroupElement = getImgGroupElement(manifest, imgGroupId);
        if (tiffImgGroupElement.isPresent()) {
            Element pathElement = manifest.getElementByXPath(path, true, tiffImgGroupElement.get());
            if (nonNull(pathElement) && nonNull(pathElement.getValue())) {
                bitstreamService.addMetadata(context, bitstream, schema, element, qualifier,
                        null, pathElement.getValue(), null, CF_ACCEPTED);
            }
        }
    }

    private void addMetricMetadata(Context context, Bitstream bitstream, MAGManifest manifest,
                                   String metricName, String imgGroupId)
            throws MetadataValidationException, SQLException {
        addMetricMetadata(context, bitstream, manifest, metricName, "mix", metricName, null, imgGroupId);
    }

    private void addMetricMetadata(Context context, Bitstream bitstream, MAGManifest manifest, String metricName,
                                   String metadataSchema, String metadataElement,
                                   String metadataQualifier, String imgGroupId)
            throws MetadataValidationException, SQLException {
        addImgGroupMetadata(context, bitstream, manifest, "mag:image_metrics/niso:" + metricName,
                metadataSchema, metadataElement, metadataQualifier, imgGroupId);
    }

    private void addMetricMetadata(Context context, Bitstream bitstream, MAGManifest manifest,
                                   String metricName, String metricMatchValue, String metadataValue, String imgGroupId)
            throws MetadataValidationException, SQLException {
        Optional<Element> tiffImgGroup = getImgGroupElement(manifest, imgGroupId);
        if (tiffImgGroup.isPresent()) {
            Element metric = manifest
                    .getElementByXPath("mag:image_metrics/niso:" + metricName, true, tiffImgGroup.get());
            if (nonNull(metric) && nonNull(metric.getValue())
                    && metric.getValue().equalsIgnoreCase(metricMatchValue)) {
                bitstreamService.addMetadata(context, bitstream, "mix", metricName,
                        null, null, metadataValue, null, CF_ACCEPTED);
            }
        }
    }

    private static Optional<Element> getImgGroupElement(MAGManifest manifest, String imgGroupId)
            throws MetadataValidationException {
        List<Element> imgGroups = manifest.getElementsByXPath("/mag:metadigit/mag:gen/mag:img_group", true);
        if (nonNull(imgGroups)) {
            return imgGroups.stream()
                    .filter(imgGroup -> imgGroup.getAttributeValue("ID", EMPTY).equalsIgnoreCase(imgGroupId))
                    .findAny();
        }
        return Optional.empty();
    }

    private void addBitstreams(Context context, Item item, MAGManifest manifest,
            String sourceDir, PackageParameters params)
                    throws SQLException, AuthorizeException, MetadataValidationException, IOException {
        List<Element> files = manifest.getFiles();
        Bundle originalBundle = getBundleElseCreate(context, item, ORIGINAL);
        Bundle thumbnailsBundle = getBundleElseCreate(context, item, THUMBNAIL);

        for (Element file : files) {
            Integer originalSequenceId = manifest.getOriginalSequenceId(file);
            String originalName = addOriginalBitstream(context, manifest, sourceDir,
                    params, file, originalBundle, originalSequenceId);
            if (originalName != null) {
                addThumbnailBitstream(context, manifest, sourceDir, params, file, thumbnailsBundle,
                        originalSequenceId, originalName);
            }
        }

        updatePrimaryBitstream(context, originalBundle);
        updatePrimaryBitstream(context, thumbnailsBundle);
    }

    private void updatePrimaryBitstream(Context context, Bundle bundle) throws SQLException, AuthorizeException {
        Optional<Bitstream> primaryBitstream = bundle.getBitstreams()
                .stream().min(comparingInt(Bitstream::getSequenceID));
        if (primaryBitstream.isPresent()) {
            bundle.setPrimaryBitstreamID(primaryBitstream.get());
            bundleService.update(context, bundle);
        }
    }

    private void addThumbnailBitstream(Context context, MAGManifest manifest,
            String sourceDir, PackageParameters params,
            Element file, Bundle thumbnailsBundle, Integer originalSequenceId, String originalName)
                    throws IOException, SQLException, AuthorizeException, MetadataValidationException {
        String thumbnailPath = manifest.getThumbnailFileName(sourceDir, file);
        String thumbnailExtension = thumbnailPath.substring(thumbnailPath.lastIndexOf("."));
        Optional<InputStream> thumbnailFileStream = Optional.of(new FileInputStream(thumbnailPath));
        if (thumbnailFileStream.isPresent()) {
            Bitstream bitstream = createBitstream(context, thumbnailsBundle,
                    thumbnailFileStream.get(), thumbnailPath, originalSequenceId, originalName + thumbnailExtension);
            addThumbnailBitstreamMetadata(context, file, bitstream, manifest);
        } else {
            log.warn("Bitstream creation failed. There are no files in provided path: {}", thumbnailPath);
        }
    }

    private String addOriginalBitstream(Context context, MAGManifest manifest, String sourceDir,
            PackageParameters params, Element file, Bundle originalBundle, Integer originalSequenceId)
                    throws IOException, SQLException, AuthorizeException, MetadataValidationException {
        String originalPath = manifest.getOriginalFileName(sourceDir, file);
        Optional<InputStream> originalFileStream = Optional.of(new FileInputStream(originalPath));
        if (originalFileStream.isPresent()) {
            Bitstream bitstream = createBitstream(context, originalBundle,
                    originalFileStream.get(), originalPath, originalSequenceId);
            addOriginalBitstreamMetadata(context, file, bitstream, manifest);
            return bitstream.getName();
        } else {
            log.warn("Bitstream creation failed. There are no files in provided path: {}", originalPath);
        }
        return null;
    }

    private Bundle getBundleElseCreate(Context context, Item item, String name)
            throws SQLException, AuthorizeException {
        List<Bundle> originalBundles = itemService.getBundles(item, name);
        return isNotEmpty(originalBundles) ? originalBundles.get(0) : bundleService.create(context, item, name);
    }

    private Bitstream createBitstream(Context context, Bundle bundle, InputStream fileStream,
                                      String path, Integer sequenceId)
            throws IOException, SQLException, AuthorizeException {
        String fileName = path.substring(path.lastIndexOf(File.separator) + 1);
        return createBitstream(context, bundle, fileStream, path, sequenceId, fileName);
    }

    private Bitstream createBitstream(Context context, Bundle bundle, InputStream fileStream,
                                      String path, Integer sequenceId, String fileName)
            throws IOException, SQLException, AuthorizeException {
        Bitstream bitstream = bitstreamService.create(context, bundle, fileStream);
        bitstream.setName(context, fileName);
        bitstream.setSequenceID(sequenceId);

        BitstreamFormat bitstreamFormat = bitstreamFormatService.guessFormat(context, bitstream);
        bitstreamService.setFormat(context, bitstream, bitstreamFormat);
        bitstreamService.update(context, bitstream);
        return bitstream;
    }

    private void addManifestBitstream(Context context, Item item, MAGManifest manifest)
            throws IOException, SQLException, AuthorizeException {

        Bundle mdBundle = bundleService.create(context, item, MAG);

        Bitstream manifestBitstream = bitstreamService.create(context, mdBundle, manifest.getMagsAsStream());
        manifestBitstream.setName(context, MAGManifest.MANIFEST_FILE);
        manifestBitstream.setSource(context, MAGManifest.MANIFEST_FILE);
        bitstreamService.update(context, manifestBitstream);

        BitstreamFormat manifestFormat = PackageUtils
                .findOrCreateBitstreamFormat(context, MAG, "application/xml", "MAG package manifest");
        manifestBitstream.setFormat(context, manifestFormat);
        bitstreamService.update(context, manifestBitstream);
    }

    private void addLicense(Context context, Item item, String license,
                            Collection collection)
            throws AuthorizeException, SQLException, IOException {
        if (PackageUtils.findDepositLicense(context, item) == null) {
            PackageUtils.addDepositLicense(context, license, item, collection);
        }
    }

    private String inventoryNumber(MAGManifest manifest) throws PackageValidationException {
        Element inventoryNumber = manifest
                .getElementByXPath("/mag:metadigit/mag:bib/mag:holdings/mag:inventory_number", true);
        if (isNull(inventoryNumber) || isBlank(inventoryNumber.getValue())) {
            throw new PackageValidationException("Manifest is missing the required inventory number.");
        }
        return inventoryNumber.getValue();
    }

    private Collection getOwningCollection(Context context, DSpaceObject dso, Item item) throws SQLException {
        Collection owningCollection = (Collection) ContentServiceFactory.getInstance().getDSpaceObjectService(dso)
                .getParentObject(context, dso);
        if (owningCollection == null) {
            InProgressSubmission inProgressSubmission = workspaceItemService.findByItem(context, item);
            if (inProgressSubmission == null) {
                inProgressSubmission = WorkflowServiceFactory.getInstance().getWorkflowItemService()
                        .findByItem(context, item);
            }
            owningCollection = inProgressSubmission.getCollection();
        }
        return owningCollection;
    }

    private static String getNomenclature(MAGManifest manifest, Element fileElement) {
        String nomenclature = null;
        Element nomenclatureTag = manifest.getElementByXPath("nomenclature", true, fileElement);
        if (nonNull(nomenclatureTag) && nonNull(nomenclatureTag.getValue())) {
            nomenclature = nomenclatureTag.getValue();
        }
        return nomenclature;
    }

    private static Integer getSequenceNumber(MAGManifest manifest, Element fileElement) {
        Integer sequenceNumber = null;
        Element sequenceNumberTag = manifest.getElementByXPath("sequence_number", true, fileElement);
        if (nonNull(sequenceNumberTag) && nonNull(sequenceNumberTag.getValue())) {
            sequenceNumber = Integer.valueOf(sequenceNumberTag.getValue());
        }
        return sequenceNumber;
    }

}
