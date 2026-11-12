/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.enhancer;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.sql.SQLException;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.matcher.CustomItemMatcher;
import org.dspace.app.matcher.MetadataValueMatcher;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.authority.AuthorityBackedRelationshipServiceImpl;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.AuthorityBackedRelationshipService;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.enhancer.service.ItemEnhancerService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ReloadableEntity;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Integration tests for the {@link RelatedItemEnhancerUpdatePoller} and its interaction with the
 * relationship-backed related entity enhancer.
 *
 * <p>The tests verify that changing a {@code Person} item enqueues the items whose metadata is
 * linked to that person through configuration-backed relationships, that the poller re-enhances
 * those dependents, and that the updated item itself is never re-queued. Derived additional
 * metadata values are written under their configured target field (for example
 * {@code dspace.virtual.orcid}), keyed by the relationship of their hop and carrying the
 * authority of the primary value; the removed {@code dspace.virtualsource.*} fields are not used.
 */
public class RelatedItemEnhancerPollerIT extends AbstractIntegrationTestWithDatabase {

    private ItemService itemService;
    private ItemEnhancerService itemEnhancerService;
    private ItemEnhancerService spyItemEnhancerService;
    private AuthorityBackedRelationshipService authorityBackedRelationshipService;
    private RelatedItemEnhancerUpdatePoller poller = new RelatedItemEnhancerUpdatePoller();
    private Collection collection;

