/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class LuckySearchIT  extends AbstractControllerIntegrationTest {
    @Autowired
    ConfigurationService configurationService;

    @Autowired
    MetadataAuthorityService metadataAuthorityService;

    @Autowired
    private DiscoveryConfigurationService discoveryConfigurationService;
    @Autowired
    private EntityTypeService entityTypeService;

    @Test
    public void discoverOrcidSearchLuckySearchConfigurationOneResult() throws Exception {
        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();
        //** GIVEN **
       // parent community with two collections
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        // Three public items that are readable by Anonymous with different orcid
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withFullName("Public item 1")
                .withIssueDate("2017-10-17")
                .withEntityType("Person")
                .withOrcidIdentifier("0000-0002-5497-7736")
                .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                .withFullName("Public item 2")
                .withIssueDate("2016-02-13")
                .withEntityType("Person")
                .withOrcidIdentifier("0000-0002-5497-1234")
                .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                .withFullName("Public item 3")
                .withIssueDate("2016-02-13")
                .withEntityType("Person")
                .withOrcidIdentifier("0000-0002-5497-4567")
                .build();

        context.restoreAuthSystemState();
        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system and enters a size of 10
        getClient().perform(get("/api/discover/search/objects")
                        .param("size", "10")
                        .param("page", "0")
                        .param("configuration", "lucky-search")
                        .param("f.orcid", "0000-0002-5497-7736,equals"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                // the configuration needs to be 'lucky-search'
                .andExpect(jsonPath("$.configuration", is("lucky-search")))
                // the type needs to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                .andExpect(jsonPath("$.appliedFilters[0].filter", is("orcid")))
                .andExpect(jsonPath("$.appliedFilters[0].value", is("0000-0002-5497-7736")))
                .andExpect(jsonPath("$.appliedFilters[0].label", is("0000-0002-5497-7736")))
                .andExpect(jsonPath("$.appliedFilters[0].operator", is("equals")))
                // total elements needs to be 1 as there is only one item with the orcid searched
                .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(1)))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]._embedded.indexableObject.id",
                        is(publicItem1.getID().toString())))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]" +
                        "._embedded.indexableObject.entityType", is("Person")))
                .andExpect(jsonPath("$._links.next").doesNotExist())
                // there always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/search/objects?" +
                        "configuration=lucky-search&f.orcid=0000-0002-5497-7736,equals")))

                // pagination
                .andExpect(jsonPath("$._embedded.searchResult.page",
                        is(PageMatcher.pageEntry(0, 10))));
        // search anonymous
        getClient().perform(get("/api/discover/search/objects")
                        .param("size", "10")
                        .param("page", "0")
                        .param("configuration", "lucky-search")
                        .param("f.orcid", "0000-0002-5497-1234,equals"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                // the configuration needs to be 'lucky-search'
                .andExpect(jsonPath("$.configuration", is("lucky-search")))
                // the type needs to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                // the filter needs to be 'orcid'
                .andExpect(jsonPath("$.appliedFilters[0].filter", is("orcid")))
                .andExpect(jsonPath("$.appliedFilters[0].value", is("0000-0002-5497-1234")))
                .andExpect(jsonPath("$.appliedFilters[0].label", is("0000-0002-5497-1234")))
                .andExpect(jsonPath("$.appliedFilters[0].operator", is("equals")))
                // only one item has this orcid then expect total elements equal to 1
                .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(1)))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]" +
                        "._embedded.indexableObject.id", is(publicItem2.getID().toString())))
                // the entity type must be person
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]" +
                        "._embedded.indexableObject.entityType", is("Person")))
                .andExpect(jsonPath("$._links.next").doesNotExist())
                // there always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/search/objects?configuration=" +
                        "lucky-search&f.orcid=0000-0002-5497-1234,equals")))
                // pagination
                .andExpect(jsonPath("$._embedded.searchResult.page",
                        is(PageMatcher.pageEntry(0, 10))));
        // search anonymous
        getClient().perform(get("/api/discover/search/objects")
                        .param("size", "10")
                        .param("page", "0")
                        .param("configuration", "lucky-search")
                        .param("f.orcid", "0000-0002-5497-4567,equals"))
                //** THEN **
                // the status has to be 200 OK
                .andExpect(status().isOk())
                // the configuration needs to be 'lucky-search'
                .andExpect(jsonPath("$.configuration", is("lucky-search")))
                // the type needs to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                // the filter needs to be 'orcid'
                .andExpect(jsonPath("$.appliedFilters[0].filter", is("orcid")))
                .andExpect(jsonPath("$.appliedFilters[0].value", is("0000-0002-5497-4567")))
                .andExpect(jsonPath("$.appliedFilters[0].label", is("0000-0002-5497-4567")))
                .andExpect(jsonPath("$.appliedFilters[0].operator", is("equals")))
                // only one item has this orcid then expect total elements equal to 1
                .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(1)))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]" +
                        "._embedded.indexableObject.id", is(publicItem3.getID().toString())))
                // the entity type must be person
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]" +
                        "._embedded.indexableObject.entityType", is("Person")))
                .andExpect(jsonPath("$._links.next").doesNotExist())
                // there always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/search/" +
                        "objects?configuration=lucky-search&f.orcid=0000-0002-5497-4567,equals")))
                // pagination
                .andExpect(jsonPath("$._embedded.searchResult.page",
                        is(PageMatcher.pageEntry(0, 10))));
    }
    @Test
    public void discoverOrcidSearchLuckySearchConfigurationMultipleResults() throws Exception {
        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        //1. A community-collection structure with one parent community with sub-community and two collections.
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        //2. Three public items that are readable by Anonymous with same orcid
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withFullName("Public item 1")
                .withIssueDate("2017-10-17")
                .withEntityType("Person")
                .withOrcidIdentifier("0000-0002-5497-7736")
                .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                .withFullName("Public item 2")
                .withIssueDate("2016-02-13")
                .withEntityType("Person")
                .withOrcidIdentifier("0000-0002-5497-7736")
                .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                .withFullName("Public item 3")
                .withIssueDate("2016-02-13")
                .withEntityType("Person")
                .withOrcidIdentifier("0000-0002-5497-7736")
                .build();


        context.restoreAuthSystemState();
        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system and enters a size of 10
        getClient().perform(get("/api/discover/search/objects")
                        .param("size", "10")
                        .param("page", "0")
                        .param("configuration", "lucky-search")
                        .param("f.orcid", "0000-0002-5497-7736,equals"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                // the configuration needs to be 'lucky-search'
                .andExpect(jsonPath("$.configuration", is("lucky-search")))
                // the type needs to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                .andExpect(jsonPath("$.appliedFilters[0].filter", is("orcid")))
                .andExpect(jsonPath("$.appliedFilters[0].value", is("0000-0002-5497-7736")))
                .andExpect(jsonPath("$.appliedFilters[0].label", is("0000-0002-5497-7736")))
                .andExpect(jsonPath("$.appliedFilters[0].operator", is("equals")))
                // the total elements need to be three
                .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(3)))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[*]._embedded" +
                    ".indexableObject.id", containsInAnyOrder(publicItem1.getID().toString(),
                        publicItem2.getID().toString(), publicItem3.getID().toString())))
                // the entity type must be person
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]" +
                        "._embedded.indexableObject.entityType", is("Person")))
                .andExpect(jsonPath("$._links.next").doesNotExist())
                // there always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/search/objects" +
                        "?configuration=lucky-search&f.orcid=0000-0002-5497-7736,equals")))
                // pagination
                .andExpect(jsonPath("$._embedded.searchResult.page",
                        is(PageMatcher.pageEntry(0, 10))));


    }
    @Test
    public void discoverDoiSearchLuckySearchConfigurationMultipleResults() throws Exception {
        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A parent community
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        //2. A community-collection structure with one parent community with sub-community and two collections.
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        //2. Three public items that are readable by Anonymous with same doi
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withFullName("Public item 1")
                .withIssueDate("2017-10-17")
                .withEntityType("Person")
                .withDoiIdentifier("10.1016/j.procs.2017.03.038")
                .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                .withFullName("Public item 2")
                .withIssueDate("2016-02-13")
                .withEntityType("Person")
                .withDoiIdentifier("10.1016/j.procs.2017.03.038")
                .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                .withFullName("Public item 3")
                .withIssueDate("2016-02-13")
                .withEntityType("Person")
                .withDoiIdentifier("10.1016/j.procs.2017.03.038")
                .build();


        context.restoreAuthSystemState();
        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system and enters a size of 10
        getClient().perform(get("/api/discover/search/objects")
                        .param("size", "10")
                        .param("page", "0")
                        .param("configuration", "lucky-search")
                        .param("f.doi", "10.1016/j.procs.2017.03.038,equals"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                // the configuration needs to be 'lucky-search'
                .andExpect(jsonPath("$.configuration", is("lucky-search")))
                // the type needs to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                .andExpect(jsonPath("$.appliedFilters[0].filter", is("doi")))
                .andExpect(jsonPath("$.appliedFilters[0].value", is("10.1016/j.procs.2017.03.038")))
                .andExpect(jsonPath("$.appliedFilters[0].label", is("10.1016/j.procs.2017.03.038")))
                .andExpect(jsonPath("$.appliedFilters[0].operator", is("equals")))
                // the size of total elements needs to be three as there are three items found from search
                .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(3)))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[*]._embedded" +
                        ".indexableObject.id", containsInAnyOrder(publicItem1.getID().toString(),
                            publicItem2.getID().toString(), publicItem3.getID().toString())))
                // the entity type must be person
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]._embedded" +
                        ".indexableObject.entityType", is("Person")))
                .andExpect(jsonPath("$._links.next").doesNotExist())
                // there always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/search/objects" +
                        "?configuration=lucky-search&f.doi=10.1016/j.procs.2017.03.038,equals")))
                // pagination
                .andExpect(jsonPath("$._embedded.searchResult.page",
                        is(PageMatcher.pageEntry(0, 10))));


    }
    @Test
    public void discoverDoiSearchLuckySearchConfigurationOneResult() throws Exception {
        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A parent community.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        //2. A community-collection structure with one parent community with sub-community and two collections.
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        //2. Three public items that are readable by Anonymous with different orcid
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withFullName("Public item 1")
                .withIssueDate("2017-10-17")
                .withEntityType("Person")
                .withDoiIdentifier("10.1016/j.procs.2017.03.031")
                .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                .withFullName("Public item 2")
                .withIssueDate("2016-02-13")
                .withEntityType("Person")
                .withDoiIdentifier("10.1016/j.procs.2017.03.032")
                .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                .withFullName("Public item 3")
                .withIssueDate("2016-02-13")
                .withEntityType("Person")
                .withDoiIdentifier("10.1016/j.procs.2017.03.033")
                .build();

        context.restoreAuthSystemState();
        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system and enters a size of 10
        getClient().perform(get("/api/discover/search/objects")
                        .param("size", "10")
                        .param("page", "0")
                        .param("configuration", "lucky-search")
                        .param("f.doi", "10.1016/j.procs.2017.03.031,equals"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                // the configuration needs to be 'lucky-search'
                .andExpect(jsonPath("$.configuration", is("lucky-search")))
                // the type needs to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                .andExpect(jsonPath("$.appliedFilters[0].filter", is("doi")))
                .andExpect(jsonPath("$.appliedFilters[0].value", is("10.1016/j.procs.2017.03.031")))
                .andExpect(jsonPath("$.appliedFilters[0].label", is("10.1016/j.procs.2017.03.031")))
                .andExpect(jsonPath("$.appliedFilters[0].operator", is("equals")))
                // only one item has this doi then expect total elements equal to 1
                .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(1)))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]" +
                        "._embedded.indexableObject.id", is(publicItem1.getID().toString())))
                // the entity type must be person
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]" +
                        "._embedded.indexableObject.entityType", is("Person")))
                .andExpect(jsonPath("$._links.next").doesNotExist())
                // there always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/search/objects" +
                        "?configuration=lucky-search&f.doi=10.1016/j.procs.2017.03.031,equals")))
                // pagination
                .andExpect(jsonPath("$._embedded.searchResult.page",
                        is(PageMatcher.pageEntry(0, 10))));
        //An anonymous user browses this endpoint to find the objects in the system and enters a size of 10
        getClient().perform(get("/api/discover/search/objects")
                        .param("size", "10")
                        .param("page", "0")
                        .param("configuration", "lucky-search")
                        .param("f.doi", "10.1016/j.procs.2017.03.032,equals"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                // the configuration needs to be 'lucky-search'
                .andExpect(jsonPath("$.configuration", is("lucky-search")))
                // the type needs to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                // the filter needs to be 'doi'
                .andExpect(jsonPath("$.appliedFilters[0].filter", is("doi")))
                .andExpect(jsonPath("$.appliedFilters[0].value", is("10.1016/j.procs.2017.03.032")))
                .andExpect(jsonPath("$.appliedFilters[0].label", is("10.1016/j.procs.2017.03.032")))
                .andExpect(jsonPath("$.appliedFilters[0].operator", is("equals")))
                // only one item has this doi then expect total elements equal to 1
                .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(1)))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]" +
                        "._embedded.indexableObject.id", is(publicItem2.getID().toString())))
                // the entity type must be person
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]" +
                        "._embedded.indexableObject.entityType", is("Person")))
                .andExpect(jsonPath("$._links.next").doesNotExist())
                // there always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/search/objects" +
                        "?configuration=lucky-search&f.doi=10.1016/j.procs.2017.03.032,equals")))
                // pagination
                .andExpect(jsonPath("$._embedded.searchResult.page",
                        is(PageMatcher.pageEntry(0, 10))));
        // search anonymous
        getClient().perform(get("/api/discover/search/objects")
                        .param("size", "10")
                        .param("page", "0")
                        .param("configuration", "lucky-search")
                        .param("f.doi", "10.1016/j.procs.2017.03.033,equals"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                // the configuration needs to be 'lucky-search'
                .andExpect(jsonPath("$.configuration", is("lucky-search")))
                // the type needs to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                // the filter needs to be 'doi'
                .andExpect(jsonPath("$.appliedFilters[0].filter", is("doi")))
                .andExpect(jsonPath("$.appliedFilters[0].value", is("10.1016/j.procs.2017.03.033")))
                .andExpect(jsonPath("$.appliedFilters[0].label", is("10.1016/j.procs.2017.03.033")))
                .andExpect(jsonPath("$.appliedFilters[0].operator", is("equals")))
                // only one item has this doi then expect total elements equal to 1
                .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(1)))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]._embedded" +
                        ".indexableObject.id", is(publicItem3.getID().toString())))
                // the entity type must be person
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]._embedded" +
                        ".indexableObject.entityType", is("Person")))
                .andExpect(jsonPath("$._links.next").doesNotExist())
                // there always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/search/objects" +
                        "?configuration=lucky-search&f.doi=10.1016/j.procs.2017.03.033,equals")))
                // pagination
                .andExpect(jsonPath("$._embedded.searchResult.page",
                        is(PageMatcher.pageEntry(0, 10))));
    }
    @Test
    public void discoverDoiSearchLuckySearchConfigurationNoResult() throws Exception {
        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A parent community
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        //2. A community-collection structure with one parent community with sub-community and two collections.
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        //3. Three public items that are readable by Anonymous with different doi
        ItemBuilder.createItem(context, col1)
                .withFullName("Public item 1")
                .withIssueDate("2017-10-17")
                .withEntityType("Person")
                .withDoiIdentifier("10.1016/j.procs.2017.03.031")
                .build();

        ItemBuilder.createItem(context, col2)
                .withFullName("Public item 2")
                .withIssueDate("2016-02-13")
                .withEntityType("Person")
                .withDoiIdentifier("10.1016/j.procs.2017.03.032")
                .build();

        ItemBuilder.createItem(context, col2)
                .withFullName("Public item 3")
                .withIssueDate("2016-02-13")
                .withEntityType("Person")
                .withDoiIdentifier("10.1016/j.procs.2017.03.033")
                .build();

        context.restoreAuthSystemState();
        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system and enters a size of 10
        getClient().perform(get("/api/discover/search/objects")
                        .param("size", "10")
                        .param("page", "0")
                        .param("configuration", "lucky-search")
                        .param("f.doi", "10.1016/j.procs.2017.03.035,equals"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                // the configuration needs to be 'lucky-search'
                .andExpect(jsonPath("$.configuration", is("lucky-search")))
                // the type needs to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                // the filter needs to be 'doi'
                .andExpect(jsonPath("$.appliedFilters[0].filter", is("doi")))
                .andExpect(jsonPath("$.appliedFilters[0].value", is("10.1016/j.procs.2017.03.035")))
                .andExpect(jsonPath("$.appliedFilters[0].label", is("10.1016/j.procs.2017.03.035")))
                .andExpect(jsonPath("$.appliedFilters[0].operator", is("equals")))
                // expect 0 as there are no items with this doi
                .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(0)))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", hasSize(0)))
                .andExpect(jsonPath("$._links.next").doesNotExist())
                // there always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/search/objects" +
                        "?configuration=lucky-search&f.doi=10.1016/j.procs.2017.03.035,equals")))
                // pagination
                .andExpect(jsonPath("$._embedded.searchResult.page",
                        is(PageMatcher.pageEntry(0, 10))));

    }
    @Test
    public void discoverOrcidSearchLuckySearchConfigurationNoResult() throws Exception {
        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        //2. Three public items that are readable by Anonymous with different orcid
        ItemBuilder.createItem(context, col1)
                .withFullName("Public item 1")
                .withIssueDate("2017-10-17")
                .withEntityType("Person")
                .withOrcidIdentifier("0000-0002-5497-7736")
                .build();

        ItemBuilder.createItem(context, col2)
                .withFullName("Public item 2")
                .withIssueDate("2016-02-13")
                .withEntityType("Person")
                .withOrcidIdentifier("0000-0002-5497-1234")
                .build();

        ItemBuilder.createItem(context, col2)
                .withFullName("Public item 3")
                .withIssueDate("2016-02-13")
                .withEntityType("Person")
                .withOrcidIdentifier("0000-0002-5497-5567")
                .build();

        context.restoreAuthSystemState();
        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system and enters a size of 10
        getClient().perform(get("/api/discover/search/objects")
                        .param("size", "10")
                        .param("page", "0")
                        .param("configuration", "lucky-search")
                        .param("f.orcid", "0000-0002-5497-1200,equals"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                // the configuration needs to be 'lucky-search'
                .andExpect(jsonPath("$.configuration", is("lucky-search")))
                // the type needs to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                // the filter needs to be 'orcid'
                .andExpect(jsonPath("$.appliedFilters[0].filter", is("orcid")))
                .andExpect(jsonPath("$.appliedFilters[0].value", is("0000-0002-5497-1200")))
                .andExpect(jsonPath("$.appliedFilters[0].label", is("0000-0002-5497-1200")))
                .andExpect(jsonPath("$.appliedFilters[0].operator", is("equals")))
                // total elements need to be 0 as there are not created items with the orcid searched
                .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(0)))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", hasSize(0)))
                .andExpect(jsonPath("$._links.next").doesNotExist())
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/search/objects" +
                        "?configuration=lucky-search&f.orcid=0000-0002-5497-1200,equals")))
                // pagination
                .andExpect(jsonPath("$._embedded.searchResult.page",
                        is(PageMatcher.pageEntry(0, 10))));

    }
    @Test
    public void discoverLuckySearchConfigurationNoSupportedFilter() throws Exception {
        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A parent community
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        //2. A community-collection structure with one parent community with sub-community and two collections.
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        context.restoreAuthSystemState();
        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system and enters a size of 10
        getClient().perform(get("/api/discover/search/objects")
                        .param("size", "10")
                        .param("page", "0")
                        .param("configuration", "lucky-search")
                        .param("f.justfortest", "1234,equals"))
                //** THEN **
                //The status has to be 400 Bad Request as the filter does not exists
                .andExpect(status().isBadRequest());

    }

    @Test
    public void discoverLegacyIdSearchLuckySearchConfigurationOneResult() throws Exception {
        // Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();
        // ** GIVEN **
        // parent community with two collections
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();
        // Three public items that are readable by Anonymous with different orcid
        Item publicItem1 = ItemBuilder.createItem(context, col1)
            .withFullName("Public item 1")
            .withIssueDate("2017-10-17")
            .withEntityType("Person")
            .withLegacyId("rp0001")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withFullName("Public item 2")
            .withIssueDate("2016-02-13")
            .withEntityType("OrgUnit")
            .withLegacyId("rp0002")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withFullName("Public item 3")
            .withIssueDate("2016-02-13")
            .withEntityType("Project")
            .withLegacyId("rp0003")
            .build();

        Item publicItem4 = ItemBuilder.createItem(context, col2)
            .withFullName("Public item 3")
            .withIssueDate("2016-02-13")
            .withEntityType("Journal")
            .withLegacyId("rp0004")
            .build();

        context.restoreAuthSystemState();
        // ** WHEN **
        // An anonymous user browses this endpoint to find the objects in the system and
        // enters a size of 10
        getClient().perform(get("/api/discover/search/objects")
            .param("size", "10")
            .param("page", "0")
            .param("configuration", "lucky-search")
            .param("f.legacy-id", "rp0001,equals"))
            // ** THEN **
            // The status has to be 200 OK
            .andExpect(status().isOk())
            // the configuration needs to be 'lucky-search'
            .andExpect(jsonPath("$.configuration", is("lucky-search")))
            // the type needs to be 'discover'
            .andExpect(jsonPath("$.type", is("discover")))
            .andExpect(jsonPath("$.appliedFilters[0].filter", is("legacy-id")))
            .andExpect(jsonPath("$.appliedFilters[0].value", is("rp0001")))
            .andExpect(jsonPath("$.appliedFilters[0].label", is("rp0001")))
            .andExpect(jsonPath("$.appliedFilters[0].operator", is("equals")))
            // total elements needs to be 1 as there is only one item with this legacy-id
            // searched
            .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(1)))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]._embedded.indexableObject.id",
                is(publicItem1.getID().toString())))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]" +
                "._embedded.indexableObject.entityType", is("Person")))
            .andExpect(jsonPath("$._links.next").doesNotExist())
            // there always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("api/discover/search/objects?" +
                "configuration=lucky-search&f.legacy-id=rp0001,equals")))
            // pagination
            .andExpect(jsonPath("$._embedded.searchResult.page",
                is(PageMatcher.pageEntry(0, 10))));

        getClient().perform(get("/api/discover/search/objects")
            .param("size", "10")
            .param("page", "0")
            .param("configuration", "lucky-search")
            .param("f.legacy-id", "rp0002,equals"))
            // ** THEN **
            // The status has to be 200 OK
            .andExpect(status().isOk())
            // the configuration needs to be 'lucky-search'
            .andExpect(jsonPath("$.configuration", is("lucky-search")))
            // the type needs to be 'discover'
            .andExpect(jsonPath("$.type", is("discover")))
            .andExpect(jsonPath("$.appliedFilters[0].filter", is("legacy-id")))
            .andExpect(jsonPath("$.appliedFilters[0].value", is("rp0002")))
            .andExpect(jsonPath("$.appliedFilters[0].label", is("rp0002")))
            .andExpect(jsonPath("$.appliedFilters[0].operator", is("equals")))
            // total elements needs to be 1 as there is only one item with this legacy-id
            // searched
            .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(1)))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]._embedded.indexableObject.id",
                is(publicItem2.getID().toString())))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]" +
                "._embedded.indexableObject.entityType", is("OrgUnit")))
            .andExpect(jsonPath("$._links.next").doesNotExist())
            // there always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("api/discover/search/objects?configuration=" +
                "lucky-search&f.legacy-id=rp0002,equals")))
            // pagination
            .andExpect(jsonPath("$._embedded.searchResult.page",
                is(PageMatcher.pageEntry(0, 10))));

        getClient().perform(get("/api/discover/search/objects")
            .param("size", "10")
            .param("page", "0")
            .param("configuration", "lucky-search")
            .param("f.legacy-id", "rp0003,equals"))
            // ** THEN **
            // The status has to be 200 OK
            .andExpect(status().isOk())
            // the configuration needs to be 'lucky-search'
            .andExpect(jsonPath("$.configuration", is("lucky-search")))
            // the type needs to be 'discover'
            .andExpect(jsonPath("$.type", is("discover")))
            .andExpect(jsonPath("$.appliedFilters[0].filter", is("legacy-id")))
            .andExpect(jsonPath("$.appliedFilters[0].value", is("rp0003")))
            .andExpect(jsonPath("$.appliedFilters[0].label", is("rp0003")))
            .andExpect(jsonPath("$.appliedFilters[0].operator", is("equals")))
            // total elements needs to be 1 as there is only one item with this legacy-id
            // searched
            .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(1)))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]._embedded.indexableObject.id",
                is(publicItem3.getID().toString())))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]" +
                "._embedded.indexableObject.entityType", is("Project")))
            .andExpect(jsonPath("$._links.next").doesNotExist())
            // there always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("api/discover/search/" +
                "objects?configuration=lucky-search&f.legacy-id=rp0003,equals")))
            // pagination
            .andExpect(jsonPath("$._embedded.searchResult.page",
                is(PageMatcher.pageEntry(0, 10))));
        getClient().perform(get("/api/discover/search/objects")
            .param("size", "10")
            .param("page", "0")
            .param("configuration", "lucky-search")
            .param("f.legacy-id", "rp0004,equals"))
            // ** THEN **
            // The status has to be 200 OK
            .andExpect(status().isOk())
            // the configuration needs to be 'lucky-search'
            .andExpect(jsonPath("$.configuration", is("lucky-search")))
            // the type needs to be 'discover'
            .andExpect(jsonPath("$.type", is("discover")))
            .andExpect(jsonPath("$.appliedFilters[0].filter", is("legacy-id")))
            .andExpect(jsonPath("$.appliedFilters[0].value", is("rp0004")))
            .andExpect(jsonPath("$.appliedFilters[0].label", is("rp0004")))
            .andExpect(jsonPath("$.appliedFilters[0].operator", is("equals")))
            // total elements needs to be 1 as there is only one item with this legacy-id
            // searched
            .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(1)))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]._embedded.indexableObject.id",
                is(publicItem4.getID().toString())))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects[0]" +
                "._embedded.indexableObject.entityType", is("Journal")))
            .andExpect(jsonPath("$._links.next").doesNotExist())
            // there always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("api/discover/search/" +
                "objects?configuration=lucky-search&f.legacy-id=rp0004,equals")))
            // pagination
            .andExpect(jsonPath("$._embedded.searchResult.page",
                is(PageMatcher.pageEntry(0, 10))));
    }
}
