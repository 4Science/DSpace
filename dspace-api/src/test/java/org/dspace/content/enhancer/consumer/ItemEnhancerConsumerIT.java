/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.consumer;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import java.sql.SQLException;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ReloadableEntity;
import org.junit.Before;
import org.junit.Test;

public class ItemEnhancerConsumerIT extends AbstractIntegrationTestWithDatabase {

    private ItemService itemService;

    private Collection collection;

    @Before
    public void setup() {

        itemService = ContentServiceFactory.getInstance().getItemService();

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .build();
        context.restoreAuthSystemState();

    }

    @Test
    public void testSingleMetadataValueEnhancement() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withPersonMainAffiliation("4Science")
            .build();

        String personId = person.getID().toString();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .withAuthor("Walter White", personId)
            .build();

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        List<MetadataValue> metadataValues = publication.getMetadata();
        assertThat(metadataValues, hasSize(9));
        assertThat(metadataValues, hasItem(with("cris.virtual.department", "4Science")));
        assertThat(metadataValues, hasItem(with("cris.virtualsource.department", personId)));

        MetadataValue virtualField = getFirstMetadataValue(publication, "cris.virtual.department");
        MetadataValue virtualSourceField = getFirstMetadataValue(publication, "cris.virtualsource.department");

        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, publication, "dc", "subject", null, null, "Test");
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        metadataValues = publication.getMetadata();
        assertThat(metadataValues, hasSize(10));
        assertThat(metadataValues, hasItem(with("dc.contributor.author", "Walter White", personId, 600)));
        assertThat(metadataValues, hasItem(with("cris.virtual.department", "4Science")));
        assertThat(metadataValues, hasItem(with("cris.virtualsource.department", personId)));

        assertThat(virtualField, equalTo(getFirstMetadataValue(publication, "cris.virtual.department")));
        assertThat(virtualSourceField, equalTo(getFirstMetadataValue(publication, "cris.virtualsource.department")));

    }

    @Test
    public void testManyMetadataValuesEnhancement() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person1 = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withPersonMainAffiliation("4Science")
            .build();

        Item person2 = ItemBuilder.createItem(context, collection)
            .withTitle("John Smith")
            .build();

        Item person3 = ItemBuilder.createItem(context, collection)
            .withTitle("Jesse Pinkman")
            .withPersonMainAffiliation("University of Rome")
            .build();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .withAuthor("Red Smith")
            .withAuthor("Walter White", person1.getID().toString())
            .withAuthor("John Smith", person2.getID().toString())
            .withAuthor("Jesse Pinkman", person3.getID().toString())
            .build();

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        List<MetadataValue> values = publication.getMetadata();
        assertThat(values, hasSize(18));
        assertThat(values, hasItem(with("dc.contributor.author", "Red Smith")));
        assertThat(values, hasItem(with("dc.contributor.author", "Walter White", person1.getID().toString(), 1, 600)));
        assertThat(values, hasItem(with("dc.contributor.author", "John Smith", person2.getID().toString(), 2, 600)));
        assertThat(values, hasItem(with("dc.contributor.author", "Jesse Pinkman", person3.getID().toString(), 3, 600)));
        assertThat(values, hasItem(with("cris.virtual.department", PLACEHOLDER_PARENT_METADATA_VALUE, 0)));
        assertThat(values, hasItem(with("cris.virtualsource.department", PLACEHOLDER_PARENT_METADATA_VALUE, 0)));
        assertThat(values, hasItem(with("cris.virtual.department", "4Science", 1)));
        assertThat(values, hasItem(with("cris.virtualsource.department", person1.getID().toString(), 1)));
        assertThat(values, hasItem(with("cris.virtual.department", PLACEHOLDER_PARENT_METADATA_VALUE, 2)));
        assertThat(values, hasItem(with("cris.virtualsource.department", person2.getID().toString(), 2)));
        assertThat(values, hasItem(with("cris.virtual.department", "University of Rome", 3)));
        assertThat(values, hasItem(with("cris.virtualsource.department", person3.getID().toString(), 3)));

        assertThat(getMetadataValues(publication, "cris.virtual.department"), hasSize(4));
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), hasSize(4));

    }

    @Test
    public void testEnhancementAfterMetadataAddition() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withPersonMainAffiliation("4Science")
            .build();

        String personId = person.getID().toString();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .build();

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        List<MetadataValue> metadataValues = publication.getMetadata();
        assertThat(metadataValues, hasSize(6));

        assertThat(getMetadataValues(publication, "cris.virtual.department"), empty());
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), empty());

        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, publication, "dc", "contributor", "author",
            null, "Walter White", personId, 600);
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        metadataValues = publication.getMetadata();
        assertThat(metadataValues, hasSize(9));
        assertThat(metadataValues, hasItem(with("dc.contributor.author", "Walter White", personId, 600)));
        assertThat(metadataValues, hasItem(with("cris.virtual.department", "4Science")));
        assertThat(metadataValues, hasItem(with("cris.virtualsource.department", personId)));

    }

    @Test
    public void testEnhancementWithMetadataRemoval() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person1 = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withPersonMainAffiliation("4Science")
            .build();

        Item person2 = ItemBuilder.createItem(context, collection)
            .withTitle("John Smith")
            .withPersonMainAffiliation("Company")
            .build();

        Item person3 = ItemBuilder.createItem(context, collection)
            .withTitle("Jesse Pinkman")
            .withPersonMainAffiliation("University of Rome")
            .build();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .withAuthor("Walter White", person1.getID().toString())
            .withAuthor("John Smith", person2.getID().toString())
            .withAuthor("Jesse Pinkman", person3.getID().toString())
            .build();

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        List<MetadataValue> values = publication.getMetadata();
        assertThat(values, hasSize(15));
        assertThat(values, hasItem(with("dc.contributor.author", "Walter White", person1.getID().toString(), 0, 600)));
        assertThat(values, hasItem(with("dc.contributor.author", "John Smith", person2.getID().toString(), 1, 600)));
        assertThat(values, hasItem(with("dc.contributor.author", "Jesse Pinkman", person3.getID().toString(), 2, 600)));
        assertThat(values, hasItem(with("cris.virtual.department", "4Science")));
        assertThat(values, hasItem(with("cris.virtualsource.department", person1.getID().toString())));
        assertThat(values, hasItem(with("cris.virtual.department", "Company", 1)));
        assertThat(values, hasItem(with("cris.virtualsource.department", person2.getID().toString(), 1)));
        assertThat(values, hasItem(with("cris.virtual.department", "University of Rome", 2)));
        assertThat(values, hasItem(with("cris.virtualsource.department", person3.getID().toString(), 2)));

        assertThat(getMetadataValues(publication, "cris.virtual.department"), hasSize(3));
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), hasSize(3));

        MetadataValue authorToRemove = getMetadataValues(publication, "dc.contributor.author").get(1);

        context.turnOffAuthorisationSystem();
        itemService.removeMetadataValues(context, publication, List.of(authorToRemove));
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        values = publication.getMetadata();
        assertThat(values, hasSize(12));
        assertThat(values, hasItem(with("dc.contributor.author", "Walter White", person1.getID().toString(), 0, 600)));
        assertThat(values, hasItem(with("dc.contributor.author", "Jesse Pinkman", person3.getID().toString(), 1, 600)));
        assertThat(values, hasItem(with("cris.virtual.department", "4Science")));
        assertThat(values, hasItem(with("cris.virtualsource.department", person1.getID().toString())));
        assertThat(values, hasItem(with("cris.virtual.department", "University of Rome", 1)));
        assertThat(values, hasItem(with("cris.virtualsource.department", person3.getID().toString(), 1)));

        assertThat(getMetadataValues(publication, "cris.virtual.department"), hasSize(2));
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), hasSize(2));

    }

    @Test
    public void testWithWorkspaceItem() throws Exception {
        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withPersonMainAffiliation("4Science")
            .build();

        String personId = person.getID().toString();

        WorkspaceItem publication = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .withAuthor("Walter White", personId)
            .build();

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        List<MetadataValue> metadataValues = publication.getItem().getMetadata();
        assertThat(metadataValues, hasSize(3));
        assertThat(getMetadataValues(publication, "cris.virtual.department"), empty());
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), empty());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEnhancementAfterItemUpdate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withOrcidIdentifier("0000-0000-1111-2222")
            .build();

        String personId = person.getID().toString();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .withAuthor("Jesse Pinkman")
            .withAuthor("Saul Goodman")
            .withAuthor("Walter White", person.getID().toString())
            .withAuthor("Gus Fring")
            .build();

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        assertThat(getMetadataValues(publication, "dc.contributor.author"), contains(
            with("dc.contributor.author", "Jesse Pinkman"),
            with("dc.contributor.author", "Saul Goodman", 1),
            with("dc.contributor.author", "Walter White", personId, 2, 600),
            with("dc.contributor.author", "Gus Fring", 3)));

        assertThat(getMetadataValues(publication, "cris.virtual.author-orcid"), contains(
            with("cris.virtual.author-orcid", PLACEHOLDER_PARENT_METADATA_VALUE),
            with("cris.virtual.author-orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 1),
            with("cris.virtual.author-orcid", "0000-0000-1111-2222", 2),
            with("cris.virtual.author-orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 3)));

        assertThat(getMetadataValues(publication, "cris.virtualsource.author-orcid"), contains(
            with("cris.virtualsource.author-orcid", PLACEHOLDER_PARENT_METADATA_VALUE),
            with("cris.virtualsource.author-orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 1),
            with("cris.virtualsource.author-orcid", personId, 2),
            with("cris.virtualsource.author-orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 3)));

        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, publication, "dc", "title", "alternative", null, "Other name");
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        assertThat(getMetadataValues(publication, "dc.contributor.author"), contains(
            with("dc.contributor.author", "Jesse Pinkman"),
            with("dc.contributor.author", "Saul Goodman", 1),
            with("dc.contributor.author", "Walter White", personId, 2, 600),
            with("dc.contributor.author", "Gus Fring", 3)));

        assertThat(getMetadataValues(publication, "cris.virtual.author-orcid"), contains(
            with("cris.virtual.author-orcid", PLACEHOLDER_PARENT_METADATA_VALUE),
            with("cris.virtual.author-orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 1),
            with("cris.virtual.author-orcid", "0000-0000-1111-2222", 2),
            with("cris.virtual.author-orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 3)));

        assertThat(getMetadataValues(publication, "cris.virtualsource.author-orcid"), contains(
            with("cris.virtualsource.author-orcid", PLACEHOLDER_PARENT_METADATA_VALUE),
            with("cris.virtualsource.author-orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 1),
            with("cris.virtualsource.author-orcid", personId, 2),
            with("cris.virtualsource.author-orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 3)));

    }

    private MetadataValue getFirstMetadataValue(Item item, String metadataField) {
        return getMetadataValues(item, metadataField).get(0);
    }

    private List<MetadataValue> getMetadataValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField);
    }

    private List<MetadataValue> getMetadataValues(WorkspaceItem item, String metadataField) {
        return itemService.getMetadataByMetadataString(item.getItem(), metadataField);
    }

    @SuppressWarnings("rawtypes")
    private <T extends ReloadableEntity> T commitAndReload(T entity) throws SQLException, AuthorizeException {
        context.commit();
        return context.reloadEntity(entity);
    }

}