    @Before
    public void setup() throws Exception {
        final DSpace dspace = new DSpace();
        ConfigurationService configurationService = dspace.getConfigurationService();
        configurationService.setProperty("relationship.enable-virtual-metadata", false);
        configurationService.setProperty("cris-consumer.skip-empty-authority", true);
        itemService = ContentServiceFactory.getInstance().getItemService();
        itemEnhancerService = dspace.getSingletonService(ItemEnhancerService.class);
        authorityBackedRelationshipService = dspace.getServiceManager().getServiceByName(
            AuthorityBackedRelationshipServiceImpl.class.getCanonicalName(),
            AuthorityBackedRelationshipService.class);
        spyItemEnhancerService = spy(itemEnhancerService);
        poller.setItemEnhancerService(spyItemEnhancerService);
        poller.setItemService(itemService);
        // cleanup the queue from any items left behind by other tests
        poller.pollItemToUpdateAndProcess();
        PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();
        ChoiceAuthorityService choiceAuthorityService = ContentAuthorityServiceFactory
            .getInstance().getChoiceAuthorityService();
        choiceAuthorityService.getChoiceAuthoritiesNames(); // initialize the ChoiceAuthorityService
        MetadataAuthorityService metadataAuthorityService = ContentAuthorityServiceFactory
            .getInstance().getMetadataAuthorityService();
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] { "org.dspace.content.authority.ItemAuthority = AuthorAuthority" });
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        configurationService.setProperty("choices.presentation.dc.contributor.author", "suggest");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .build();
        context.restoreAuthSystemState();
    }

    /**
     * Verify that changing a Person re-enhances the Publication linked to it through a
     * configuration-backed relationship, and that the enhanced values are stamped with that
     * relationship and the person authority.
     */
    @Test
    public void testPersonUpdateReEnhancesRelatedPublication() throws Exception {
        context.turnOffAuthorisationSystem();
        Item person = createPerson("Walter White", null, "4Science");
        Item publication = createPublication("Test publication", "Walter White", person);
        context.restoreAuthSystemState();
        context.commit();

        person = context.reloadEntity(person);
        publication = context.reloadEntity(publication);

        assertThat(itemService.getMetadataByMetadataString(publication, "dspace.virtual.department"),
            containsInAnyOrder(derived("dspace.virtual.department", "4Science", person)));
        assertThat(itemService.getMetadataByMetadataString(publication, "dspace.virtual.orcid"), empty());
        assertDerivedValuesUsePrimaryRelationship(publication, person);
        assertNoVirtualSourceMetadata(publication);

        // drain anything queued while building the fixtures
        poller.pollItemToUpdateAndProcess();
        Mockito.reset(spyItemEnhancerService);

        context.turnOffAuthorisationSystem();
        person = context.reloadEntity(person);
        itemService.addMetadata(context, person, "person", "identifier", "orcid", null, "1234-5678-9101");
        itemService.addMetadata(context, person, "person", "affiliation", "name", null, "Company");
        itemService.update(context, person);
        context.restoreAuthSystemState();
        context.commit();
        Mockito.reset(spyItemEnhancerService);

        poller.pollItemToUpdateAndProcess();

        verify(spyItemEnhancerService).enhance(any(), argThat(new CustomItemMatcher(publication.getID())), eq(true));
        verify(spyItemEnhancerService, never())
            .enhance(any(), argThat(new CustomItemMatcher(person.getID())), eq(true));
        // the publication is queued once, the second poll returns null
        verify(spyItemEnhancerService, times(2)).pollItemToUpdate(any());
        verify(spyItemEnhancerService).saveAffectedItemsForUpdate(any(), eq(publication.getID()));
        verifyNoMoreInteractions(spyItemEnhancerService);

        person = context.reloadEntity(person);
        publication = context.reloadEntity(publication);

        assertThat(itemService.getMetadataByMetadataString(publication, "dspace.virtual.department"),
            containsInAnyOrder(
                derived("dspace.virtual.department", "4Science", person),
                derived("dspace.virtual.department", "Company", person)));
        assertThat(itemService.getMetadataByMetadataString(publication, "dspace.virtual.orcid"),
            containsInAnyOrder(derived("dspace.virtual.orcid", "1234-5678-9101", person)));
        assertDerivedValuesUsePrimaryRelationship(publication, person);
        assertNoVirtualSourceMetadata(publication);
    }

    /**
     * Verify that every Publication related to the changed Person is queued, that multi-author
     * publications keep one derived block per relationship, and that the changed Person itself is
     * never queued or enhanced by the poller.
     */
    @Test
    public void testPersonUpdateQueuesAllRelatedPublicationsButNotThePerson() throws Exception {
        context.turnOffAuthorisationSystem();
        Item john = createPerson("John Red", null);
        Item marc = createPerson("Marc Green", "orcid-person3", "Affiliation 1", "Affiliation 2");
        Item publication = createPublication("Test publication", "John Red", john);
        Item publication2 = createPublication("Test publication 2", "John Red", john);
        addAuthor(publication2, "Marc Green", marc);
        context.restoreAuthSystemState();
        context.commit();

        poller.pollItemToUpdateAndProcess();
        Mockito.reset(spyItemEnhancerService);

        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, john, "person", "affiliation", "name", null, "Company");
        itemService.update(context, john);
        context.restoreAuthSystemState();
        john = commitAndReload(john);
        Mockito.reset(spyItemEnhancerService);

        poller.pollItemToUpdateAndProcess();

        verify(spyItemEnhancerService).enhance(any(), argThat(new CustomItemMatcher(publication.getID())), eq(true));
        verify(spyItemEnhancerService).enhance(any(), argThat(new CustomItemMatcher(publication2.getID())), eq(true));
        verify(spyItemEnhancerService, never())
            .enhance(any(), argThat(new CustomItemMatcher(john.getID())), eq(true));
        verify(spyItemEnhancerService, never())
            .enhance(any(), argThat(new CustomItemMatcher(marc.getID())), eq(true));
        // two publications are queued, the third poll returns null
        verify(spyItemEnhancerService, times(3)).pollItemToUpdate(any());
        verify(spyItemEnhancerService).saveAffectedItemsForUpdate(any(), eq(publication.getID()));
        verify(spyItemEnhancerService).saveAffectedItemsForUpdate(any(), eq(publication2.getID()));
        verifyNoMoreInteractions(spyItemEnhancerService);

        publication = context.reloadEntity(publication);
        publication2 = context.reloadEntity(publication2);

        assertThat(itemService.getMetadataByMetadataString(publication, "dspace.virtual.department"),
            containsInAnyOrder(derived("dspace.virtual.department", "Company", john)));
        assertThat(itemService.getMetadataByMetadataString(publication, "dspace.virtual.orcid"), empty());
        assertThat(itemService.getMetadataByMetadataString(publication2, "dspace.virtual.department"),
            containsInAnyOrder(
                derived("dspace.virtual.department", "Company", john),
                derived("dspace.virtual.department", "Affiliation 1", marc),
                derived("dspace.virtual.department", "Affiliation 2", marc)));
        assertThat(itemService.getMetadataByMetadataString(publication2, "dspace.virtual.orcid"),
            containsInAnyOrder(derived("dspace.virtual.orcid", "orcid-person3", marc)));
        assertNoVirtualSourceMetadata(publication);
        assertNoVirtualSourceMetadata(publication2);
    }

    private Item createPerson(String title, String orcid, String... affiliations) throws Exception {
        ItemBuilder builder = ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .withEntityType("Person");
        if (orcid != null) {
            builder = builder.withOrcidIdentifier(orcid);
        }
        for (String affiliation : affiliations) {
            builder = builder.withPersonMainAffiliation(affiliation);
        }
        return builder.build();
    }

    private Item createPublication(String title, String authorName, Item person) throws Exception {
        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .withEntityType("Publication")
            .build();
        addAuthor(publication, authorName, person);
        return publication;
    }

    private void addAuthor(Item publication, String authorName, Item person) throws Exception {
        MetadataValue author = itemService.addMetadata(context, publication, "dc", "contributor", "author",
            null, authorName, person.getID().toString(), Choices.CF_ACCEPTED);
        authorityBackedRelationshipService.promoteResolvedAuthority(context, publication, author, person);
    }

    private void assertDerivedValuesUsePrimaryRelationship(Item item, Item relatedItem) {
        MetadataValue primary = itemService.getMetadata(item, "dc", "contributor", "author", Item.ANY).get(0);
        Relationship relationship = primary.getRelationship();
        assertThat(relationship, notNullValue());
        List<MetadataValue> derivedValues = itemService.getMetadata(item, "dspace", "virtual", Item.ANY, Item.ANY);
        assertThat(derivedValues.isEmpty(), equalTo(false));
        for (MetadataValue derivedValue : derivedValues) {
            assertThat(derivedValue.getRelationship(), notNullValue());
            assertThat(derivedValue.getRelationship().getID(), equalTo(relationship.getID()));
            assertThat(derivedValue.getAuthority(), equalTo(relatedItem.getID().toString()));
        }
    }

    private MetadataValueMatcher derived(String field, String value, Item person) {
        return with(field, value, null, person.getID().toString(), null, -1);
    }

    private void assertNoVirtualSourceMetadata(Item item) {
        long virtualSourceValues = item.getMetadata().stream()
            .map(value -> value.getMetadataField().toString('.'))
            .filter(field -> field.startsWith("dspace.virtualsource."))
            .count();
        assertThat(virtualSourceValues, equalTo(0L));
    }

    @SuppressWarnings("rawtypes")
    private <T extends ReloadableEntity> T commitAndReload(T entity) throws SQLException, AuthorizeException {
        context.commit();
        return context.reloadEntity(entity);
    }

}
