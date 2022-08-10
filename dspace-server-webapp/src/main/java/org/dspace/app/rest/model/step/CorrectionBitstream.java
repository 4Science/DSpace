/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class CorrectionBitstream {
    private String filename;
    private CorrectionData.OperationType operationType;
    private List<CorrectionMetadata> metadata;
    private List<CorrectionPolicy> policies;

    public CorrectionBitstream() {
        this.policies = new ArrayList<>();
        this.metadata = new ArrayList<>();
    }

    public CorrectionBitstream addOperationType(CorrectionData.OperationType operationType) {
        this.operationType = operationType;
        return this;
    }


    public CorrectionBitstream addMetadata(String metadataName,
                                           Set<String> newValues,
                                           Set<String> oldValues,
                                           String label) {
        this.metadata.add(new CorrectionMetadata(metadataName, newValues, oldValues, label));
        return this;
    }

    public CorrectionBitstream addPolicy(String newValue,
                                         String oldValue,
                                         String label) {
        this.policies.add(new CorrectionPolicy(newValue, oldValue, label));
        return this;
    }

    public CorrectionBitstream addPolicy(CorrectionPolicy policy) {
        if (policy != null) {
            this.policies.add(policy);
        }
        return this;
    }


    public CorrectionBitstream addFilename(String filename) {
        if (StringUtils.isNotBlank(filename)) {
            this.filename = filename;
        }
        return this;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public CorrectionData.OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(CorrectionData.OperationType operationType) {
        this.operationType = operationType;
    }


    public List<CorrectionMetadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<CorrectionMetadata> metadata) {
        this.metadata = metadata;
    }

    public List<CorrectionPolicy> getPolicies() {
        return policies;
    }

    public void setPolicies(List<CorrectionPolicy> policies) {
        this.policies = policies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CorrectionBitstream that = (CorrectionBitstream) o;
        return Objects.equals(filename, that.filename) &&
            operationType == that.operationType &&
            Objects.deepEquals(metadata, that.metadata) &&
            Objects.deepEquals(policies, that.policies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, operationType, metadata, policies);
    }
}
