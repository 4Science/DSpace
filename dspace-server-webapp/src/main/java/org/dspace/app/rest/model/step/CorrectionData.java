/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

public class CorrectionData implements SectionData {
    public enum OperationType {
        ADD,
        REMOVE,
        MODIFY
    }

    private List<CorrectionBitstream> bitstream;
    private List<CorrectionMetadata> metadata;


    private CorrectionData() {
        this.metadata = new ArrayList<>();
        this.bitstream = new ArrayList<>();
    }

    public static CorrectionData newCorrection() {
        return new CorrectionData();
    }

    public CorrectionBitstream newBitstream() {
        CorrectionBitstream bitStream = new CorrectionBitstream();
        this.bitstream.add(bitStream);
        return bitStream;
    }

    public CorrectionData addBitstream(CorrectionBitstream bitStream) {
        if (bitStream != null) {
            this.bitstream.add(bitStream);
        }
        return this;
    }

    public CorrectionData addAllBitstream(Collection<CorrectionBitstream> bitStream) {
        if (CollectionUtils.isNotEmpty(bitStream)) {
            this.bitstream.addAll(bitStream);
        }
        return this;
    }

    public CorrectionData addMetadata(String metadataName,
                                      Set<String> newValues,
                                      Set<String> oldValues,
                                      String label) {
        this.metadata.add(new CorrectionMetadata(metadataName, newValues, oldValues, label));
        return this;
    }

    public List<CorrectionBitstream> getBitstream() {
        return bitstream;
    }

    public void setbitStream(List<CorrectionBitstream> bitStream) {
        this.bitstream = bitStream;
    }

    public List<CorrectionMetadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<CorrectionMetadata> metadata) {
        this.metadata = metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CorrectionData that = (CorrectionData) o;
        return Objects.equals(bitstream, that.bitstream) &&
            Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bitstream, metadata);
    }

    public boolean isEmpty() {
        return CollectionUtils.isEmpty(this.bitstream) && CollectionUtils.isEmpty(this.metadata);
    }

}
