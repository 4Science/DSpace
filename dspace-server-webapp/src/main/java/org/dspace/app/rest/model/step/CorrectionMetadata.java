/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

public class CorrectionMetadata {
    private String metadata;
    private Set<String> newValues;
    private Set<String> oldValues;
    private String label;

    public CorrectionMetadata(String attributeName, Set<String> newValues, Set<String> oldValues, String label) {
        this.metadata = attributeName;
        this.newValues = newValues != null ? newValues : new HashSet<>();
        this.oldValues = oldValues != null ? oldValues : new HashSet<>();
        this.label = label;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Set<String> getNewValues() {
        return newValues;
    }

    public void setNewValues(Set<String> newValues) {
        this.newValues = newValues;
    }

    public Set<String> getOldValues() {
        return oldValues;
    }

    public void setOldValues(Set<String> oldValues) {
        this.oldValues = oldValues;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CorrectionMetadata that = (CorrectionMetadata) o;
        return Objects.equals(metadata, that.metadata) &&
            CollectionUtils.isEqualCollection(newValues, that.newValues) &&
            CollectionUtils.isEqualCollection(oldValues, that.oldValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata, newValues, oldValues);
    }
}
