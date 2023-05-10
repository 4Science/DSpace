/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dspace.importer.external.metadatamapping.MetadataFieldMapping;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link LimitedMetadataContributor}
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class LimitedMetadataContributorTest {

    @Test
    public void testWithNoNullMetadataValuesAndMaxGreaterThanZero() {

        // Given an inner contributor returning 5 not null values, and a maximum set to 2
        MetadataContributor<String> innerContributor = metadataContributorReturning(Arrays.asList(
            metadatum("dc", "first", "present", "validValue"),
            metadatum("dc", "first", "absent", "absentValue"),
            metadatum("dc", "first", "author", "authorValue"),
            metadatum("dc", "first", "editor", "editorValue"),
            metadatum("dc", "first", "title", "titleValue")
        ));

        LimitedMetadataContributor contributor = new LimitedMetadataContributor(innerContributor, 2);

        final Collection<MetadatumDTO> contributedMetadata = contributor.contributeMetadata("foo");

        Assert.assertEquals(contributedMetadata.size(), 2);

        Map<String, String> metadata = convertToMap(contributedMetadata);

        Assert.assertEquals(metadata.size(), 2);

        assertThat(metadata.get("dc.first.present"), is("validValue"));
        assertThat(metadata.get("dc.first.absent"), is("absentValue"));
    }

    @Test
    public void testWithNullMetadataValuesAndMaxGreaterThanZero() {

        // Given an inner contributor returning 2 not null values, and a maximum set to 5
        MetadataContributor<String> innerContributor = metadataContributorReturning(Arrays.asList(
            metadatum("dc", "first", "editor", null),
            metadatum("dc", "first", "present", "validValue"),
            metadatum("dc", "first", "absent", "absentValue"),
            metadatum("dc", "first", "author", ""),
            metadatum("dc", "first", "title", null)
        ));

        LimitedMetadataContributor contributor = new LimitedMetadataContributor(innerContributor, 5);

        final Collection<MetadatumDTO> contributedMetadata = contributor.contributeMetadata("foo");

        Assert.assertEquals(contributedMetadata.size(), 2);

        Map<String, String> metadata = convertToMap(contributedMetadata);

        Assert.assertEquals(metadata.size(), 2);

        assertThat(metadata.get("dc.first.present"), is("validValue"));
        assertThat(metadata.get("dc.first.absent"), is("absentValue"));
    }

    @Test
    public void testWhenMaxLessThanZero() {

        // Given an inner contributor returning 3 not null values, 2 null values , and a maximum set to -1
        MetadataContributor<String> innerContributor = metadataContributorReturning(Arrays.asList(
            metadatum("dc", "first", "present", "validValue"),
            metadatum("dc", "first", "absent", "absentValue"),
            metadatum("dc", "first", "author", "authorValue"),
            metadatum("dc", "first", "editor", null),
            metadatum("dc", "first", "title", "")
        ));

        LimitedMetadataContributor contributor = new LimitedMetadataContributor(innerContributor, -1);

        final Collection<MetadatumDTO> contributedMetadata = contributor.contributeMetadata("foo");

        Assert.assertEquals(contributedMetadata.size(), 3);

        Map<String, String> metadata = convertToMap(contributedMetadata);

        Assert.assertEquals(metadata.size(), 3);

        assertThat(metadata.get("dc.first.present"), is("validValue"));
        assertThat(metadata.get("dc.first.absent"), is("absentValue"));
        assertThat(metadata.get("dc.first.author"), is("authorValue"));
    }

    @Test
    public void testWhenAllMetadataValuesIsNull() {

        // Given an inner contributor returning 5 null values, and a maximum set to 5
        MetadataContributor<String> innerContributor = metadataContributorReturning(Arrays.asList(
            metadatum("dc", "first", "editor", null),
            metadatum("dc", "first", "present", null),
            metadatum("dc", "first", "absent", null),
            metadatum("dc", "first", "author", ""),
            metadatum("dc", "first", "title", null)
        ));

        LimitedMetadataContributor contributor = new LimitedMetadataContributor(innerContributor, 5);

        final Collection<MetadatumDTO> contributedMetadata = contributor.contributeMetadata("foo");

        Assert.assertEquals(contributedMetadata.size(), 0);

    }

    @Test
    public void testWithNoNullMetadataValuesAndMaxEqualToZero() {

        //Given an inner contributor returning 5 not null values , and a maximum set to 0
        MetadataContributor<String> innerContributor = metadataContributorReturning(Arrays.asList(
            metadatum("dc", "first", "present", "validValue"),
            metadatum("dc", "first", "absent", "absentValue"),
            metadatum("dc", "first", "author", "authorValue"),
            metadatum("dc", "first", "editor", "editorValue"),
            metadatum("dc", "first", "title", "titleValue")
        ));

        LimitedMetadataContributor contributor = new LimitedMetadataContributor(innerContributor, 0);

        final Collection<MetadatumDTO> contributedMetadata = contributor.contributeMetadata("foo");

        Assert.assertEquals(contributedMetadata.size(), 0);
    }

    @Test
    public void testWithNoMetadataFoundByInnerContributor() {

        // Given an inner contributor returning 0  values, and a maximum set to 5
        MetadataContributor<String> innerContributor = metadataContributorReturning(Arrays.asList());

        LimitedMetadataContributor contributor = new LimitedMetadataContributor(innerContributor, 5);

        final Collection<MetadatumDTO> contributedMetadata = contributor.contributeMetadata("foo");

        Assert.assertEquals(contributedMetadata.size(), 0);
    }

    private MetadatumDTO metadatum(final String schema, final String element, final String qualifier,
                                   final String value) {
        final MetadatumDTO metadatumDTO = new MetadatumDTO();
        metadatumDTO.setSchema(schema);
        metadatumDTO.setElement(element);
        metadatumDTO.setQualifier(qualifier);
        metadatumDTO.setValue(value);
        return metadatumDTO;
    }

    private MetadataContributor<String> metadataContributorReturning(final List<MetadatumDTO> contributedMetadata) {
        return new MetadataContributor<>() {
            @Override
            public void setMetadataFieldMapping(final MetadataFieldMapping<String, MetadataContributor<String>> rt) {}

            @Override
            public Collection<MetadatumDTO> contributeMetadata(final String t) {
                return contributedMetadata;
            }
        };
    }

    private Map<String, String> convertToMap(Collection<MetadatumDTO> contributedMetadata) {
        return
            contributedMetadata
            .stream()
            .collect(Collectors.toMap(dto ->
                    dto.getSchema() + "." +
                        dto.getElement() + "." +
                        dto.getQualifier(),
                MetadatumDTO::getValue));
    }
}
