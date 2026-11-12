/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.authority.AuthorityBackedRelationshipServiceImpl;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.AuthorityBackedRelationshipService;
import org.dspace.content.enhancer.service.ItemEnhancerService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the re-keying of the related entity enhancer onto the relationship id
 * and the relationship-join based fan-out of the update poller.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class RelationshipEnhancerRekeyIT extends AbstractIntegrationTestWithDatabase {

    private ItemService itemService;

    private AuthorityBackedRelationshipService authorityBackedRelationshipService;

    private ItemEnhancerService itemEnhancerService;

    private Collection collection;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        DSpace dspace = new DSpace();
        itemService = ContentServiceFactory.getInstance().getItemService();
        authorityBackedRelationshipService = dspace.getServiceManager().getServiceByName(
            AuthorityBackedRelationshipServiceImpl.class.getCanonicalName(), AuthorityBackedRelationshipService.class);
        itemEnhancerService = dspace.getServiceManager().getServiceByName(
            "org.dspace.content.enhancer.service.ItemEnhancerService", ItemEnhancerService.class);

        ConfigurationService configurationService = dspace.getSingletonService(ConfigurationService.class);
        configurationService.setProperty("cris-consumer.skip-empty-authority", true);
        ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService().getChoiceAuthoritiesNames();
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] { "org.dspace.content.authority.ItemAuthority = AuthorAuthority" });
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("choices.plugin.dc.contributor.editor", "AuthorAuthority");
        configurationService.setProperty("authority.controlled.dc.contributor.editor", "true");
        configurationService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");
        CoreServiceFactory.getInstance().getPluginService().clearNamedPluginClasses();
        ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService().clearCache();
        ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService().clearCache();

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("community").build();
        collection = CollectionBuilder.createCollection(context, community).withName("collection").build();
        context.restoreAuthSystemState();
    }

    @Test
    public void testEnhancementIsIdempotentAndDoesNotWriteVirtualSource() throws Exception {
        context.turnOffAuthorisationSystem();
        Item person = createPerson("0000-0002-1825-0097", "4Science");
        Item publication = createPublication(person, null);
        Relationship relationship = promote(publication, person, "author");
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);

        context.turnOffAuthorisationSystem();
        itemEnhancerService.enhance(context, publication, false);
        boolean changedOnSecondRun = itemEnhancerService.enhance(context, publication, false);
        context.restoreAuthSystemState();
        assertThat(changedOnSecondRun, equalTo(false));
        context.commit();
        publication = context.reloadEntity(publication);

        List<MetadataValue> orcidValues = itemService.getMetadata(publication, "dspace", "virtual", "orcid", Item.ANY);
        assertThat(orcidValues, hasSize(1));
        assertThat(orcidValues.get(0).getRelationship(), notNullValue());
        assertThat(orcidValues.get(0).getRelationship().getID(), equalTo(relationship.getID()));

        List<MetadataValue> departmentValues =
            itemService.getMetadata(publication, "dspace", "virtual", "department", Item.ANY);
        assertThat(departmentValues, hasSize(1));

        assertThat(itemService.getMetadataByMetadataString(publication, "dspace.virtualsource.orcid"), empty());
        assertThat(itemService.getMetadataByMetadataString(publication, "dspace.virtualsource.department"), empty());
    }

    @Test
    public void testObsoleteAdditionalIsReplacedWhenRelatedValueChanges() throws Exception {
        context.turnOffAuthorisationSystem();
        Item person = createPerson("0000-0002-1825-0097", "4Science");
        Item publication = createPublication(person, null);
        Relationship relationship = promote(publication, person, "author");
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        person = context.reloadEntity(person);

        context.turnOffAuthorisationSystem();
        itemEnhancerService.enhance(context, publication, false);
        itemService.clearMetadata(context, person, "person", "identifier", "orcid", Item.ANY);
        itemService.update(context, person);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);

        context.turnOffAuthorisationSystem();
        itemEnhancerService.enhance(context, publication, false);
        context.restoreAuthSystemState();
        context.commit();
        publication = context.reloadEntity(publication);

        List<MetadataValue> orcidValues = itemService.getMetadata(publication, "dspace", "virtual", "orcid", Item.ANY);
        assertThat(orcidValues, hasSize(1));
        assertThat(orcidValues.stream().noneMatch(value -> "0000-0002-1825-0097".equals(value.getValue())),
            equalTo(true));
        assertThat(orcidValues.get(0).getRelationship().getID(), equalTo(relationship.getID()));
    }

    @Test
    public void testSamePersonAsAuthorAndEditorYieldsTwoOccurrences() throws Exception {
        context.turnOffAuthorisationSystem();
        Item person = createPerson("0000-0002-1825-0097", "4Science");
        Item publication = createPublication(person, person);
        Relationship authorship = promote(publication, person, "author");
        Relationship editorship = promote(publication, person, "editor");
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);

        context.turnOffAuthorisationSystem();
        itemEnhancerService.enhance(context, publication, false);
        context.restoreAuthSystemState();
        context.commit();
        publication = context.reloadEntity(publication);

        List<MetadataValue> orcidValues = itemService.getMetadata(publication, "dspace", "virtual", "orcid", Item.ANY);
        assertThat(orcidValues, hasSize(2));
        assertThat(orcidValues.stream().map(value -> value.getRelationship().getID()).toList(),
            contains(authorship.getID(), editorship.getID()));
    }

    @Test
    public void testPollerFanOutUsesRelationshipJoinAndExcludesChangedItem() throws Exception {
        context.turnOffAuthorisationSystem();
        Item person = createPerson("0000-0002-1825-0097", "4Science");
        Item publication = createPublication(person, null);
        promote(publication, person, "author");
        context.restoreAuthSystemState();
        context.commit();

        UUID publicationId = publication.getID();
        UUID personId = person.getID();

        drainEnhancementQueue();

        itemEnhancerService.saveAffectedItemsForUpdate(context, personId);

        List<UUID> queued = drainEnhancementQueue();
        assertThat(queued, contains(publicationId));
        assertThat(queued, not(contains(personId)));
    }

    private List<UUID> drainEnhancementQueue() throws Exception {
        List<UUID> queued = new ArrayList<>();
        UUID next = itemEnhancerService.pollItemToUpdate(context);
        while (next != null) {
            queued.add(next);
            next = itemEnhancerService.pollItemToUpdate(context);
        }
        return queued;
    }

    private Item createPerson(String orcid, String affiliation) throws Exception {
        Item person = ItemBuilder.createItem(context, collection)
            .withTitle("person")
            .withEntityType("Person")
            .build();
        itemService.addMetadata(context, person, "person", "identifier", "orcid", "en", orcid);
        itemService.addMetadata(context, person, "person", "affiliation", "name", null, affiliation);
        itemService.update(context, person);
        return person;
    }

    private Item createPublication(Item author, Item editor) throws Exception {
        ItemBuilder builder = ItemBuilder.createItem(context, collection)
            .withTitle("publication")
            .withEntityType("Publication")
            .withAuthor("Smith, John", author.getID().toString());
        if (editor != null) {
            builder.withEditor("Smith, John", editor.getID().toString());
        }
        return builder.build();
    }

    private Relationship promote(Item publication, Item person, String qualifier) throws Exception {
        MetadataValue value = itemService.getMetadata(publication, "dc", "contributor", qualifier, Item.ANY).get(0);
        return authorityBackedRelationshipService.promoteResolvedAuthority(context, publication, value, person);
    }
}
