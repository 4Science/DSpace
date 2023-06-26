/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;

/**
 * This contributor to limit the number of metadata values contributed during the parsing
 * and filtering also by not null metadata values
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class LimitedMetadataContributor<T> implements MetadataContributor<T> {

    private final MetadataContributor<T> innerContributor;
    private final int maximum;

    public LimitedMetadataContributor(MetadataContributor<T> innerContributor, int maximum) {
        this.innerContributor = innerContributor;
        this.maximum = maximum;
    }

    @Override
    public void setMetadataFieldMapping(MetadataFieldMapping<T, MetadataContributor<T>> rt) {
    }

    @Override
    public Collection<MetadatumDTO> contributeMetadata(T t) {
        final Collection<MetadatumDTO> metadata = innerContributor.contributeMetadata(t);
        return
            metadata
                .stream()
                .filter(Metadatum -> !Objects.isNull(Metadatum))
                .filter(Metadatum -> StringUtils.isNotBlank(Metadatum.getValue()))
                .limit(maximum < 0 ? metadata.size() : maximum)
                .collect(Collectors.toList());
    }
}