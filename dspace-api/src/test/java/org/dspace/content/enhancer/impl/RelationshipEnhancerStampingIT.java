/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;

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
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.enhancer.service.ItemEnhancerService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the relationship id stamped by the related entity enhancer on the
 * additional metadata values it generates.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class RelationshipEnhancerStampingIT extends AbstractIntegrationTestWithDatabase {

    private static final String RELATIONSHIP_CONFIG_KEY = "authority:dc.contributor.author";

    private ItemService itemService;

    private MetadataValueService metadataValueService;


    private AuthorityBackedRelationshipService authorityBackedRelationshipService;

    private ItemEnhancerService itemEnhancerService;

    private Collection collection;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        DSpace dspace = new DSpace();
        itemService = ContentServiceFactory.getInstance().getItemService();
        metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();
        authorityBackedRelationshipService = dspace.getServiceManager().getServiceByName(
            AuthorityBackedRelationshipServiceImpl.class.getCanonicalName(), AuthorityBackedRelationshipService.class);
        itemEnhancerService = dspace.getServiceManager().getServiceByName(
            "org.dspace.content.enhancer.service.ItemEnhancerService", ItemEnhancerService.class);

        ConfigurationService configurationService = dspace.getSingletonService(ConfigurationService.class);
        configurationService.setProperty("cris-consumer.skip-empty-authority", true);
        ChoiceAuthorityService choices = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
        choices.getChoiceAuthoritiesNames();
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] { "org.dspace.content.authority.ItemAuthority = AuthorAuthority" });
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");
        CoreServiceFactory.getInstance().getPluginService().clearNamedPluginClasses();
        choices.clearCache();
        ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService().clearCache();

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("community").build();
        collection = CollectionBuilder.createCollection(context, community).withName("collection").build();
        context.restoreAuthSystemState();
    }

    @Test
    public void testAdditionalsAreStampedOnIncrementalEnhancement() throws Exception {
        context.turnOffAuthorisationSystem();
        Item person = createPerson("0000-0002-1825-0097", "en");
        Item publication = createPublication(person);
        Relationship relationship = promote(publication, person);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        relationship = context.reloadEntity(relationship);

        context.turnOffAuthorisationSystem();
        itemEnhancerService.enhance(context, publication, false);
        context.restoreAuthSystemState();
        context.commit();
        publication = context.reloadEntity(publication);

        MetadataValue additional = singleVirtualValue(publication, "orcid");
        assertThat(additional.getLanguage(), equalTo("en"));
        assertThat(additional.getAuthority(), equalTo(person.getID().toString()));
        assertThat(additional.getRelationship(), notNullValue());
        assertThat(additional.getRelationship().getID(), equalTo(relationship.getID()));

        List<MetadataValue> values = metadataValueService.findByRelationship(context, relationship);
        assertThat(values, hasSize(3));
    }

    @Test
    public void testAdditionalsAreStampedAndKeepLanguageOnDeepEnhancement() throws Exception {
        context.turnOffAuthorisationSystem();
        Item person = createPerson("0000-0002-1825-0097", "en");
        Item publication = createPublication(person);
        Relationship relationship = promote(publication, person);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        relationship = context.reloadEntity(relationship);
        person = context.reloadEntity(person);

        context.turnOffAuthorisationSystem();
        itemEnhancerService.enhance(context, publication, false);
        itemService.clearMetadata(context, person, "person", "identifier", "orcid", Item.ANY);
        itemService.addMetadata(context, person, "person", "identifier", "orcid", "fr", "0000-0001-5109-3700");
        itemService.update(context, person);
        context.restoreAuthSystemState();
        context.commit();

        publication = context.reloadEntity(publication);
        person = context.reloadEntity(person);

        context.turnOffAuthorisationSystem();
        itemEnhancerService.enhance(context, publication, true);
        context.restoreAuthSystemState();
        context.commit();
        publication = context.reloadEntity(publication);

        MetadataValue additional = singleVirtualValue(publication, "orcid");
        assertThat(additional.getValue(), equalTo("0000-0001-5109-3700"));
        assertThat(additional.getLanguage(), equalTo("fr"));
        assertThat(additional.getRelationship(), notNullValue());
        assertThat(additional.getRelationship().getID(), equalTo(relationship.getID()));
    }


    private Item createPerson(String orcid, String language) throws Exception {
        Item person = ItemBuilder.createItem(context, collection)
            .withTitle("person")
            .withEntityType("Person")
            .build();
        itemService.addMetadata(context, person, "person", "identifier", "orcid", language, orcid);
        itemService.update(context, person);
        return person;
    }

    private Item createPublication(Item person) throws Exception {
        return ItemBuilder.createItem(context, collection)
            .withTitle("publication")
            .withEntityType("Publication")
            .withAuthor("Smith, John", person.getID().toString())
            .build();
    }

    private Relationship promote(Item publication, Item person) throws Exception {
        MetadataValue author = itemService
            .getMetadata(publication, "dc", "contributor", "author", Item.ANY).get(0);
        return authorityBackedRelationshipService.promoteResolvedAuthority(context, publication, author, person);
    }

    private MetadataValue singleVirtualValue(Item item, String qualifier) {
        List<MetadataValue> values = itemService.getMetadata(item, "dspace", "virtual", qualifier, Item.ANY);
        assertThat(values, hasSize(1));
        return values.get(0);
    }
}
