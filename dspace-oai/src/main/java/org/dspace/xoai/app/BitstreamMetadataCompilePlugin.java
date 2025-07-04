/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app;

import java.util.Map;

import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.xoai.util.ItemUtils;

/**
 * This plugin can be used to add additional metadata to the bitstream.compile solr OAI core field.
 * Those metadata are defined in the configuration file, with a proper mapping between the field name and the
 * metadata field.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class BitstreamMetadataCompilePlugin
    implements XOAIExtensionBitstreamCompilePlugin {

    protected final Map<String, String> metadataMapping;

    protected final BitstreamService bitstreamService;

    public BitstreamMetadataCompilePlugin(
        Map<String, String> metadataMapping,
        BitstreamService bitstreamService
    ) {
        this.metadataMapping = metadataMapping;
        this.bitstreamService = bitstreamService;
    }

    @Override
    public void appendElements(Context context, Element bitstreamElement, Bitstream bitstream) {
        this.metadataMapping.forEach(
            (fieldName, metadataField) -> addToBitstream(bitstream, bitstreamElement, metadataField, fieldName)
        );/*
        addToBitstream(bitstream, bitstreamElement, "iiif.toc", "toc");
        addToBitstream(bitstream, bitstreamElement, "mix.samplingfrequencyunit", "samplingfrequencyunit");
        addToBitstream(bitstream, bitstreamElement, "mix.samplingfrequencyplane", "samplingfrequencyplane");
        addToBitstream(bitstream, bitstreamElement, "mix.xsamplingfrequency", "xsamplingfrequency");
        addToBitstream(bitstream, bitstreamElement, "mix.ysamplingfrequency", "ysamplingfrequency");
        addToBitstream(bitstream, bitstreamElement, "mix.colorSpace", "colorSpace");
        addToBitstream(bitstream, bitstreamElement, "mix.bitsPerSampleValue", "bitsPerSampleValue");
        addToBitstream(bitstream, bitstreamElement, "mix.compressionScheme", "compressionScheme");
        addToBitstream(bitstream, bitstreamElement, "mix.captureDevice", "captureDevice");
        addToBitstream(bitstream, bitstreamElement, "mix.scannerManufacturer", "scannerManufacturer");
        addToBitstream(bitstream, bitstreamElement, "mix.scannerModelName", "scannerModelName");
        addToBitstream(bitstream, bitstreamElement, "mix.scanningSoftwareName", "scanningSoftwareName");*/
    }


    protected void addToBitstream(Bitstream bit, Element bitstreamElement, String metadataField, String fieldName) {
        String value = bitstreamService.getMetadata(bit, metadataField);
        if (value != null) {
            bitstreamElement.getField().add(ItemUtils.createValue(fieldName, value));
        }
    }
}
