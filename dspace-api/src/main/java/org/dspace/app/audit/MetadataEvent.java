/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.content.MetadataValue;

/**
 * class that contains details about the event in the audit solr core of
 * modifying the metadata value
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class MetadataEvent {

    public static final String INITIAL_ADD = "INITIAL_ADD";
    public static final String ADD = "ADD";
    public static final String MODIFY = "MODIFY";
    public static final String REMOVE = "REMOVE";

    private String metadataField;
    private String value;
    private String authority;
    private Integer confidence;
    private int place;
    private String action;

    public MetadataEvent() {
    }

    public MetadataEvent(MetadataValue metadataValue, String action) {
        this.metadataField = metadataValue.getMetadataField().toString('_');
        this.value = metadataValue.getValue();
        this.authority = metadataValue.getAuthority();
        this.confidence = metadataValue.getConfidence();
        this.place = metadataValue.getPlace();
        this.action = action;
    }

    public String getMetadataField() {
        return metadataField;
    }

    public void setMetadataField(String metadataField) {
        this.metadataField = metadataField;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    public int getPlace() {
        return place;
    }

    public void setPlace(int place) {
        this.place = place;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
