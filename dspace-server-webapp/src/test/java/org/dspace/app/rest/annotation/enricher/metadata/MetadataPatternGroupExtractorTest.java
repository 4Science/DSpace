/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation.enricher.metadata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.dspace.AbstractUnitTest;
import org.junit.Before;
import org.junit.Test;

public class MetadataPatternGroupExtractorTest extends AbstractUnitTest {

    static final String ITEM_PATTERN = "/iiif/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})";

    private MetadataPatternGroupExtractor extractor;

    @Before
    public void setUp() {
        extractor = new MetadataPatternGroupExtractor(ITEM_PATTERN);
    }

    @Test
    public void testExtractorInitialization() {
        assertNotNull(extractor);
    }

    @Test
    public void testGroupExtraction() {
        String extract = extractor.extract("http://localhost:8080/server/iiif/12345678-1234-1234-1234-123456789012");
        assertThat(
            extract,
            is("12345678-1234-1234-1234-123456789012")
        );
    }

    @Test
    public void testThrowExceptionWhenPatternNotFound() {
        assertThrows(IllegalArgumentException.class,
                     () -> extractor.extract("http://localhost:8080/server/iiif/12345678"));
    }
}
