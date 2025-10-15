/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.marcxml2item.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Implementation of {@link ItemsImportMetadataFieldReader}
 * for reading metadata values from a properties file
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 **/
public class ItemsImportPropertiesReader implements ItemsImportMetadataFieldReader {

    private static final Logger log = LoggerFactory.getLogger(ItemsImportPropertiesReader.class);

    private String readerName;
    private String defaultValue;
    private String propertiesPath;
    private Properties properties;

    @Autowired
    private ConfigurationService configurationService;

    @PostConstruct
    private void setupMapping() {
        properties = new Properties();
        try (FileInputStream fis = new FileInputStream(getPropertiesFile())) {
            properties.load(fis);
        } catch (IOException ex) {
            log.error("An error occurs reading properties file at path " + propertiesPath, ex);
        }
    }

    @Override
    public List<MetadataValueDTO> readValues(Context context, String metadataField, String type, NodeList nodeList) {
        List<MetadataValueDTO> metadataValues = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            String key = node.getTextContent();
            if (StringUtils.isBlank(key) || (StringUtils.isBlank(defaultValue) && !properties.containsKey(key))) {
                continue;
            }
            metadataValues.add(new MetadataValueDTO(metadataField, properties.getProperty(key, defaultValue)));
        }
        return metadataValues;
    }

    private File getPropertiesFile() {
        String parent = configurationService.getProperty("dspace.dir") + File.separator + "config" + File.separator;
        return new File(parent, propertiesPath);
    }

    @Override
    public String getReaderName() {
        return readerName;
    }

    public void setReaderName(String readerName) {
        this.readerName = readerName;
    }

    public String getPropertiesPath() {
        return propertiesPath;
    }

    public void setPropertiesPath(String propertiesPath) {
        this.propertiesPath = propertiesPath;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

}
