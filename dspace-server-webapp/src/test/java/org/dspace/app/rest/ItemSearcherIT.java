/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.matcher.LambdaMatcher.has;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.sql.SQLException;
import java.util.function.Predicate;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ItemSearcherIT extends AbstractControllerIntegrationTest {

    private static final String ORCID_ID_1 = "0000-0001-8387-8895";

    private static final String ORCID_ID_2 = "0000-0001-0000-0000";

    private static final String ORCID_ID_3 = "0000-0002-0000-0000";

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private InstallItemService installItemService;

    @Autowired
    private ItemService itemService;

    @Test
    public void testPersonFirstThenPublication() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setDispatcher("cris-default");
        try {
            Community community = CommunityBuilder.createCommunity(context).withName("community test").build();
            Collection collection = CollectionBuilder.createCollection(context, community).withName("community test")
                .build();
            Item person = createPersonAndInstall(community, collection, ORCID_ID_1);
            Item publication = createPublicationAndInstall(community, collection, ORCID_ID_1);
            // Commit context to save data on SOLR
            context.commit();
            checkReferenceResolved(person, publication);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    @Test
    public void testMultiplePersonFirstThenMultiplePublication() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setDispatcher("cris-default");
        try {
            Community community = CommunityBuilder.createCommunity(context).withName("community test").build();
            Collection collection = CollectionBuilder.createCollection(context, community).withName("community test")
                .build();
            Item person = createPersonAndInstall(community, collection, ORCID_ID_1);
            Item person2 = createPersonAndInstall(community, collection, ORCID_ID_2);
            Item publication = createPublicationAndInstall(community, collection, ORCID_ID_1);
            Item publication2 = createPublicationAndInstall(community, collection, ORCID_ID_2);
            // Commit context to save data on SOLR
            context.commit();
            checkReferenceResolved(person, publication);
            checkReferenceResolved(person2, publication2);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    @Test
    public void testPublicationFirstThenPerson() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setDispatcher("cris-default");
        try {
            Community community = CommunityBuilder.createCommunity(context).withName("community test").build();
            Collection collection = CollectionBuilder.createCollection(context, community).withName("community test")
                .build();
            Item publication = createPublicationAndInstall(community, collection, ORCID_ID_1);
            Item person = createPersonAndInstall(community, collection, ORCID_ID_1);
            // Commit context to save data on SOLR
            context.commit();
            checkReferenceResolved(person, publication);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    @Test
    public void testMultiplePublicationFirstThenPerson() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setDispatcher("cris-default");
        try {
            Community community = CommunityBuilder.createCommunity(context).withName("community test").build();
            Collection collection = CollectionBuilder.createCollection(context, community).withName("community test")
                .build();
            Item publication = createPublicationAndInstall(community, collection, ORCID_ID_1);
            Item publication2 = createPublicationAndInstall(community, collection, ORCID_ID_1);
            Item publication3 = createPublicationAndInstall(community, collection, ORCID_ID_1);
            Item person = createPersonAndInstall(community, collection, ORCID_ID_1);
            // Commit context to save data on SOLR
            context.commit();

            checkReferenceResolved(person, publication);
            checkReferenceResolved(person, publication2);
            checkReferenceResolved(person, publication3);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    @Test
    public void testMultiplePublicationFirstThenPersonOneNotResolved() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setDispatcher("cris-default");
        try {
            Community community = CommunityBuilder.createCommunity(context).withName("community test").build();
            Collection collection = CollectionBuilder.createCollection(context, community).withName("community test")
                .build();
            Item publication = createPublicationAndInstall(community, collection, ORCID_ID_1);
            Item publication2 = createPublicationAndInstall(community, collection, ORCID_ID_1);
            Item publication3 = createPublicationAndInstall(community, collection, ORCID_ID_2);
            Item person = createPersonAndInstall(community, collection, ORCID_ID_1);
            // Commit context to save data on SOLR
            context.commit();

            checkReferenceResolved(person, publication);
            checkReferenceResolved(person, publication2);
            checkReferenceNotResolved(ORCID_ID_2, publication3);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    @Test
    public void testMultiplePublicationFirstThenMultiplePerson() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setDispatcher("cris-default");
        try {
            Community community = CommunityBuilder.createCommunity(context).withName("community test").build();
            Collection collection = CollectionBuilder.createCollection(context, community).withName("community test")
                .build();
            Item publication = createPublicationAndInstall(community, collection, ORCID_ID_1);
            Item publication2 = createPublicationAndInstall(community, collection, ORCID_ID_1);
            Item publication3 = createPublicationAndInstall(community, collection, ORCID_ID_2);
            Item person = createPersonAndInstall(community, collection, ORCID_ID_1);
            Item person2 = createPersonAndInstall(community, collection, ORCID_ID_2);
            // Commit context to save data on SOLR
            context.commit();

            checkReferenceResolved(person, publication);
            checkReferenceResolved(person, publication2);
            checkReferenceResolved(person2, publication3);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    @Test
    public void testMultiplePublicationMultiplePerson() throws Exception {
        context.turnOffAuthorisationSystem();
        context.setDispatcher("cris-default");
        try {
            Community community = CommunityBuilder.createCommunity(context).withName("community test").build();
            Collection collection = CollectionBuilder.createCollection(context, community).withName("community test")
                .build();
            Item publication = createPublicationAndInstall(community, collection, ORCID_ID_1, ORCID_ID_2, ORCID_ID_3);
            Item person = createPersonAndInstall(community, collection, ORCID_ID_1);
            Item publication2 = createPublicationAndInstall(community, collection, ORCID_ID_1, ORCID_ID_2);
            Item person2 = createPersonAndInstall(community, collection, ORCID_ID_2);
            // Commit context to save data on SOLR
            context.commit();

            checkReferenceResolved(person, publication);
            checkReferenceResolved(person2, publication);
            checkReferenceNotResolved(ORCID_ID_3, publication);

            checkReferenceResolved(person, publication2);
            checkReferenceResolved(person2, publication2);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private Item createPersonAndInstall(Community community, Collection collection, String orcid)
        throws SQLException, IOException, AuthorizeException {
        // Person item
        WorkspaceItem personWspItem = workspaceItemService.create(context, collection, true);
        Item person = personWspItem.getItem();
        itemService.addMetadata(context, person, "person", "identifier", "orcid", Item.ANY, orcid);
        itemService.setEntityType(context, person, "Person");
        // Installing person
        installItemService.installItem(context, personWspItem, null);
        itemService.update(context, person);
        return person;
    }

    private Item createPublicationAndInstall(Community community, Collection collection, String... orcids)
        throws SQLException, IOException, AuthorizeException {
        // Publication item
        WorkspaceItem publicationWspItem = workspaceItemService.create(context, collection, true);
        Item publication = publicationWspItem.getItem();
        for (String orcid : orcids) {
            itemService.addMetadata(context, publication, "dc", "contributor", "author", Item.ANY, "P-orcid-" + orcid,
                AuthorityValueService.REFERENCE + "ORCID::" + orcid, -1);
        }
        itemService.setEntityType(context, publication, "Publication");
        // Installing publication
        installItemService.installItem(context, publicationWspItem, null);
        itemService.update(context, publication);
        return publication;
    }

    private void checkReferenceResolved(Item person, Item publication) throws SQLException, Exception {
        publication = context.reloadEntity(publication);
        assertThat(publication.getMetadata(), has(authorMetadataFieldWithAuthority(person.getID().toString())));
    }

    private void checkReferenceNotResolved(String personOrcid, Item publication) throws SQLException, Exception {
        publication = context.reloadEntity(publication);
        String expectedAuthority = AuthorityValueService.REFERENCE + "ORCID::" + personOrcid;
        assertThat(publication.getMetadata(), has(authorMetadataFieldWithAuthority(expectedAuthority)));
    }

    private Predicate<MetadataValue> authorMetadataFieldWithAuthority(String authority) {
        return metadataValue -> "dc.contributor.author".equals(metadataValue.getMetadataField().toString('.'))
            && authority.equals(metadataValue.getAuthority());
    }
}