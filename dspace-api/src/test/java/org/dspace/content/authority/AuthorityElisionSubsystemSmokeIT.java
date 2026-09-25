/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.AuthorityBackedRelationshipService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Subsystem smoke test for authority elision (substep 3 / D23): once a resolved internal
 * reference has minted its relationship row and elided the raw authority column, downstream
 * read subsystems that consume {@link MetadataValue#getAuthority()} must still see the related
 * item's UUID even though the column is physically null.
 *
 * This exercises the CSV export round-trip and Solr {@code author_authority} indexing (two
 * representative readers that funnel through the public authority getter) plus the derived getter
 * itself, mirroring how the REST teardown IT observes elision indirectly rather than asserting
 * Hibernate internals. Security-by-authority reads the same {@link MetadataValue#getAuthority()}
 * seam, so it is covered transitively by the derived-getter contract these readers exercise.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class AuthorityElisionSubsystemSmokeIT extends AbstractIntegrationTestWithDatabase {

    private AuthorityBackedRelationshipService authorityBackedRelationshipService;
    private ItemService itemService;
    private SearchService searchService;

    private final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();
    private final MetadataAuthorityService metadataAuthorityService =
        ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();
    private final IndexingService indexingService = DSpaceServicesFactory.getInstance().getServiceManager()
        .getServiceByName(IndexingService.class.getName(), IndexingService.class);

    private Collection collection;
    private Item owner;
    private Item target;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        authorityBackedRelationshipService = new DSpace().getServiceManager()
            .getServiceByName(AuthorityBackedRelationshipServiceImpl.class.getCanonicalName(),
                AuthorityBackedRelationshipService.class);
        itemService = ContentServiceFactory.getInstance().getItemService();
        searchService = SearchUtils.getSearchService();

        // author_authority is only indexed when dc.contributor.author is authority controlled.
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
            .withName("community")
            .build();

        collection = CollectionBuilder.createCollection(context, community)
            .withName("collection")
            .build();

        owner = ItemBuilder.createItem(context, collection)
            .withTitle("owner publication")
            .withAuthor("Smith, John")
            .build();

        target = ItemBuilder.createItem(context, collection)
            .withTitle("target person")
            .build();

        context.restoreAuthSystemState();
    }

    @Override
    public void destroy() throws Exception {
        configurationService.setProperty("authority.controlled.dc.contributor.author", null);
        metadataAuthorityService.clearCache();
        super.destroy();
    }

    @Test
    public void testCsvExportSurfacesTheElidedUuidThroughThePublicGetter() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataValue authorValue = itemService.getMetadata(owner, "dc", "contributor", "author", Item.ANY).get(0);
        authorityBackedRelationshipService.markRelationshipForResolvedAuthority(context, owner, authorValue, target);
        itemService.update(context, owner);
        context.commit();

        // the raw column is elided...
        context.uncacheEntity(owner);
        owner = context.reloadEntity(owner);
        MetadataValue reloaded = itemService.getMetadata(owner, "dc", "contributor", "author", Item.ANY).get(0);
        assertThat(reloaded.getRawAuthority(), nullValue());
        assertThat(reloaded.getAuthority(), equalTo(target.getID().toString()));

        // ...yet the CSV export (a reader that funnels through getAuthority()) still emits the UUID
        DSpaceCSV csv = new DSpaceCSV(false);
        csv.addItem(owner);
        String exported = csv.toString();

        context.restoreAuthSystemState();

        assertThat(exported, containsString(target.getID().toString()));
    }

    @Test
    public void testDerivedGetterIsStableAcrossAReloadFromTheDatabase() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataValue authorValue = itemService.getMetadata(owner, "dc", "contributor", "author", Item.ANY).get(0);
        authorityBackedRelationshipService.markRelationshipForResolvedAuthority(context, owner, authorValue, target);
        itemService.update(context, owner);
        context.commit();

        // evict and reload straight from the DB: proves the null column persisted
        context.uncacheEntity(owner);
        owner = context.reloadEntity(owner);

        List<MetadataValue> authors =
            itemService.getMetadata(owner, "dc", "contributor", "author", Item.ANY);
        assertThat(authors, hasSize(1));
        assertThat(authors.get(0).getRawAuthority(), nullValue());
        assertThat(authors.get(0).getAuthority(), equalTo(target.getID().toString()));

        context.restoreAuthSystemState();
    }

    @Test
    public void testSolrIndexingSurfacesTheElidedUuidAsAuthorAuthority() throws Exception {
        context.turnOffAuthorisationSystem();

        // A resolved internal reference: the author names the target by UUID with accepted
        // confidence, so the update-loop reconcile mints the row and elides the raw column.
        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("indexed publication")
            .withAuthor("Smith, John", target.getID().toString(), Choices.CF_ACCEPTED)
            .build();
        context.commit();
        indexingService.commit();

        MetadataValue authorValue =
            itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY).get(0);
        assertThat(authorValue.getRawAuthority(), nullValue());
        assertThat(authorValue.getAuthority(), equalTo(target.getID().toString()));

        context.restoreAuthSystemState();

        // The discovery index factory populates author_authority from getAuthority(); with the raw
        // column elided the derived UUID must still drive the person->publications listing.
        DiscoverQuery query = new DiscoverQuery();
        query.addFilterQueries("search.resourcetype:" + IndexableItem.TYPE);
        query.addFilterQueries("author_authority:" + target.getID().toString());
        DiscoverResult result = searchService.search(context, query);

        List<String> foundIds = result.getIndexableObjects().stream()
            .map(IndexableObject::getID)
            .map(Object::toString)
            .collect(Collectors.toList());
        assertThat(foundIds, hasItem(publication.getID().toString()));
    }
}
