/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.dspace.AbstractUnitTest;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test class for FacetYearRange
 */
public class FacetYearRangeTest extends AbstractUnitTest {

    @Test
    public void testCalculateEqualYearRanges() {
        // Create a mock DiscoverySearchFilterFacet
        DiscoverySearchFilterFacet facet = Mockito.mock(DiscoverySearchFilterFacet.class);

        // Create FacetYearRange instance
        FacetYearRange facetYearRange = new FacetYearRange(facet);

        // Test with the example range: 1900-2050, 5 subsets
        List<int[]> ranges = facetYearRange.calculateEqualYearRanges(1900, 2050, 5);

        // Verify we have 5 ranges
        assertEquals(5, ranges.size());

        // Verify each range
        // Expected: [1900-1929], [1930-1959], [1960-1989], [1990-2019], [2020-2050]
        verifyRange(ranges.get(0), 1900, 1929);
        verifyRange(ranges.get(1), 1930, 1959);
        verifyRange(ranges.get(2), 1960, 1989);
        verifyRange(ranges.get(3), 1990, 2019);
        verifyRange(ranges.get(4), 2020, 2050);

    }

    @Test
    public void testEightYearsSubset() {
        // Create a mock DiscoverySearchFilterFacet
        DiscoverySearchFilterFacet facet = Mockito.mock(DiscoverySearchFilterFacet.class);

        // Create FacetYearRange instance
        FacetYearRange facetYearRange = new FacetYearRange(facet);
        // Test with another example: 2000-2023, 3 subsets
        List<int[]> ranges = facetYearRange.calculateEqualYearRanges(2000, 2023, 3);

        // Verify we have 3 ranges
        assertEquals(3, ranges.size());

        // Verify each range (24 years total, so each range should be 8 years)
        verifyRange(ranges.get(0), 2000, 2007);
        verifyRange(ranges.get(1), 2008, 2015);
        verifyRange(ranges.get(2), 2016, 2023);
    }

    @Test
    public void testFiveYearsSubset() {
        // Create a mock DiscoverySearchFilterFacet
        DiscoverySearchFilterFacet facet = Mockito.mock(DiscoverySearchFilterFacet.class);

        // Create FacetYearRange instance
        FacetYearRange facetYearRange = new FacetYearRange(facet);
        // Test with another example: 2000-2005, 3 subsets
        List<int[]> ranges = facetYearRange.calculateEqualYearRanges(2000, 2005, 3);

        // Verify we have 3 ranges
        assertEquals(3, ranges.size());

        // Verify each range (24 years total, so each range should be 8 years)
        verifyRange(ranges.get(0), 2000, 2001);
        verifyRange(ranges.get(1), 2002, 2003);
        verifyRange(ranges.get(2), 2004, 2005);
    }

    @Test
    public void testFourYearsSubsets() {
        // Create a mock DiscoverySearchFilterFacet
        DiscoverySearchFilterFacet facet = Mockito.mock(DiscoverySearchFilterFacet.class);

        // Create FacetYearRange instance
        FacetYearRange facetYearRange = new FacetYearRange(facet);
        // Test with another example: 2000-2004, 3 subsets
        List<int[]> ranges = facetYearRange.calculateEqualYearRanges(2000, 2004, 3);

        // Verify we have 3 ranges
        assertEquals(3, ranges.size());

        // Verify each range (4 years total, so each range should be 1~2 years)
        verifyRange(ranges.get(0), 2000, 2000);
        verifyRange(ranges.get(1), 2001, 2002);
        verifyRange(ranges.get(2), 2003, 2004);
    }

    @Test
    public void testThreeYearsSubset() {
        // Create a mock DiscoverySearchFilterFacet
        DiscoverySearchFilterFacet facet = Mockito.mock(DiscoverySearchFilterFacet.class);

        // Create FacetYearRange instance
        FacetYearRange facetYearRange = new FacetYearRange(facet);
        // Test with another example: 2000-2005, 3 subsets
        List<int[]> ranges = facetYearRange.calculateEqualYearRanges(2000, 2003, 3);

        // Verify we have 3 ranges
        assertEquals(3, ranges.size());

        // Verify each range (3 years total, so each range should be 1 years)
        verifyRange(ranges.get(0), 2000, 2000);
        verifyRange(ranges.get(1), 2000, 2001);
        verifyRange(ranges.get(2), 2002, 2003);
    }

    @Test
    public void testTwoYearsSubsets() {
        // Create a mock DiscoverySearchFilterFacet
        DiscoverySearchFilterFacet facet = Mockito.mock(DiscoverySearchFilterFacet.class);

        // Create FacetYearRange instance
        FacetYearRange facetYearRange = new FacetYearRange(facet);
        // Test with another example: 2000-2005, 3 subsets
        List<int[]> ranges = facetYearRange.calculateEqualYearRanges(2000, 2002, 3);

        // Verify we have 3 ranges
        assertEquals(3, ranges.size());

        // Verify each range (2 years total, so each range should be 1 years)
        verifyRange(ranges.get(0), 2000, 2000);
        verifyRange(ranges.get(1), 2000, 2000);
        verifyRange(ranges.get(2), 2001, 2002);
    }

    public void verifyRange(int[] range, int expectedStart, int expectedEnd) {
        assertEquals("Start year should match", expectedStart, range[0]);
        assertEquals("End year should match", expectedEnd, range[1]);
    }
}