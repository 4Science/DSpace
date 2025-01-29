/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.consumer;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.app.matcher.MetadataValueMatcher.withNoPlace;
import static org.dspace.content.authority.Choices.CF_ACCEPTED;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.StringUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.MetadataFieldBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
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

    public static final String CRIS_VIRTUALSOURCE_ROOT_FOND = "cris.virtualsource.rootFond";
    public static final String CRIS_VIRTUAL_ROOT_FOND = "cris.virtual.rootFond";
    public static final String GLAMFONDS_PARENT = "glamfonds.parent";
    public static final String CRIS_VIRTUAL_TREE_FONDS_ROOT = "cris.virtual.treeFondsRoot";
    public static final String CRIS_VIRTUALSOURCE_TREE_FONDS_ROOT = "cris.virtualsource.treeFondsRoot";

    private ItemService itemService;
    private ConfigurationService configurationService;
    private PluginService pluginService;
    private ChoiceAuthorityService choiceAuthorityService;
    private MetadataAuthorityService metadataAuthorityService;

    private Collection collection;
    private Collection fondsCollection;
    private Item rootFond;
    private Item childFond;
    private Item leafFond;

    @Before
    public void setup() {

        itemService = ContentServiceFactory.getInstance().getItemService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        pluginService = CoreServiceFactory.getInstance().getPluginService();
        choiceAuthorityService =  ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
        metadataAuthorityService =  ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();

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
        assertThat(metadataValues, hasSize(11));
        assertThat(metadataValues, hasItem(with("cris.virtual.department", "4Science")));
        assertThat(metadataValues, hasItem(with("cris.virtualsource.department", personId)));
        assertThat(metadataValues, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE)));
        assertThat(metadataValues, hasItem(with("cris.virtualsource.orcid", personId)));


        MetadataValue virtualField = getFirstMetadataValue(publication, "cris.virtual.department");
        MetadataValue virtualSourceField = getFirstMetadataValue(publication, "cris.virtualsource.department");

        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, publication, "dc", "subject", null, null, "Test");
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        metadataValues = publication.getMetadata();
        assertThat(metadataValues, hasSize(12));
        assertThat(metadataValues, hasItem(with("dc.contributor.author", "Walter White", personId, 600)));
        assertThat(metadataValues, hasItem(with("cris.virtual.department", "4Science")));
        assertThat(metadataValues, hasItem(with("cris.virtualsource.department", personId)));
        assertThat(metadataValues, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE)));
        assertThat(metadataValues, hasItem(with("cris.virtualsource.orcid", personId)));

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
            .withEditor("Jesse Pinkman", person3.getID().toString())
            .build();

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        List<MetadataValue> values = publication.getMetadata();
        assertThat(values, hasSize(22));
        assertThat(values, hasItem(with("dc.contributor.author", "Red Smith")));
        assertThat(values, hasItem(with("dc.contributor.author", "Walter White", person1.getID().toString(), 1, 600)));
        assertThat(values, hasItem(with("dc.contributor.author", "John Smith", person2.getID().toString(), 2, 600)));
        assertThat(values, hasItem(with("dc.contributor.editor", "Jesse Pinkman", person3.getID().toString(), 0, 600)));
        // virtual source and virtual metadata are not required to respect the order of the source metadata
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.department", person1.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.department", person2.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.department", person3.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtual.department", PLACEHOLDER_PARENT_METADATA_VALUE)));
        assertThat(values, hasItem(withNoPlace("cris.virtual.department", "4Science")));
        assertThat(values, hasItem(withNoPlace("cris.virtual.department", "University of Rome")));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.orcid", person1.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.orcid", person2.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.orcid", person3.getID().toString())));
        // we can check with the position as all the values are expected to be placeholder
        assertThat(values, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 0)));
        assertThat(values, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 1)));
        assertThat(values, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 2)));
        assertThat(getMetadataValues(publication, "cris.virtual.department"), hasSize(3));
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), hasSize(3));
        assertThat(getMetadataValues(publication, "cris.virtual.orcid"), hasSize(3));
        assertThat(getMetadataValues(publication, "cris.virtualsource.orcid"), hasSize(3));

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
        assertThat(metadataValues, hasSize(11));
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
        assertThat(values, hasSize(21));
        assertThat(values, hasItem(with("dc.contributor.author", "Walter White", person1.getID().toString(), 0, 600)));
        assertThat(values, hasItem(with("dc.contributor.author", "John Smith", person2.getID().toString(), 1, 600)));
        assertThat(values, hasItem(with("dc.contributor.author", "Jesse Pinkman", person3.getID().toString(), 2, 600)));

        // virtual source and virtual metadata are not required to respect the order of the source metadata
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.department", person1.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.department", person2.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.department", person3.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtual.department", "4Science")));
        assertThat(values, hasItem(withNoPlace("cris.virtual.department", "Company")));
        assertThat(values, hasItem(withNoPlace("cris.virtual.department", "University of Rome")));
        assertThat(getMetadataValues(publication, "cris.virtual.department"), hasSize(3));
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), hasSize(3));

        assertThat(values, hasItem(withNoPlace("cris.virtualsource.orcid",  person1.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.orcid", person2.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.orcid", person3.getID().toString())));
        // we can check with the position as all the values are expected to be placeholder
        assertThat(values, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 0)));
        assertThat(values, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 1)));
        assertThat(values, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 2)));
        assertThat(getMetadataValues(publication, "cris.virtual.orcid"), hasSize(3));
        assertThat(getMetadataValues(publication, "cris.virtualsource.orcid"), hasSize(3));


        MetadataValue authorToRemove = getMetadataValues(publication, "dc.contributor.author").get(1);

        context.turnOffAuthorisationSystem();
        itemService.removeMetadataValues(context, publication, List.of(authorToRemove));
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        values = publication.getMetadata();
        assertThat(values, hasSize(16));
        assertThat(values, hasItem(with("dc.contributor.author", "Walter White", person1.getID().toString(), 0, 600)));
        assertThat(values, hasItem(with("dc.contributor.author", "Jesse Pinkman", person3.getID().toString(), 1, 600)));
        // virtual source and virtual metadata are not required to respect the order of the source metadata
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.department", person1.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.department", person3.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtual.department", "4Science")));
        assertThat(values, hasItem(withNoPlace("cris.virtual.department", "University of Rome")));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.orcid",  person1.getID().toString())));
        assertThat(values, hasItem(withNoPlace("cris.virtualsource.orcid", person3.getID().toString())));
        // we can check with the position as all the values are expected to be placeholder
        assertThat(values, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 0)));
        assertThat(values, hasItem(with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE, 1)));
        assertThat(getMetadataValues(publication, "cris.virtual.department"), hasSize(2));
        assertThat(getMetadataValues(publication, "cris.virtualsource.department"), hasSize(2));
        assertThat(getMetadataValues(publication, "cris.virtual.orcid"), hasSize(2));
        assertThat(getMetadataValues(publication, "cris.virtualsource.orcid"), hasSize(2));

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

        assertThat(getMetadataValues(publication, "cris.virtual.orcid"), contains(
            with("cris.virtual.orcid", "0000-0000-1111-2222")));

        assertThat(getMetadataValues(publication, "cris.virtualsource.orcid"), contains(
            with("cris.virtualsource.orcid", personId)));

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

        assertThat(getMetadataValues(publication, "cris.virtual.orcid"), contains(
                with("cris.virtual.orcid", "0000-0000-1111-2222")));

        assertThat(getMetadataValues(publication, "cris.virtualsource.orcid"), contains(
            with("cris.virtualsource.orcid", personId)));

    }

    @Test
    public void testMultipleRelatedItemValuesEnhancement() throws Exception {

        context.turnOffAuthorisationSystem();
        MetadataSchema schema = ContentServiceFactory.getInstance()
                .getMetadataSchemaService().find(context, "cris");
        MetadataFieldBuilder.createMetadataField(context, schema, "virtual", "testmultival", null);
        MetadataFieldBuilder.createMetadataField(context, schema, "virtualsource", "testmultival", null);

        Item person1 = ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withPersonMainAffiliation("4Science")
            .withPersonMainAffiliation("DSpace")
            .withOrcidIdentifier("orcid1")
            .build();

        Item person2 = ItemBuilder.createItem(context, collection)
            .withTitle("John Smith")
            .build();

        Item person3 = ItemBuilder.createItem(context, collection)
            .withTitle("Jesse Pinkman")
            .withPersonMainAffiliation("University of Rome")
            .build();

        Item testEntity = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication")
            // let's use our custom entity for test purpose, see extra-metadata-enhancers-for-test.xml
            .withEntityType("TestEntity")
            .withAuthor("Red Smith")
            .withAuthor("Walter White", person1.getID().toString())
            .withAuthor("John Smith", person2.getID().toString())
            .withEditor("Jesse Pinkman", person3.getID().toString())
            .build();

        context.restoreAuthSystemState();
        testEntity = commitAndReload(testEntity);

        List<MetadataValue> values = testEntity.getMetadata();
        assertThat(values, hasItem(with("dc.contributor.author", "Red Smith")));
        assertThat(values, hasItem(with("dc.contributor.author", "Walter White", person1.getID().toString(), 1, 600)));
        assertThat(values, hasItem(with("dc.contributor.author", "John Smith", person2.getID().toString(), 2, 600)));
        assertThat(values, hasItem(with("dc.contributor.editor", "Jesse Pinkman", person3.getID().toString(), 0, 600)));
        // virtual source and virtual metadata are not required to respect the order of the source metadata
        List<Integer> posPerson1 = getPlacesAsVirtualSource(person1, testEntity, "cris.virtualsource.testmultival");
        List<Integer> posPerson2 = getPlacesAsVirtualSource(person2, testEntity, "cris.virtualsource.testmultival");
        List<Integer> posPerson3 = getPlacesAsVirtualSource(person3, testEntity, "cris.virtualsource.testmultival");
        assertThat(values,
                hasItem(with("cris.virtualsource.testmultival", person1.getID().toString(), posPerson1.get(0))));
        assertThat(values, hasItem(with("cris.virtual.testmultival", "4Science", posPerson1.get(0))));
        assertThat(values,
                hasItem(with("cris.virtualsource.testmultival", person1.getID().toString(), posPerson1.get(1))));
        assertThat(values, hasItem(with("cris.virtual.testmultival", "DSpace", posPerson1.get(1))));
        assertThat(values,
                hasItem(with("cris.virtualsource.testmultival", person1.getID().toString(), posPerson1.get(2))));
        assertThat(values, hasItem(with("cris.virtual.testmultival", "orcid1", posPerson1.get(2))));

        assertThat(values,
                hasItem(with("cris.virtualsource.testmultival", person2.getID().toString(), posPerson2.get(0))));
        assertThat(values,
                hasItem(with("cris.virtual.testmultival", PLACEHOLDER_PARENT_METADATA_VALUE, posPerson2.get(0))));

        assertThat(values,
                hasItem(with("cris.virtualsource.testmultival", person3.getID().toString(), posPerson3.get(0))));
        assertThat(values, hasItem(with("cris.virtual.testmultival", "University of Rome", posPerson3.get(0))));

        assertThat(getMetadataValues(testEntity, "cris.virtualsource.testmultival"), hasSize(5));
        assertThat(getMetadataValues(testEntity, "cris.virtual.testmultival"), hasSize(5));

    }


    @Test
    public void testRelatedFondFilterEnhancements() throws Exception {

        context.turnOffAuthorisationSystem();

        initFonds();

        context.restoreAuthSystemState();

        childFond = commitAndReload(childFond);

        List<MetadataValue> metadataValues = childFond.getMetadata();

        // check that the root fond reference has been pushed to the child
        assertThat(
            metadataValues,
            hasItem(
                with(GLAMFONDS_PARENT, "Root Fond", rootFond.getID().toString(), CF_ACCEPTED)
            )
        );
        assertThat(
            metadataValues,
            hasItem(
                with(
                    CRIS_VIRTUAL_ROOT_FOND,
                    rootFond.getName(),
                    rootFond.getID().toString(),0, 600
                )
            )
        );
        assertThat(
            metadataValues,
            hasItem(
                with(
                    CRIS_VIRTUALSOURCE_ROOT_FOND,
                    rootFond.getID().toString()
                )
            )
        );

        leafFond = context.reloadEntity(leafFond);

        context.turnOffAuthorisationSystem();

        // adds a glamfonds.parent after
        itemService.addMetadata(
            context, leafFond, "glamfonds", "parent", null, null, "Child Fond",
            childFond.getID().toString(), CF_ACCEPTED
        );

        itemService.update(context, leafFond);

        context.restoreAuthSystemState();

        leafFond = commitAndReload(leafFond);

        metadataValues = leafFond.getMetadata();

        assertThat(
            metadataValues,
            hasItem(
                with(GLAMFONDS_PARENT, "Child Fond", childFond.getID().toString(), CF_ACCEPTED)
            )
        );
        assertThat(
            metadataValues,
            hasItem(
                with(
                    CRIS_VIRTUAL_ROOT_FOND,
                    "Root Fond",
                    rootFond.getID().toString(),
                    CF_ACCEPTED
                )
            )
        );
        assertThat(
            metadataValues,
            hasItem(
                with(
                    CRIS_VIRTUALSOURCE_ROOT_FOND,
                    childFond.getID().toString()
                )
            )
        );

    }

    @Test
    public void testPublicationRelatedFondFilterEnhancements() throws Exception {

        context.turnOffAuthorisationSystem();

        initFonds();

        Item publication =
            ItemBuilder.createItem(context, collection)
                       .withEntityType("Publication")
                       .withTitle("My Publication")
                       .withRelationFonds("Leaf Fond", leafFond.getID().toString())
                       .build();

        Item customPublication =
            ItemBuilder.createItem(context, collection)
                       .withEntityType("Publication")
                       .withTitle("Custom Publication")
                       .build();

        context.restoreAuthSystemState();

        publication = commitAndReload(publication);

        List<MetadataValue> metadataValues = publication.getMetadata();

        assertThat(
            metadataValues,
            hasItem(
                with(
                    CRIS_VIRTUAL_TREE_FONDS_ROOT,
                    "Root Fond",
                    rootFond.getID().toString(),
                    CF_ACCEPTED
                )
            )
        );
        assertThat(
            metadataValues,
            hasItem(
                with(
                    CRIS_VIRTUALSOURCE_TREE_FONDS_ROOT,
                    leafFond.getID().toString()
                )
            )
        );

        customPublication = context.reloadEntity(customPublication);

        context.turnOffAuthorisationSystem();

        // adds a glamfonds.parent after
        itemService.addMetadata(
            context, customPublication, "dc", "relation", "fonds", null, "Leaf Fond",
            leafFond.getID().toString(), CF_ACCEPTED
        );

        itemService.update(context, customPublication);

        context.restoreAuthSystemState();

        customPublication = commitAndReload(customPublication);

        metadataValues = customPublication.getMetadata();

        assertThat(
            metadataValues,
            hasItem(
                with(
                    CRIS_VIRTUAL_TREE_FONDS_ROOT,
                    "Root Fond",
                    rootFond.getID().toString(),
                    CF_ACCEPTED
                )
            )
        );
        assertThat(
            metadataValues,
            hasItem(
                with(
                    CRIS_VIRTUALSOURCE_TREE_FONDS_ROOT,
                    leafFond.getID().toString()
                )
            )
        );

    }


    private void initFonds() {
        fondsCollection =
            CollectionBuilder.createCollection(context, parentCommunity)
                             .withName("Fonds Collection")
                             .withEntityType("Fonds")
                             .build();

        rootFond =
            ItemBuilder.createItem(context, fondsCollection)
                       .withTitle("Root Fond")
                       .build();


        childFond =
            ItemBuilder.createItem(context, fondsCollection)
                       .withTitle("Child Fond")
                       .withFondParent("Root Fond", rootFond.getID())
                       .build();


        leafFond =
            ItemBuilder.createItem(context, fondsCollection)
                       .withTitle("Leaf Fond")
                       .withFondParent("Child Fond", childFond.getID())
                       .build();
    }

    @Test
    public void testSingleMetadataJournalAnceEnhancement() throws Exception {

        configurationService.setProperty("choices.plugin.dc.relation.journal", "SherpaAuthority");
        configurationService.setProperty("choices.presentation.dc.relation.journal", "suggest");
        configurationService.setProperty("authority.controlled.dc.relation.journal", "true");

        // These clears have to happen so that the config is actually reloaded in those classes. This is needed for
        // the properties that we're altering above and this is only used within the tests
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        Item journalItem = ItemBuilder.createItem(context, collection)
                .withTitle("Test journal")
                .withEntityType("Journal")
                .withJournalAnce("AA110022")
                .build();

        Item publication = ItemBuilder.createItem(context, collection)
                .withTitle("Test publication")
                .withEntityType("Publication")
                .withRelationJournal(journalItem.getName(), journalItem.getID().toString())
                .build();

        context.restoreAuthSystemState();
        publication = commitAndReload(publication);

        List<MetadataValue> metadataValues = publication.getMetadata();
        assertThat(metadataValues, hasSize(9));
        assertThat(metadataValues, hasItem(with("cris.virtual.journalance", "AA110022")));
        assertThat(metadataValues, hasItem(with("cris.virtualsource.journalance", journalItem.getID().toString())));
    }

    @Test
    public void testVirtualMetadataTreeFondsRootAndSourceSetCorrectly() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        // Create collections for each entity type
        Collection fondsCollection = CollectionBuilder.createCollection(context, parentCommunity)
                                                      .withEntityType("Fonds")
                                                      .build();

        Collection archivalMaterialCollection = CollectionBuilder.createCollection(context, parentCommunity)
                                                                 .withEntityType("ArchivalMaterial")
                                                                 .build();

        Collection publicationCollection = CollectionBuilder.createCollection(context, parentCommunity)
                                                            .withEntityType("Publication")
                                                            .build();

        // Create root Fonds
        Item rootFond = ItemBuilder.createItem(context, fondsCollection)
                                   .withTitle("Root Fonds")
                                   .build();

        // Create a child Fonds (leaf)
        Item childFond = ItemBuilder.createItem(context, fondsCollection)
                                    .withFondParent(rootFond.getName(), rootFond.getID())
                                    .withMetadata("glam", "leaf", null, "true")
                                    .withTitle("Leaf Fonds")
                                    .build();

        // Create and relate a Publication item to the child Fonds
        Item publicationItem = ItemBuilder.createItem(context, publicationCollection)
                                          .withMetadata("dc", "relation", "journalfonds", null, childFond.getName(),
                                                        childFond.getID().toString(), 600)
                                          .withTitle("Publication Item")
                                          .build();

        // Create and relate an ArchivalMaterial item to the child Fonds
        Item archivalMaterialItem = ItemBuilder.createItem(context, archivalMaterialCollection)
                                               .withMetadata("dc", "relation", "journalfonds", null, childFond.getName(),
                                                             childFond.getID().toString(), 600)
                                               .withTitle("Archival Material Item")
                                               .build();

        context.restoreAuthSystemState();
        publicationItem = commitAndReload(publicationItem);
        archivalMaterialItem = commitAndReload(archivalMaterialItem);

        List<MetadataValue> metadataValues = publicationItem.getMetadata();
        assertThat(metadataValues, hasSize(9));
        assertThat(metadataValues, hasItem(with("cris.virtual.treeFondsRoot", rootFond.getName(),
                                                rootFond.getID().toString(),0, 600)));
        assertThat(metadataValues, hasItem(with("cris.virtualsource.treeFondsRoot", childFond.getID().toString())));

        List<MetadataValue> metadataValues2 = archivalMaterialItem.getMetadata();
        assertThat(metadataValues2, hasSize(9));
        assertThat(metadataValues2, hasItem(with("cris.virtual.treeFondsRoot", rootFond.getName(),
                                                 rootFond.getID().toString(),0, 600)));
        assertThat(metadataValues2, hasItem(with("cris.virtualsource.treeFondsRoot", childFond.getID().toString())));

    }

    @Test
    public void testVirtualMetadataTreeJournalFondsRootAndSourceSetCorrectly() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        // Create collections for each entity type
        Collection journalFondsCollection = CollectionBuilder.createCollection(context, parentCommunity)
                                                      .withEntityType("JournalFonds")
                                                      .build();

        Collection journalFileCollection = CollectionBuilder.createCollection(context, parentCommunity)
                                                            .withEntityType("JournalFile")
                                                            .build();

        // Create root Fonds
        Item rootFond = ItemBuilder.createItem(context, journalFondsCollection)
                                   .withTitle("Root JournalFonds")
                                   .build();

        // Create a child Fonds (leaf)
        Item childFond = ItemBuilder.createItem(context, journalFondsCollection)
                                    .withJournalFondParent(rootFond.getName(), rootFond.getID())
                                    .withMetadata("glam", "leaf", null, "true")
                                    .withTitle("Leaf Fonds")
                                    .build();

        // Create and relate a Publication item to the child Fonds
        Item journalFileItem = ItemBuilder.createItem(context, journalFileCollection)
                                          .withMetadata("dc", "relation", "journalfonds", null,
                                                        childFond.getName(), childFond.getID().toString(), 600)
                                          .withTitle("Publication Item")
                                          .build();

        context.restoreAuthSystemState();
        journalFileItem = commitAndReload(journalFileItem);

        List<MetadataValue> metadataValues = journalFileItem.getMetadata();
        assertThat(metadataValues, hasSize(9));
        assertThat(metadataValues, hasItem(with("cris.virtual.treeJournalFondsRoot", rootFond.getName(),
                                                rootFond.getID().toString(),0, 600)));
        assertThat(metadataValues, hasItem(with("cris.virtualsource.treeJournalFondsRoot", childFond.getID().toString())));

    }


    private List<Integer> getPlacesAsVirtualSource(Item person1, Item publication, String metadata) {
        return getMetadataValues(publication, metadata).stream()
                .filter(mv -> StringUtils.equals(mv.getValue(), person1.getID().toString())).map(mv -> mv.getPlace())
                .collect(Collectors.toList());
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
