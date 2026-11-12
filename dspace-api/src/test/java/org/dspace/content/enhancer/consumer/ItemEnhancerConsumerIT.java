/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.consumer;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ReloadableEntity;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;

public class ItemEnhancerConsumerIT extends AbstractIntegrationTestWithDatabase {

    private ItemService itemService;

    private Collection collection;

    private PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();

    private ChoiceAuthorityService choiceAuthorityService = ContentAuthorityServiceFactory
        .getInstance().getChoiceAuthorityService();

    private MetadataAuthorityService metadataAuthorityService = ContentAuthorityServiceFactory
        .getInstance().getMetadataAuthorityService();

    private ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    @Before
    public void setup() throws Exception {

        itemService = ContentServiceFactory.getInstance().getItemService();

        // Configure the ItemAuthority used by the promotion of authority-backed metadata to relationships
        choiceAuthorityService.getChoiceAuthoritiesNames();
        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] { "org.dspace.content.authority.ItemAuthority = AuthorAuthority" });
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        configurationService.setProperty("choices.plugin.dc.contributor.editor", "AuthorAuthority");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        configurationService.setProperty("authority.controlled.dc.contributor.editor", "true");
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

    @Test
    public void testSingleMetadataValueEnhancement() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withEntityType("Person")
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
        assertThat(metadataValues, hasSize(7));
        assertThat(metadataValues, hasItem(with("dspace.virtual.department", "4Science", null, personId, null, -1)));

        MetadataValue primary = getFirstMetadataValue(publication, "dc.contributor.author");
        MetadataValue virtualField = getFirstMetadataValue(publication, "dspace.virtual.department");

        assertThat(virtualField.getRelationship(), notNullValue());
        assertThat(virtualField.getRelationship().getID(), equalTo(primary.getRelationship().getID()));
        assertThat(virtualField.getAuthority(), equalTo(personId));

        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, publication, "dc", "subject", null, null, "Test");
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        metadataValues = publication.getMetadata();
        assertThat(metadataValues, hasSize(8));
        assertThat(metadataValues, hasItem(with("dc.contributor.author", "Walter White", personId, 600)));
        assertThat(metadataValues, hasItem(with("dspace.virtual.department", "4Science", null, personId, null, -1)));
        assertThat(getMetadataValues(publication, "dspace.virtual.department"), hasSize(1));
        assertThat(getMetadataValues(publication, "dspace.virtual.orcid"), empty());

    }

    @Test
    public void testManyMetadataValuesEnhancement() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person1 = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withEntityType("Person")
            .withPersonMainAffiliation("4Science")
            .build();

        Item person2 = ItemBuilder.createItem(context, collection)
            .withTitle("John Smith")
            .withEntityType("Person")
            .build();

        Item person3 = ItemBuilder.createItem(context, collection)
            .withTitle("Jesse Pinkman")
            .withEntityType("Person")
            .withPersonMainAffiliation("University of Rome")
            .build();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .withAuthor("Red Smith")
            .withAuthor("Walter White", person1.getID().toString())
            .withAuthor("John Smith", person2.getID().toString())
            .withEditor("Jesse Pinkman", person3.getID().toString())
            .build();

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        List<MetadataValue> values = publication.getMetadata();
        assertThat(values, hasSize(11));
        assertThat(values, hasItem(with("dc.contributor.author", "Red Smith")));
        assertThat(values, hasItem(with("dc.contributor.author", "Walter White", person1.getID().toString(), 1, 600)));
        assertThat(values, hasItem(with("dc.contributor.author", "John Smith", person2.getID().toString(), 2, 600)));
        assertThat(values, hasItem(with("dc.contributor.editor", "Jesse Pinkman", person3.getID().toString(), 0, 600)));

        assertThat(getMetadataValues(publication, "dspace.virtual.department"), hasSize(2));
        assertThat(getMetadataValues(publication, "dspace.virtual.orcid"), empty());
        assertThat(values, hasItem(with("dspace.virtual.department", "4Science", null,
            person1.getID().toString(), null, -1)));
        assertThat(values, hasItem(with("dspace.virtual.department", "University of Rome", null,
            person3.getID().toString(), null, -1)));

    }

    @Test
    public void testEnhancementAfterMetadataAddition() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withEntityType("Person")
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
        assertThat(metadataValues, hasSize(5));

        assertThat(getMetadataValues(publication, "dspace.virtual.department"), empty());
        assertThat(getMetadataValues(publication, "dspace.virtual.orcid"), empty());

        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, publication, "dc", "contributor", "author",
            null, "Walter White", personId, 600);
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        metadataValues = publication.getMetadata();
        assertThat(metadataValues, hasSize(7));
        assertThat(metadataValues, hasItem(with("dc.contributor.author", "Walter White", personId, 600)));
        assertThat(metadataValues, hasItem(with("dspace.virtual.department", "4Science", null, personId, null, -1)));

    }

    @Test
    public void testEnhancementWithMetadataRemoval() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person1 = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withEntityType("Person")
            .withPersonMainAffiliation("4Science")
            .build();

        Item person2 = ItemBuilder.createItem(context, collection)
            .withTitle("John Smith")
            .withEntityType("Person")
            .withPersonMainAffiliation("Company")
            .build();

        Item person3 = ItemBuilder.createItem(context, collection)
            .withTitle("Jesse Pinkman")
            .withEntityType("Person")
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
        assertThat(values, hasSize(11));
        assertThat(values, hasItem(with("dc.contributor.author", "Walter White", person1.getID().toString(), 0, 600)));
        assertThat(values, hasItem(with("dc.contributor.author", "John Smith", person2.getID().toString(), 1, 600)));
        assertThat(values, hasItem(with("dc.contributor.author", "Jesse Pinkman", person3.getID().toString(), 2, 600)));

        assertThat(getMetadataValues(publication, "dspace.virtual.department"), hasSize(3));
        assertThat(getMetadataValues(publication, "dspace.virtual.orcid"), empty());
        assertThat(values, hasItem(with("dspace.virtual.department", "4Science", null,
            person1.getID().toString(), null, -1)));
        assertThat(values, hasItem(with("dspace.virtual.department", "Company", null,
            person2.getID().toString(), null, -1)));
        assertThat(values, hasItem(with("dspace.virtual.department", "University of Rome", null,
            person3.getID().toString(), null, -1)));

        MetadataValue authorToRemove = getMetadataValues(publication, "dc.contributor.author").get(1);

        context.turnOffAuthorisationSystem();
        itemService.removeMetadataValues(context, publication, List.of(authorToRemove));
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        values = publication.getMetadata();
        assertThat(values, hasSize(9));
        assertThat(values, hasItem(with("dc.contributor.author", "Walter White", person1.getID().toString(), 0, 600)));
        assertThat(values, hasItem(with("dc.contributor.author", "Jesse Pinkman", person3.getID().toString(), 1, 600)));

        assertThat(getMetadataValues(publication, "dspace.virtual.department"), hasSize(2));
        assertThat(getMetadataValues(publication, "dspace.virtual.orcid"), empty());
        assertThat(values, hasItem(with("dspace.virtual.department", "4Science", null,
            person1.getID().toString(), null, -1)));
        assertThat(values, hasItem(with("dspace.virtual.department", "University of Rome", null,
            person3.getID().toString(), null, -1)));

    }

    @Test
    public void testWithWorkspaceItem() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withEntityType("Person")
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
        assertThat(getMetadataValues(publication, "dspace.virtual.department"), empty());
        assertThat(getMetadataValues(publication, "dspace.virtual.orcid"), empty());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEnhancementAfterItemUpdate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withEntityType("Person")
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

        assertThat(getMetadataValues(publication, "dspace.virtual.orcid"), contains(
            with("dspace.virtual.orcid", "0000-0000-1111-2222", null, personId, null, -1)));

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

        assertThat(getMetadataValues(publication, "dspace.virtual.orcid"), contains(
            with("dspace.virtual.orcid", "0000-0000-1111-2222", null, personId, null, -1)));

    }

    @Test
    public void testMultipleRelatedItemValuesEnhancement() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person1 = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withEntityType("Person")
            .withPersonMainAffiliation("4Science")
            .withPersonMainAffiliation("DSpace")
            .withOrcidIdentifier("orcid1")
            .build();

        Item person2 = ItemBuilder.createItem(context, collection)
            .withTitle("John Smith")
            .withEntityType("Person")
            .build();

        Item person3 = ItemBuilder.createItem(context, collection)
            .withTitle("Jesse Pinkman")
            .withEntityType("Person")
            .withPersonMainAffiliation("University of Rome")
            .build();

        String person1Id = person1.getID().toString();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .withAuthor("Walter White", person1Id)
            .withAuthor("John Smith", person2.getID().toString())
            .withEditor("Walter White", person1Id)
            .withEditor("Jesse Pinkman", person3.getID().toString())
            .build();

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        MetadataValue authorPrimary = findByAuthority(
            getMetadataValues(publication, "dc.contributor.author"), person1Id);
        MetadataValue editorPrimary = findByAuthority(
            getMetadataValues(publication, "dc.contributor.editor"), person1Id);

        List<MetadataValue> orcids = getMetadataValues(publication, "dspace.virtual.orcid");
        assertThat(orcids, hasSize(2));

        List<MetadataValue> person1Orcids = orcids.stream()
            .filter(value -> "orcid1".equals(value.getValue()))
            .collect(Collectors.toList());
        assertThat(person1Orcids, hasSize(2));
        assertThat(person1Orcids.stream().map(value -> value.getRelationship().getID())
            .collect(Collectors.toSet()), equalTo(Set.of(
                authorPrimary.getRelationship().getID(), editorPrimary.getRelationship().getID())));

        for (MetadataValue orcid : person1Orcids) {
            assertThat(orcid.getRelationship(), notNullValue());
            assertThat(orcid.getAuthority(), equalTo(person1Id));
        }

        List<MetadataValue> departments = getMetadataValues(publication, "dspace.virtual.department");
        assertThat(departments, hasSize(5));
        assertThat(departments.stream().filter(value -> "4Science".equals(value.getValue()))
            .collect(Collectors.toList()), hasSize(2));
        assertThat(departments.stream().filter(value -> "DSpace".equals(value.getValue()))
            .collect(Collectors.toList()), hasSize(2));
        assertThat(departments, hasItem(with("dspace.virtual.department", "University of Rome", null,
            person3.getID().toString(), null, -1)));
        assertThat(departments.stream().map(value -> value.getRelationship().getID()).distinct()
            .collect(Collectors.toSet()), equalTo(Set.of(
                authorPrimary.getRelationship().getID(),
                editorPrimary.getRelationship().getID(),
                findByAuthority(getMetadataValues(publication, "dc.contributor.editor"),
                    person3.getID().toString()).getRelationship().getID())));

    }

    private MetadataValue findByAuthority(List<MetadataValue> values, String authority) {
        return values.stream()
            .filter(value -> authority.equals(value.getAuthority()))
            .findFirst()
            .orElseThrow(IllegalStateException::new);
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
