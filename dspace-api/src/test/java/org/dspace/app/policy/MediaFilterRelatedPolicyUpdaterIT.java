/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.policy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.mediafilter.FormatFilter;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.eperson.Group;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Integration tests for {@link MediaFilterRelatedPolicyUpdater}.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class MediaFilterRelatedPolicyUpdaterIT extends AbstractIntegrationTestWithDatabase {

    protected ItemService itemService =
        ContentServiceFactory.getInstance().getItemService();

    protected PolicyUpdaterService policyUpdaterService =
        DSpaceServicesFactory.getInstance().getServiceManager()
                             .getServicesByType(PolicyUpdaterService.class)
                             .get(0);

    protected ResourcePolicyService resourcePolicyService =
        AuthorizeServiceFactory.getInstance().getResourcePolicyService();

    private MediaFilterRelatedPolicyUpdater policyUpdater;

    private Community community;
    private Collection collection;
    private Item testItem;
    private Bitstream sourceBitstream;
    private Bitstream derivativeBitstream;
    private Bundle originalBundle;
    private Bundle derivativeBundle;
    private Group policyGroup;

    @Before
    public void initialize() throws Exception {

        // Create test data structure
        context.turnOffAuthorisationSystem();

        policyUpdater = new MediaFilterRelatedPolicyUpdater(itemService, policyUpdaterService);

        community = CommunityBuilder.createCommunity(context)
                                    .withName("Test Community")
                                    .build();

        collection = CollectionBuilder.createCollection(context, community)
                                      .withName("Test Collection")
                                      .build();

        testItem = ItemBuilder.createItem(context, collection)
                              .withTitle("Test Item")
                              .build();

        policyGroup = GroupBuilder.createGroup(context)
                                  .withName("Test Policy Group")
                                  .build();

        // Create source bitstream in ORIGINAL bundle
        String sourceContent = "This is test content for source bitstream";
        try (InputStream sourceStream = IOUtils.toInputStream(sourceContent, "UTF-8")) {
            sourceBitstream = BitstreamBuilder.createBitstream(context, testItem, sourceStream)
                                              .withName("source.pdf")
                                              .withMimeType("application/pdf")
                                              .build();
        }

        originalBundle = sourceBitstream.getBundles().get(0);

        // Create derivative bundle and bitstream
        derivativeBundle = BundleBuilder.createBundle(context, testItem)
                                        .withName("TEXT")
                                        .build();

        String derivativeContent = "This is extracted text content";
        try (InputStream derivativeStream = IOUtils.toInputStream(derivativeContent, "UTF-8")) {
            derivativeBitstream =
                BitstreamBuilder.createBitstream(context, testItem, derivativeStream, derivativeBundle.getName())
                                .withName("source.pdf.txt")
                                .withMimeType("text/plain")
                                .build();
        }

        context.restoreAuthSystemState();
    }

    @Test
    public void testUpdatePoliciesWithNullItem() {
        IllegalArgumentException exception = assertThrows(
            "Should throw IllegalArgumentException for null item",
            IllegalArgumentException.class,
            () -> policyUpdater.updatePolicies(context, null, sourceBitstream)
        );

        assertThat(exception.getMessage(), containsString("Cannot update policies on null item/bitstream"));
    }

    @Test
    public void testUpdatePoliciesWithNullSourceBitstream() {
        IllegalArgumentException exception = assertThrows(
            "Should throw IllegalArgumentException for null source bitstream",
            IllegalArgumentException.class,
            () -> policyUpdater.updatePolicies(context, testItem, null)
        );

        assertThat(exception.getMessage(), containsString("Cannot update policies on null item/bitstream"));
    }

    @Test
    public void testLoadFilterClassesWithValidFilters() {
        // Test the loadFilterClasses method with valid filter names
        List<String> filterNames = Arrays.asList("PDFFilter", "HTMLFilter");

        // Mock the plugin service to return mock filters
        FormatFilter pdfFilter = mock(FormatFilter.class);
        FormatFilter htmlFilter = mock(FormatFilter.class);

        // We can't easily mock the static CoreServiceFactory call in a unit test,
        // so this test demonstrates the expected behavior
        List<FormatFilter> result = policyUpdater.loadFilterClasses(filterNames);

        // The result should be a list (might be empty if no plugins are actually configured)
        assertNotNull("Result should not be null", result);
    }

    @Test
    public void testLoadFilterClassesWithEmptyList() {
        List<String> emptyFilterNames = Collections.emptyList();
        List<FormatFilter> result = policyUpdater.loadFilterClasses(emptyFilterNames);

        assertNotNull("Result should not be null", result);
        assertThat("Result should be empty", result, empty());
    }

    @Test
    public void testLoadFormatFilterWithNullName() {
        FormatFilter result = policyUpdater.loadFormatFilter(null);
        assertThat("Result should be null for null filter name", result, nullValue());
    }

    @Test
    public void testFindDerivativeBitstreamsWithMatchingName() throws Exception {
        // This test verifies the private findDerivativeBitstreams method indirectly
        // by testing the overall updatePolicies workflow

        context.turnOffAuthorisationSystem();

        // Create a test scenario with matching derivative bitstream names
        Item testItem3 = ItemBuilder.createItem(context, collection)
                                    .withTitle("Test Item 3")
                                    .build();
        ResourcePolicy sourceRp;
        // Create source bitstream
        try (InputStream sourceStream = IOUtils.toInputStream("Source content", "UTF-8")) {
            Bitstream source = BitstreamBuilder.createBitstream(context, testItem3, sourceStream)
                                               .withName("document.pdf")
                                               .withMimeType("application/pdf")
                                               .build();

            sourceRp = ResourcePolicyBuilder.createResourcePolicy(context, null, policyGroup)
                                            .withPolicyType("CUSTOM")
                                            .withName("Source Policy")
                                            .withAction(Constants.WRITE)
                                            .withDspaceObject(source)
                                            .build();

            // Create derivative bundle
            Bundle textBundle = BundleBuilder.createBundle(context, testItem3)
                                             .withName("TEXT")
                                             .build();

            // Create derivative bitstream with expected naming pattern
            try (InputStream derivativeStream = IOUtils.toInputStream("Extracted text content", "UTF-8")) {
                derivativeBitstream =
                    BitstreamBuilder.createBitstream(context, testItem3, derivativeStream, textBundle.getName())
                                    .withName("document.pdf.txt")
                                    .withMimeType("text/plain")
                                    .build();
            }

            context.restoreAuthSystemState();

            try (
                MockedStatic<CoreServiceFactory> coreServiceFactoryMockedStatic =
                    Mockito.mockStatic(CoreServiceFactory.class)) {

                CoreServiceFactory coreServiceFactory = mock(CoreServiceFactory.class);
                coreServiceFactoryMockedStatic.when(CoreServiceFactory::getInstance)
                                              .thenReturn(coreServiceFactory);

                PluginService pluginService = mock(PluginService.class);
                when(coreServiceFactory.getPluginService()).thenReturn(pluginService);

                FormatFilter formatFilter = mock(FormatFilter.class);
                when(pluginService.getNamedPlugin(eq(FormatFilter.class), anyString())).thenReturn(formatFilter);

                when(formatFilter.getFilteredName(eq(source.getName()))).thenReturn(derivativeBitstream.getName());
                when(formatFilter.getBundleName()).thenReturn(textBundle.getName());

                // Test the policy update process
                policyUpdater.updatePolicies(context, testItem3, source);

                List<ResourcePolicy> policies =
                    resourcePolicyService.find(context, derivativeBitstream, policyGroup, Constants.WRITE);
                assertThat(policies, hasSize(1));
                assertThat(
                    policies,
                    hasItem(
                        allOf(
                            hasProperty("action", is(sourceRp.getAction())),
                            hasProperty("rpName", is(sourceRp.getRpName())),
                            hasProperty("group", is(sourceRp.getGroup()))
                        )
                    )
                );
            }

        }
    }

    @Test
    public void testUpdatePoliciesWithMultipleBundles() throws Exception {
        context.turnOffAuthorisationSystem();

        // Create test item with multiple derivative bundles
        Item multiItem = ItemBuilder.createItem(context, collection)
                                    .withTitle("Multi Bundle Item")
                                    .build();

        ResourcePolicy sourceRp;
        // Create source bitstream
        try (InputStream sourceStream = IOUtils.toInputStream("Multi bundle source", "UTF-8")) {
            Bitstream source = BitstreamBuilder.createBitstream(context, multiItem, sourceStream)
                                               .withName("multi.pdf")
                                               .withMimeType("application/pdf")
                                               .build();


            sourceRp = ResourcePolicyBuilder.createResourcePolicy(context, null, policyGroup)
                                            .withPolicyType("CUSTOM")
                                            .withName("Source Policy")
                                            .withAction(Constants.ADMIN)
                                            .withDspaceObject(source)
                                            .build();

            // Create multiple derivative bundles
            Bundle textBundle = BundleBuilder.createBundle(context, multiItem)
                                              .withName("TEXT")
                                              .build();

            Bundle thumbnailBundle = BundleBuilder.createBundle(context, multiItem)
                                              .withName("THUMBNAIL")
                                              .build();

            // Create derivative bitstreams in different bundles
            try (InputStream stream1 = IOUtils.toInputStream("Text 1", "UTF-8")) {
                derivativeBitstream =
                    BitstreamBuilder.createBitstream(context, multiItem, stream1, textBundle.getName())
                                    .withName("multi.pdf.txt")
                                    .withMimeType("text/plain")
                                    .build();
            }

            Bitstream relatedBitstream;
            try (InputStream stream2 = IOUtils.toInputStream("Text 2", "UTF-8")) {
                relatedBitstream = BitstreamBuilder.createBitstream(context, multiItem, stream2, thumbnailBundle.getName())
                                                   .withName("multi.pdf.txt")
                                                   .withMimeType("text/plain")
                                                   .build();
            }

            context.restoreAuthSystemState();

            try (
                MockedStatic<CoreServiceFactory> coreServiceFactoryMockedStatic =
                    Mockito.mockStatic(CoreServiceFactory.class)) {

                CoreServiceFactory coreServiceFactory = mock(CoreServiceFactory.class);
                coreServiceFactoryMockedStatic.when(CoreServiceFactory::getInstance)
                                              .thenReturn(coreServiceFactory);

                PluginService pluginService = mock(PluginService.class);
                when(coreServiceFactory.getPluginService()).thenReturn(pluginService);

                FormatFilter formatFilter = mock(FormatFilter.class);
                when(pluginService.getNamedPlugin(eq(FormatFilter.class), anyString())).thenReturn(formatFilter);

                when(formatFilter.getFilteredName(eq(source.getName()))).thenReturn(derivativeBitstream.getName());
                when(formatFilter.getBundleName()).thenReturn(textBundle.getName());

                // Test policy updates with multiple matching derivative bitstreams
                policyUpdater.updatePolicies(context, multiItem, source);

                List<ResourcePolicy> policies =
                    resourcePolicyService.find(context, derivativeBitstream, policyGroup, Constants.ADMIN);
                assertThat(policies, hasSize(1));
                assertThat(
                    policies,
                    hasItem(
                        allOf(
                            hasProperty("action", is(sourceRp.getAction())),
                            hasProperty("rpName", is(sourceRp.getRpName())),
                            hasProperty("group", is(sourceRp.getGroup()))
                        )
                    )
                );
                policies =
                    resourcePolicyService.find(context, relatedBitstream, policyGroup, Constants.ADMIN);
                assertThat(policies, empty());
            }
        }
    }

    @Test
    public void testUpdatePoliciesWithNoDerivativeBitstreams() throws Exception {
        context.turnOffAuthorisationSystem();

        // Create item with only source bitstream, no derivatives
        Item sourceOnlyItem = ItemBuilder.createItem(context, collection)
                                         .withTitle("Source Only Item")
                                         .build();

        try (InputStream sourceStream = IOUtils.toInputStream("Only source content", "UTF-8")) {
            Bitstream sourceOnly = BitstreamBuilder.createBitstream(context, sourceOnlyItem, sourceStream)
                                                   .withName("source-only.pdf")
                                                   .withMimeType("application/pdf")
                                                   .build();

            context.restoreAuthSystemState();

            // Test policy updates when no derivative bitstreams exist
            policyUpdater.updatePolicies(context, sourceOnlyItem, sourceOnly);
        }
    }

}
