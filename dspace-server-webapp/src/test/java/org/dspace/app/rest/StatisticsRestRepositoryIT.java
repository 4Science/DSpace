/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static java.io.InputStream.nullInputStream;
import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.dspace.app.rest.matcher.UsageReportMatcher.matchUsageReport;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_CATEGORIES_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_CITIES_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_CITIES_REPORT_ID_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_CITIES_REPORT_ID_RELATION_PERSON_PROJECTS;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_CITIES_REPORT_ID_RELATION_PERSON_RESEARCHOUTPUTS;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_CONTINENTS_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_COUNTRIES_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_COUNTRIES_REPORT_ID_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_COUNTRIES_REPORT_ID_RELATION_PERSON_PROJECTS;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_COUNTRIES_REPORT_ID_RELATION_PERSON_RESEARCHOUTPUTS;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_DOWNLOAD_CITIES_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_DOWNLOAD_CONTINENTS_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_DOWNLOAD_COUNTRIES_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_ITEMS_CATEGORIES_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_ITEMS_CITIES_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_ITEMS_CONTINENTS_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_ITEMS_COUNTRIES_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_ITEMS_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOP_ITEMS_REPORT_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS;
import static org.dspace.app.rest.utils.UsageReportUtils.TOTAL_DOWNLOADS_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOTAL_DOWNLOADS_REPORT_ID_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS;
import static org.dspace.app.rest.utils.UsageReportUtils.TOTAL_DOWNLOAD_PER_MONTH_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOTAL_ITEMS_VISITS_PER_MONTH_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOTAL_ITEMS_VISITS_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOTAL_VISITS_PER_MONTH_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOTAL_VISITS_PER_MONTH_REPORT_ID_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS;
import static org.dspace.app.rest.utils.UsageReportUtils.TOTAL_VISITS_PER_MONTH_REPORT_ID_RELATION_PERSON_PROJECTS;
import static org.dspace.app.rest.utils.UsageReportUtils.TOTAL_VISITS_PER_MONTH_REPORT_ID_RELATION_PERSON_RESEARCHOUTPUTS;
import static org.dspace.app.rest.utils.UsageReportUtils.TOTAL_VISITS_REPORT_ID;
import static org.dspace.app.rest.utils.UsageReportUtils.TOTAL_VISITS_REPORT_ID_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS;
import static org.dspace.app.rest.utils.UsageReportUtils.TOTAL_VISITS_REPORT_ID_RELATION_PERSON_PROJECTS;
import static org.dspace.app.rest.utils.UsageReportUtils.TOTAL_VISITS_REPORT_ID_RELATION_PERSON_RESEARCHOUTPUTS;
import static org.dspace.app.rest.utils.UsageReportUtils.TOTAL_VISITS_TOTAL_DOWNLOADS;
import static org.dspace.app.rest.utils.UsageReportUtils.TOTAL_VISITS_TOTAL_DOWNLOADS_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS;
import static org.dspace.util.FunctionalUtils.throwingConsumerWrapper;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.matcher.UsageReportMatcher;
import org.dspace.app.rest.model.UsageReportPointCategoryRest;
import org.dspace.app.rest.model.UsageReportPointCityRest;
import org.dspace.app.rest.model.UsageReportPointContinentRest;
import org.dspace.app.rest.model.UsageReportPointCountryRest;
import org.dspace.app.rest.model.UsageReportPointDateRest;
import org.dspace.app.rest.model.UsageReportPointDsoTotalVisitsRest;
import org.dspace.app.rest.model.UsageReportPointRest;
import org.dspace.app.rest.model.ViewEventRest;
import org.dspace.app.rest.repository.StatisticsRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.UsageReportUtils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.builder.SiteBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.EventService;
import org.dspace.services.model.Event;
import org.dspace.services.model.EventListener;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.util.MultiFormatDateParser;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * Integration test to test the /api/statistics/usagereports/ endpoints, see {@link UsageReportUtils} and
 * {@link StatisticsRestRepository}
 *
 * @author Maria Verdonck (Atmire) on 10/06/2020
 */
public class StatisticsRestRepositoryIT extends AbstractControllerIntegrationTest {

    protected final StatisticsEventListener statisticsEventListener = new StatisticsEventListener();

    @Autowired
    ConfigurationService configurationService;
    @Autowired
    protected AuthorizeService authorizeService;
    @Autowired
    protected EventService eventService;

    private Community communityNotVisited;
    private Community communityVisited;
    private Collection collectionNotVisited;
    private Collection collectionVisited;
    private Item itemNotVisitedWithBitstreams;
    private Item itemVisited;
    private Bitstream bitstreamNotVisited;
    private Bitstream bitstreamVisited;
    private Item person;
    private Item orgUnit;
    private Item publicationVisited1;
    private Item publicationVisited2;
    private Bitstream bitstreampublication_first;
    private Bitstream bitstreampublication_second;
    private String loggedInToken;
    private String adminToken;

    @BeforeClass
    public static void clearStatistics() throws Exception {
        // To ensure these tests start "fresh", clear out any existing statistics data.
        // NOTE: this is committed immediately in removeIndex()
        StatisticsServiceFactory.getInstance().getSolrLoggerService().removeIndex("*:*");
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Explicitly use solr commit in SolrLoggerServiceImpl#postView
        configurationService.setProperty("solr-statistics.autoCommit", false);
        configurationService.setProperty("usage-statistics.authorization.admin.usage", true);

        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context).build();
        communityNotVisited = CommunityBuilder.createSubCommunity(context, community).build();
        communityVisited = CommunityBuilder.createSubCommunity(context, community).build();
        collectionNotVisited = CollectionBuilder.createCollection(context, community).build();
        collectionVisited = CollectionBuilder.createCollection(context, community).build();
        itemVisited = ItemBuilder.createItem(context, collectionNotVisited).build();
        itemNotVisitedWithBitstreams = ItemBuilder.createItem(context, collectionNotVisited).build();
        bitstreamNotVisited = BitstreamBuilder.createBitstream(context,
                itemNotVisitedWithBitstreams,
                toInputStream("test", UTF_8))
            .withName("BitstreamNotVisitedName").build();
        bitstreamVisited = BitstreamBuilder
                .createBitstream(context, itemNotVisitedWithBitstreams, toInputStream("test", UTF_8))
                .withName("BitstreamVisitedName").build();

        EntityTypeBuilder.createEntityTypeBuilder(context, "OrgUnit").build();
        EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        //orgUnit
        orgUnit = ItemBuilder.createItem(context, collectionVisited)
                             .withEntityType("OrgUnit").withFullName("4Science")
                             .withTitle("4Science").build();
        //person item for relation inverse
        //it has as affiliation 4Science
        person = ItemBuilder.createItem(context, collectionVisited)
                            .withEntityType("Person").withFullName("testPerson")
                            .withTitle("testPerson")
                            .withAffiliation(orgUnit.getName(), orgUnit.getID().toString()).build();
        //first publication for person item
        publicationVisited1 = ItemBuilder.createItem(context, collectionVisited)
                                         .withEntityType("Publication")
                                         .withTitle("publicationVisited1")
                                         .withAuthor(person.getName(), person.getID().toString())
                                         .build();
        //second publication for person item
        publicationVisited2 = ItemBuilder.createItem(context, collectionVisited)
                                         .withEntityType("Publication")
                                         .withTitle("publicationVisited2")
                                         .withAuthor(person.getName(), person.getID().toString())
                                         .build();
        //bitstream for first publication of person
        bitstreampublication_first = BitstreamBuilder
            .createBitstream(context, publicationVisited1,
                toInputStream("test", UTF_8))
            .withName("bitstream1")
                .build();
        //bitstream for second publication of person
        bitstreampublication_second = BitstreamBuilder
            .createBitstream(context, publicationVisited2,
                toInputStream("test", UTF_8))
            .withName("bitstream2")
                .build();

        loggedInToken = getAuthToken(eperson.getEmail(), password);
        adminToken = getAuthToken(admin.getEmail(), password);

        this.eventService.registerEventListener(this.statisticsEventListener);

        context.restoreAuthSystemState();
    }

    @Test
    public void usagereports_withoutId_NotImplementedException() throws Exception {
        getClient().perform(get("/api/statistics/usagereports"))
                .andExpect(status().is(HttpStatus.METHOD_NOT_ALLOWED.value()));
    }

    @Test
    public void usagereports_notProperUUIDAndReportId_Exception() throws Exception {
        getClient(adminToken).perform(get("/api/statistics/usagereports/notProperUUIDAndReportId"))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void usagereports_nonValidUUIDpart_Exception() throws Exception {
        getClient(adminToken).perform(get("/api/statistics/usagereports/notAnUUID" + "_" + TOTAL_VISITS_REPORT_ID))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void usagereports_nonValidReportIDpart_Exception() throws Exception {
        getClient(adminToken).perform(get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() +
                "_NotValidReport"))
            .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    public void usagereports_nonValidReportIDpart_Exception_By_Anonymous_Unauthorized_Test() throws Exception {
        getClient().perform(get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() +
                "_NotValidReport"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void usagereports_nonValidReportIDpart_Exception_By_Anonymous_Test() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.usage", false);
        getClient().perform(get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() +
                "_NotValidReport"))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void usagereports_NonExistentUUID_Exception() throws Exception {
        getClient(adminToken).perform(
                get("/api/statistics/usagereports/" + UUID.randomUUID() + "_" + TOTAL_VISITS_REPORT_ID))
            .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    public void usagereport_onlyAdminReadRights() throws Exception {
        // ** WHEN **
        authorizeService.removeAllPolicies(context, itemNotVisitedWithBitstreams);
        // We request a dso's TotalVisits usage stat report as anon but dso has no read policy for anon
        getClient().perform(
                get(
                    "/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() +
                        "_" + TOTAL_VISITS_REPORT_ID))
                // ** THEN **
                .andExpect(status().isUnauthorized());
        // We request a dso's TotalVisits usage stat report as admin
        getClient(adminToken).perform(
                get("/api/statistics/usagereports/" +
                    itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                // ** THEN **
                .andExpect(status().isOk());
    }

    @Test
    public void usagereport_onlyAdminReadRights_unvalidToken() throws Exception {
        // ** WHEN **
        authorizeService.removeAllPolicies(context, itemNotVisitedWithBitstreams);
        // We request a dso's TotalVisits usage stat report with unvalid token
        getClient("unvalidToken").perform(
            get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                 // ** THEN **
                 .andExpect(status().isUnauthorized());
    }

    @Test
    public void usagereport_loggedInUserReadRights() throws Exception {
        // ** WHEN **
        context.turnOffAuthorisationSystem();
        authorizeService.removeAllPolicies(context, itemNotVisitedWithBitstreams);
        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
                             .withDspaceObject(itemNotVisitedWithBitstreams)
                             .withAction(Constants.READ).build();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
                .withEmail("eperson1@mail.com")
                .withPassword(password)
                .build();
        context.restoreAuthSystemState();
        String anotherLoggedInUserToken = getAuthToken(eperson1.getEmail(), password);
        // We request a dso's TotalVisits usage stat report as anon but dso has no read policy for anon
        getClient().perform(
                get("/api/statistics/usagereports/" +
                    itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                // ** THEN **
                .andExpect(status().isUnauthorized());
        // We request a dso's TotalVisits usage stat report as logged in eperson and has read policy for this user
        getClient(loggedInToken).perform(
            get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                // ** THEN **
                .andExpect(status().isForbidden());
        // We request a dso's TotalVisits usage stat report as another logged in eperson and has no read policy for
        // this user
        getClient(anotherLoggedInUserToken).perform(
                get("/api/statistics/usagereports/" +
                    itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                // ** THEN **
                .andExpect(status().isForbidden());
    }

    @Test
    public void usagereport_loggedInUserReadRights_and_usage_statistics_admin_is_false_Test() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.usage", false);
        context.turnOffAuthorisationSystem();
        authorizeService.removeAllPolicies(context, itemNotVisitedWithBitstreams);
        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
                             .withDspaceObject(itemNotVisitedWithBitstreams)
                             .withAction(Constants.READ).build();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
                                         .withEmail("eperson1@mail.com")
                                         .withPassword(password)
                                         .build();
        context.restoreAuthSystemState();
        String anotherLoggedInUserToken = getAuthToken(eperson1.getEmail(), password);
        // We request a dso's TotalVisits usage stat report as anon but dso has no read policy for anon
        getClient().perform(get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() + "_" +
                TOTAL_VISITS_REPORT_ID))
                   .andExpect(status().isUnauthorized());

        // We request a dso's TotalVisits usage stat report as logged in eperson and has read policy for this user
        getClient(loggedInToken).perform(get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() +
                "_" + TOTAL_VISITS_REPORT_ID))
                                .andExpect(status().isOk());

        // We request a dso's TotalVisits usage stat report as another logged
        // in eperson and has no read policy for this user
        getClient(anotherLoggedInUserToken).perform(
            get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID))
               .andExpect(status().isForbidden());
    }

    @Test
    public void totalVisitsReport_Community_Visited() throws Exception {
        // ** WHEN **
        // We visit the community
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("community");
        viewEventRest.setTargetId(communityVisited.getID());

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {
                // And request that community's TotalVisits stat report
                getClient(adminToken).perform(
                        get("/api/statistics/usagereports/" + communityVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                    // ** THEN **
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        UsageReportMatcher.matchUsageReport(
                            communityVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID,
                            List.of(
                                getExpectedDsoViews(communityVisited, 1)
                            )
                        )
                    )));
            }));

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
                .andExpect(status().isCreated());
    }

    @Test
    public void totalVisitsReport_Community_NotVisited() throws Exception {
        // ** WHEN **
        // Community is never visited
        // And request that community's TotalVisits stat report
        getClient(adminToken).perform(
                get("/api/statistics/usagereports/" + communityNotVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.is(
                UsageReportMatcher.matchUsageReport(
                    communityNotVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                    TOTAL_VISITS_REPORT_ID,
                    List.of(
                        getExpectedDsoViews(communityNotVisited, 0)
                    )
                )
            )));
    }

    @Test
    public void totalVisitsReport_Collection_Visited() throws Exception {
        // ** WHEN **
        // We visit the collection twice
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("collection");
        viewEventRest.setTargetId(collectionVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
                .andExpect(status().isCreated());

        Thread.sleep(1000);

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {
                // And request that collection's TotalVisits stat report
                getClient(adminToken).perform(
                        get("/api/statistics/usagereports/" + collectionVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                    // ** THEN **
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        UsageReportMatcher.matchUsageReport(
                            collectionVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID,
                            List.of(
                                getExpectedDsoViews(collectionVisited, 2)
                            )
                        )
                    )));
            }));

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
                .andExpect(status().isCreated());
    }

    @Test
    public void totalVisitsReport_Collection_NotVisited() throws Exception {
        // ** WHEN **
        // Collection is never visited
        // And request that collection's TotalVisits stat report
        getClient(adminToken).perform(
                get("/api/statistics/usagereports/" + collectionNotVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.is(
                UsageReportMatcher.matchUsageReport(
                    collectionNotVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                    TOTAL_VISITS_REPORT_ID,
                    List.of(
                        getExpectedDsoViews(collectionNotVisited, 0)
                    )
                )
            )));
    }

    @Test
    public void totalVisitsReport_Item_Visited() throws Exception {

        Thread.sleep(1000);

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {
                // And request that collection's TotalVisits stat report
                getClient(adminToken).perform(
                        get("/api/statistics/usagereports/" + itemVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                    // ** THEN **
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        UsageReportMatcher.matchUsageReport(
                            itemVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID,
                            List.of(
                                getExpectedDsoViews(itemVisited, 1)
                            )
                        )
                    )));
            }));

        // ** WHEN **
        // We visit an Item
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("item");
        viewEventRest.setTargetId(itemVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
                .andExpect(status().isCreated());
    }

    @Test
    public void totalVisitsReport_Item_NotVisited() throws Exception {
        // ** WHEN **
        //Item is never visited
        List<UsageReportPointRest> expectedPoints = List.of(
            getExpectedDsoViews(itemNotVisitedWithBitstreams, 0)
        );

        // And request that item's TotalVisits stat report
        getClient(adminToken).perform(
            get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID))
            // ** THEN **
            .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(
                                 UsageReportMatcher.matchUsageReport(
                                     itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                                     TOTAL_VISITS_REPORT_ID,
                                     expectedPoints
                                 )
                             )));

        // only admin access visits report
        getClient(loggedInToken).perform(
            get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                .andExpect(status().isForbidden());

        getClient().perform(
            get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID))
            .andExpect(status().isUnauthorized());

        // make statistics visible to all
        configurationService.setProperty("usage-statistics.authorization.admin.usage", false);

        getClient(loggedInToken).perform(
                get("/api/statistics/usagereports/"
                    + itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(
                    UsageReportMatcher.matchUsageReport(
                        itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                        TOTAL_VISITS_REPORT_ID,
                        expectedPoints
                    )
                )));

        getClient().perform(
                get("/api/statistics/usagereports/"
                    + itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(
                    UsageReportMatcher.matchUsageReport(
                        itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                        TOTAL_VISITS_REPORT_ID,
                        expectedPoints
                    )
                )));
    }

    @Test
    public void totalVisitsReport_Bitstream_Visited() throws Exception {
        // ** WHEN **
        // We visit a Bitstream
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("bitstream");
        viewEventRest.setTargetId(bitstreamVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
                .andExpect(status().isCreated());

        Thread.sleep(1000);
        List<UsageReportPointRest> expectedPoints = List.of(
            getExpectedDsoViews(bitstreamVisited, 1)
        );

        // And request that bitstream's TotalVisits stat report
        getClient(adminToken).perform(
                get("/api/statistics/usagereports/" + bitstreamVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.is(
                UsageReportMatcher.matchUsageReport(
                    bitstreamVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                    TOTAL_VISITS_REPORT_ID,
                    expectedPoints
                )
            )));
        // only admin access visits report
        getClient(loggedInToken).perform(
                get("/api/statistics/usagereports/" + bitstreamVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
            .andExpect(status().isForbidden());

        getClient().perform(
                get("/api/statistics/usagereports/" + bitstreamVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
            .andExpect(status().isUnauthorized());

        // make statistics visible to all
        configurationService.setProperty("usage-statistics.authorization.admin.usage", false);

        getClient(loggedInToken).perform(
                get("/api/statistics/usagereports/" + bitstreamVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(
                    UsageReportMatcher.matchUsageReport(
                        bitstreamVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                        TOTAL_VISITS_REPORT_ID,
                        expectedPoints
                    )
                )));

        getClient().perform(
                get("/api/statistics/usagereports/" + bitstreamVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(
                    UsageReportMatcher.matchUsageReport(
                        bitstreamVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                        TOTAL_VISITS_REPORT_ID,
                        expectedPoints
                    )
                )));
    }

    @Test
    public void totalVisitsReport_Bitstream_NotVisited() throws Exception {
        // ** WHEN **
        // Bitstream is never visited

        List<UsageReportPointRest> expectedPoints = List.of(
            getExpectedDsoViews(bitstreamNotVisited, 0)
        );

        String authToken = getAuthToken(admin.getEmail(), password);
        // And request that bitstream's TotalVisits stat report
        getClient(authToken).perform(
                get("/api/statistics/usagereports/" + bitstreamNotVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.is(
                UsageReportMatcher.matchUsageReport(
                    bitstreamNotVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                    TOTAL_VISITS_REPORT_ID,
                    expectedPoints
                )
            )));

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(
                get("/api/statistics/usagereports/" + bitstreamNotVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
            .andExpect(status().isForbidden());

        getClient().perform(
                get("/api/statistics/usagereports/" + bitstreamNotVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                   .andExpect(status().isUnauthorized());

        // make statistics visible to all
        configurationService.setProperty("usage-statistics.authorization.admin.usage", false);

        getClient(tokenEPerson).perform(
                get("/api/statistics/usagereports/" + bitstreamNotVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(
                    UsageReportMatcher.matchUsageReport(
                        bitstreamNotVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                        TOTAL_VISITS_REPORT_ID,
                        expectedPoints
                    )
                )));

        getClient().perform(
                get("/api/statistics/usagereports/" + bitstreamNotVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(
                    UsageReportMatcher.matchUsageReport(
                        bitstreamNotVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                        TOTAL_VISITS_REPORT_ID,
                        expectedPoints
                    )
                )));
    }

    @Test
    public void totalVisitsPerMonthReport_Item_Visited() throws Exception {

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {
                List<UsageReportPointRest> expectedPoints = getLastMonthVisitPoints(1);

                // And request that item's TotalVisitsPerMonth stat report
                getClient(adminToken).perform(
                        get(
                            "/api/statistics/usagereports/" + itemVisited.getID() +
                                "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID
                        )
                    )
                    // ** THEN **
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        UsageReportMatcher.matchUsageReport(
                            itemVisited.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            expectedPoints
                        )
                    )));

                // only admin has access
                getClient(loggedInToken).perform(
                        get(
                            "/api/statistics/usagereports/" + itemVisited.getID() +
                                "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID
                        )
                    )
                    .andExpect(status().isForbidden());

                getClient().perform(
                        get("/api/statistics/usagereports/" + itemVisited.getID() +
                            "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID))
                    .andExpect(status().isUnauthorized());

                // make statistics visible to all
                configurationService.setProperty("usage-statistics.authorization.admin.usage", false);

                getClient(loggedInToken).perform(
                        get(
                            "/api/statistics/usagereports/" + itemVisited.getID() +
                                "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID
                        )
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        UsageReportMatcher.matchUsageReport(
                            itemVisited.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            expectedPoints
                        )
                    )));

                getClient().perform(
                    get("/api/statistics/usagereports/" + itemVisited.getID() +
                        "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        UsageReportMatcher.matchUsageReport(
                            itemVisited.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            expectedPoints
                        )
                    )));
            }));

        // ** WHEN **
        // We visit an Item
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("item");
        viewEventRest.setTargetId(itemVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
            .andExpect(status().isCreated());
    }

    @Test
    public void totalVisitsPerMonthReport_Item_NotVisited() throws Exception {
        // ** WHEN **
        // Item is not visited
        // And request that item's TotalVisitsPerMonth stat report
        getClient(adminToken).perform(
                get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() + "_" +
                    TOTAL_VISITS_PER_MONTH_REPORT_ID))
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.is(
                UsageReportMatcher.matchUsageReport(
                    itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                    TOTAL_VISITS_PER_MONTH_REPORT_ID,
                    getLastMonthVisitPoints(0)
                )
            )));
    }

    @Test
    public void totalVisitsPerMonthReport_Collection_Visited() throws Exception {
        // ** WHEN **
        // We visit a Collection twice
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("collection");
        viewEventRest.setTargetId(collectionVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
                .andExpect(status().isCreated());

        Thread.sleep(3000);

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {
                // And request that collection's TotalVisitsPerMonth stat report
                getClient(adminToken).perform(
                        get("/api/statistics/usagereports/" + collectionVisited.getID() + "_" +
                            TOTAL_VISITS_PER_MONTH_REPORT_ID))
                    // ** THEN **
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        UsageReportMatcher.matchUsageReport(
                            collectionVisited.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            getLastMonthVisitPoints(2)
                        )
                    )));
            }));

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
                .andExpect(status().isCreated());
    }

    @Test
    public void TotalDownloadsReport_Bitstream() throws Exception {
        // ** WHEN **
        // We visit a Bitstream
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("bitstream");
        viewEventRest.setTargetId(bitstreamVisited.getID());

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {
                List<UsageReportPointRest> expectedPoints = List.of(
                    getExpectedDsoViews(bitstreamVisited, 1)
                );

                // And request that bitstreams's TotalDownloads stat report
                getClient(adminToken).perform(
                    get("/api/statistics/usagereports/" + bitstreamVisited.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID))
                    // ** THEN **
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        UsageReportMatcher.matchUsageReport(
                            bitstreamVisited.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID,
                            TOTAL_DOWNLOADS_REPORT_ID,
                            expectedPoints
                        )
                    )));

                // only admin has access to downloads report
                getClient(loggedInToken).perform(
                    get("/api/statistics/usagereports/" + bitstreamVisited.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID))
                    .andExpect(status().isForbidden());

                getClient().perform(
                    get("/api/statistics/usagereports/" + bitstreamVisited.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID))
                    .andExpect(status().isUnauthorized());

                // make statistics visible to all
                configurationService.setProperty("usage-statistics.authorization.admin.usage", false);

                getClient(loggedInToken).perform(
                    get("/api/statistics/usagereports/" + bitstreamVisited.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        UsageReportMatcher.matchUsageReport(
                            bitstreamVisited.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID,
                            TOTAL_DOWNLOADS_REPORT_ID,
                            expectedPoints
                        )
                    )));

                getClient().perform(
                    get("/api/statistics/usagereports/" + bitstreamVisited.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        UsageReportMatcher.matchUsageReport(
                            bitstreamVisited.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID,
                            TOTAL_DOWNLOADS_REPORT_ID,
                            expectedPoints
                        )
                    )));
            }));
        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
                .andExpect(status().isCreated());
    }

    @Test
    public void TotalDownloadsReport_Item() throws Exception {

        Thread.sleep(1000);

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {
                UsageReportPointDsoTotalVisitsRest expectedPoint = new UsageReportPointDsoTotalVisitsRest();
                expectedPoint.addValue("views", 1);
                expectedPoint.setId(bitstreamVisited.getID().toString());
                expectedPoint.setLabel("BitstreamVisitedName");
                expectedPoint.setType("bitstream");

                // And request that item's TotalDownloads stat report
                getClient(adminToken)
                    .perform(
                        get("/api/statistics/usagereports/" +
                            itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID
                        )
                    )
                    // ** THEN **
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        UsageReportMatcher.matchUsageReport(
                            itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID,
                            TOTAL_DOWNLOADS_REPORT_ID,
                            List.of(
                                getExpectedDsoViews(bitstreamVisited, 1)
                            )
                        )
                    )));
            }));

        // ** WHEN **
        // We visit an Item's bitstream
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("bitstream");
        viewEventRest.setTargetId(bitstreamVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
                .andExpect(status().isCreated());
    }

    @Test
    public void TotalDownloadsReport_Item_NotVisited() throws Exception {
        // ** WHEN **
        // You don't visit an item's bitstreams
        // And request that item's TotalDownloads stat report
        getClient(adminToken).perform(
            get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() + "_" +
                TOTAL_DOWNLOADS_REPORT_ID))
                   // ** THEN **
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       UsageReportMatcher.matchUsageReport(
                           itemNotVisitedWithBitstreams.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID,
                           TOTAL_DOWNLOADS_REPORT_ID,
                           List.of()
                       )
                   )));
    }

    /**
     * Note: Geolite response mocked in {@link org.dspace.statistics.MockSolrLoggerServiceImpl}
     */
    @Test
    public void topCountriesReport_Collection_Visited() throws Exception {
        // ** WHEN **
        // We visit a Collection
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("collection");
        viewEventRest.setTargetId(collectionVisited.getID());

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {
                List<UsageReportPointRest> expectedPoints = List.of(
                    getExpectedCountryViews(Locale.US.getCountry(),
                        Locale.US.getDisplayCountry(context.getCurrentLocale()),
                        1));

                // And request that collection's TopCountries report
                getClient(adminToken).perform(
                    get("/api/statistics/usagereports/" + collectionVisited.getID() + "_" + TOP_COUNTRIES_REPORT_ID))
                    // ** THEN **
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        UsageReportMatcher.matchUsageReport(
                            collectionVisited.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID,
                            expectedPoints
                        )
                    )));

                // only admin has access to countries report
                getClient(loggedInToken).perform(
                    get("/api/statistics/usagereports/" + collectionVisited.getID() + "_" + TOP_COUNTRIES_REPORT_ID))
                    .andExpect(status().isForbidden());

                getClient().perform(
                    get("/api/statistics/usagereports/" + collectionVisited.getID() + "_" + TOP_COUNTRIES_REPORT_ID))
                    .andExpect(status().isUnauthorized());

                // make statistics visible to all
                configurationService.setProperty("usage-statistics.authorization.admin.usage", false);

                getClient(loggedInToken).perform(
                    get("/api/statistics/usagereports/" + collectionVisited.getID() + "_" + TOP_COUNTRIES_REPORT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        UsageReportMatcher.matchUsageReport(
                            collectionVisited.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID,
                            expectedPoints
                        )
                    )));

                getClient().perform(
                    get("/api/statistics/usagereports/" + collectionVisited.getID() + "_" + TOP_COUNTRIES_REPORT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        UsageReportMatcher.matchUsageReport(
                            collectionVisited.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID,
                            expectedPoints
                        )
                    )));
            }));
        ObjectMapper mapper = new ObjectMapper();
        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
            .andExpect(status().isCreated());
    }

    @Test
    public void TotalDownloadsReport_SupportedDSO_Collection() throws Exception {
        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/" + collectionVisited.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID))
            .andExpect(status().isOk());
    }

    /**
     * Note: Geolite response mocked in {@link org.dspace.statistics.MockSolrLoggerServiceImpl}
     */
    @Test
    public void topCountriesReport_Community_Visited() throws Exception {
        // ** WHEN **
        // We visit a Community twice
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("community");
        viewEventRest.setTargetId(communityVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
                .andExpect(status().isCreated());

        Thread.sleep(1000);

        UsageReportPointCountryRest expectedPoint = new UsageReportPointCountryRest();
        expectedPoint.addValue("views", 2);
        expectedPoint.setIdAndLabel(Locale.US.getCountry(),
            Locale.US.getDisplayCountry(context.getCurrentLocale()));

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {
                // And request that collection's TopCountries report
                getClient(adminToken).perform(
                        get("/api/statistics/usagereports/" + communityVisited.getID() + "_" + TOP_COUNTRIES_REPORT_ID))
                    // ** THEN **
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        UsageReportMatcher.matchUsageReport(
                            communityVisited.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID,
                            List.of(
                                getExpectedCountryViews("US", "United States", 2)
                            )
                        )
                    )));
            }));

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
            .andExpect(status().isCreated());
    }

    /**
     * Note: Geolite response mocked in {@link org.dspace.statistics.MockSolrLoggerServiceImpl}
     */
    @Test
    public void topCountriesReport_Item_NotVisited() throws Exception {
        // ** WHEN **
        // Item is not visited
        // And request that item's TopCountries report
        getClient(adminToken).perform(
            get("/api/statistics/usagereports/" + itemNotVisitedWithBitstreams.getID() +
                "_" + TOP_COUNTRIES_REPORT_ID))
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.is(
                UsageReportMatcher.matchUsageReport(
                    itemNotVisitedWithBitstreams.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                    TOP_COUNTRIES_REPORT_ID,
                    List.of()
                )
            )));
    }

    /**
     * Note: Geolite response mocked in {@link org.dspace.statistics.MockSolrLoggerServiceImpl}
     */
    @Test
    public void topCitiesReport_Item_Visited() throws Exception {
        // ** WHEN **
        // We visit an Item
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("item");
        viewEventRest.setTargetId(itemVisited.getID());

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {
                List<UsageReportPointRest> expectedPoints = List.of(
                    getExpectedCityViews("New York", 1)
                );

                // And request that item's TopCities report
                getClient(adminToken).perform(
                        get("/api/statistics/usagereports/" + itemVisited.getID() + "_" + TOP_CITIES_REPORT_ID))
                    // ** THEN **
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        UsageReportMatcher.matchUsageReport(
                            itemVisited.getID() + "_" + TOP_CITIES_REPORT_ID,
                            TOP_CITIES_REPORT_ID,
                            expectedPoints
                        )
                    )));

                // only admin has access to cities report
                getClient(loggedInToken).perform(
                        get("/api/statistics/usagereports/" + itemVisited.getID() + "_" + TOP_CITIES_REPORT_ID))
                    .andExpect(status().isForbidden());

                getClient().perform(
                        get("/api/statistics/usagereports/" + itemVisited.getID() + "_" + TOP_CITIES_REPORT_ID))
                    .andExpect(status().isUnauthorized());

                // make statistics visible to all
                configurationService.setProperty("usage-statistics.authorization.admin.usage", false);

                getClient(loggedInToken).perform(
                        get("/api/statistics/usagereports/" + itemVisited.getID() + "_" + TOP_CITIES_REPORT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        UsageReportMatcher.matchUsageReport(
                            itemVisited.getID() + "_" + TOP_CITIES_REPORT_ID,
                            TOP_CITIES_REPORT_ID,
                            expectedPoints
                        )
                    )));

                getClient().perform(
                        get("/api/statistics/usagereports/" + itemVisited.getID() + "_" + TOP_CITIES_REPORT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        UsageReportMatcher.matchUsageReport(
                            itemVisited.getID() + "_" + TOP_CITIES_REPORT_ID,
                            TOP_CITIES_REPORT_ID,
                            expectedPoints
                        )
                    )));
            }));

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
                .andExpect(status().isCreated());
    }

    /**
     * Note: Geolite response mocked in {@link org.dspace.statistics.MockSolrLoggerServiceImpl}
     */
    @Test
    public void topCitiesReport_Community_Visited() throws Exception {
        // ** WHEN **
        // We visit a Community thrice
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("community");
        viewEventRest.setTargetId(communityVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
                .andExpect(status().isCreated());

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
                .andExpect(status().isCreated());

        Thread.sleep(1000);

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {
                // And request that community's TopCities report
                getClient(adminToken).perform(
                        get("/api/statistics/usagereports/" + communityVisited.getID() + "_" + TOP_CITIES_REPORT_ID))
                    // ** THEN **
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        UsageReportMatcher.matchUsageReport(
                            communityVisited.getID() + "_" + TOP_CITIES_REPORT_ID,
                            TOP_CITIES_REPORT_ID,
                            List.of(
                                getExpectedCityViews("New York", 3)
                            )
                        )
                    )));
            }));

        getClient(loggedInToken).perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
                .andExpect(status().isCreated());
    }

    /**
     * Note: Geolite response mocked in {@link org.dspace.statistics.MockSolrLoggerServiceImpl}
     */
    @Test
    public void topCitiesReport_Collection_NotVisited() throws Exception {
        // ** WHEN **
        // Collection is not visited
        // And request that collection's TopCountries report
        getClient(adminToken).perform(
                get("/api/statistics/usagereports/" + collectionNotVisited.getID() + "_" + TOP_CITIES_REPORT_ID))
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.is(
                UsageReportMatcher.matchUsageReport(
                    collectionNotVisited.getID() + "_" + TOP_CITIES_REPORT_ID,
                    TOP_CITIES_REPORT_ID,
                    List.of()
                )
            )));
    }

    @Test
    public void usagereportsSearch_notProperURI_Exception() throws Exception {
        getClient(adminToken).perform(get("/api/statistics/usagereports/search/object?uri=BadUri"))
            .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    public void usagereportsSearch_noURI_Exception() throws Exception {
        getClient().perform(get("/api/statistics/usagereports/search/object"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    public void usagereportsSearch_NonExistentUUID_Exception() throws Exception {
        getClient(adminToken).perform(
                get("/api/statistics/usagereports/search/object?uri=http://localhost:8080/server/api" +
                    "/core" +
                                "/items/" + UUID.randomUUID()))
            .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    public void usagereportSearch_onlyAdminReadRights() throws Exception {
        // ** WHEN **
        authorizeService.removeAllPolicies(context, itemNotVisitedWithBitstreams);
        // We request a dso's TotalVisits usage stat report as anon but dso has no read policy for anon
        getClient().perform(get("/api/statistics/usagereports/search/object?uri=http://localhost:8080/server/api/core" +
                "/items/" + itemNotVisitedWithBitstreams.getID()))
                // ** THEN **
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasNoJsonPath("$.points")));
        // We request a dso's TotalVisits usage stat report as admin
        getClient(adminToken)
                .perform(get("/api/statistics/usagereports/search/object?uri=http://localhost:8080/server/api" +
                        "/core/items/" + itemNotVisitedWithBitstreams.getID()))
                // ** THEN **
                .andExpect(status().isOk());
    }

    @Test
    public void usagereportSearch_onlyAdminReadRights_unvalidToken() throws Exception {
        // ** WHEN **
        authorizeService.removeAllPolicies(context, itemNotVisitedWithBitstreams);
        // We request a dso's TotalVisits usage stat report with unvalid token
        getClient("unvalidToken")
            .perform(get("/api/statistics/usagereports/search/object?uri=http://localhost:8080/server/api/core" +
                "/items/" + itemNotVisitedWithBitstreams.getID()))
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasNoJsonPath("$.points")));
    }

    @Test
    public void usagereportSearch_loggedInUserReadRights() throws Exception {
        // ** WHEN **
        context.turnOffAuthorisationSystem();
        authorizeService.removeAllPolicies(context, itemNotVisitedWithBitstreams);
        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
                             .withDspaceObject(itemNotVisitedWithBitstreams)
                             .withAction(Constants.READ).build();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
                .withEmail("eperson1@mail.com")
                .withPassword(password)
                .build();
        context.restoreAuthSystemState();
        String anotherLoggedInUserToken = getAuthToken(eperson1.getEmail(), password);
        // We request a dso's TotalVisits usage stat report as anon but dso has no read policy for anon
        getClient()
                .perform(get("/api/statistics/usagereports/search/object?uri=http://localhost:8080/server/api/core" +
                        "/items/" + itemNotVisitedWithBitstreams.getID()))
                // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasNoJsonPath("$.points")));
        // We request a dso's TotalVisits usage stat report as logged in eperson and has read policy for this user
        getClient(loggedInToken)
            .perform(get("/api/statistics/usagereports/search/object?uri=http://localhost:8080/server/api/core" +
                "/items/" + itemNotVisitedWithBitstreams.getID()))
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasNoJsonPath("$.points")));
        // We request a dso's TotalVisits usage stat report as another logged in eperson and has no read policy for
        // this user
        getClient(anotherLoggedInUserToken)
                .perform(get("/api/statistics/usagereports/search/object?uri=http://localhost:8080/server/api/core" +
                        "/items/" + itemNotVisitedWithBitstreams.getID()))
                // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasNoJsonPath("$.points")));
    }

    @Test
    public void usageReportsSearch_Site_mainReports() throws Exception {
        context.turnOffAuthorisationSystem();
        Site site = SiteBuilder.createSite(context).build();
        Item item = ItemBuilder.createItem(context, collectionNotVisited)
            .withEntityType("Publication")
            .withTitle("My item")
            .build();
        Item item2 = ItemBuilder.createItem(context, collectionNotVisited)
            .withEntityType("Patent")
            .withTitle("My item 2")
            .build();
        Item item3 = ItemBuilder.createItem(context, collectionNotVisited)
            .withEntityType("Funding")
            .withTitle("My item 3")
            .build();
        Item item4 = ItemBuilder.createItem(context, collectionNotVisited)
            .withEntityType("Project")
            .withTitle("My item 4")
            .build();
        context.restoreAuthSystemState();

        ObjectMapper mapper = new ObjectMapper();

        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("item");
        viewEventRest.setTargetId(item.getID());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
                .andExpect(status().isCreated());

        ViewEventRest viewEventRest2 = new ViewEventRest();
        viewEventRest2.setTargetType("item");
        viewEventRest2.setTargetId(item2.getID());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest2))
                .contentType(contentType))
                .andExpect(status().isCreated());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest2))
                .contentType(contentType))
                .andExpect(status().isCreated());

        ViewEventRest viewEventRest3 = new ViewEventRest();
        viewEventRest3.setTargetType("item");
        viewEventRest3.setTargetId(item3.getID());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest3))
                .contentType(contentType))
            .andExpect(status().isCreated());

        ViewEventRest viewEventRest4 = new ViewEventRest();
        viewEventRest4.setTargetType("item");
        viewEventRest4.setTargetId(item4.getID());


        UsageReportPointDsoTotalVisitsRest expectedPoint1 = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint1.addValue("views", 1);
        expectedPoint1.setType("item");
        expectedPoint1.setLabel("My item");
        expectedPoint1.setId(item.getID().toString());

        UsageReportPointDsoTotalVisitsRest expectedPoint2 = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint2.addValue("views", 2);
        expectedPoint2.setType("item");
        expectedPoint2.setLabel("My item 2");
        expectedPoint2.setId(item2.getID().toString());

        UsageReportPointDsoTotalVisitsRest expectedPoint3 = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint3.addValue("views", 1);
        expectedPoint3.setType("item");
        expectedPoint3.setLabel("My item 3");
        expectedPoint3.setId(item3.getID().toString());

        UsageReportPointDsoTotalVisitsRest expectedPoint4 = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint4.addValue("views", 1);
        expectedPoint4.setType("item");
        expectedPoint4.setLabel("My item 4");
        expectedPoint4.setId(item4.getID().toString());

        List<UsageReportPointRest> points =
            List.of(expectedPoint1, expectedPoint2, expectedPoint3, expectedPoint4);

        UsageReportPointCityRest pointCity = new UsageReportPointCityRest();
        pointCity.addValue("views", 5);
        pointCity.setId("New York");

        UsageReportPointContinentRest pointContinent = new UsageReportPointContinentRest();
        pointContinent.addValue("views", 5);
        pointContinent.setId("North America");

        UsageReportPointCountryRest pointCountry = new UsageReportPointCountryRest();
        pointCountry.addValue("views", 5);
        pointCountry.setIdAndLabel(Locale.US.getCountry(),
            Locale.US.getDisplayCountry(context.getCurrentLocale()));

        UsageReportPointCategoryRest publicationCategory = new UsageReportPointCategoryRest();
        publicationCategory.addValue("views", 1);
        publicationCategory.setId("publication");

        UsageReportPointCategoryRest patentCategory = new UsageReportPointCategoryRest();
        patentCategory.addValue("views", 2);
        patentCategory.setId("patent");

        UsageReportPointCategoryRest fundingCategory = new UsageReportPointCategoryRest();
        fundingCategory.addValue("views", 1);
        fundingCategory.setId("funding");

        UsageReportPointCategoryRest projectCategory = new UsageReportPointCategoryRest();
        projectCategory.addValue("views", 1);
        projectCategory.setId("project");

        UsageReportPointCategoryRest productCategory = new UsageReportPointCategoryRest();
        productCategory.addValue("views", 0);
        productCategory.setId("product");

        UsageReportPointCategoryRest journalCategory = new UsageReportPointCategoryRest();
        journalCategory.addValue("views", 0);
        journalCategory.setId("journal");

        UsageReportPointCategoryRest personCategory = new UsageReportPointCategoryRest();
        personCategory.addValue("views", 0);
        personCategory.setId("person");

        UsageReportPointCategoryRest orgUnitCategory = new UsageReportPointCategoryRest();
        orgUnitCategory.addValue("views", 0);
        orgUnitCategory.setId("orgunit");

        UsageReportPointCategoryRest equipmentCategory = new UsageReportPointCategoryRest();
        equipmentCategory.addValue("views", 0);
        equipmentCategory.setId("equipment");

        UsageReportPointCategoryRest eventCategory = new UsageReportPointCategoryRest();
        eventCategory.addValue("views", 0);
        eventCategory.setId("event");

        List<UsageReportPointRest> categories = List.of(publicationCategory, patentCategory, fundingCategory,
            projectCategory, productCategory, journalCategory,
            personCategory, orgUnitCategory,
            equipmentCategory, eventCategory);
        Thread.sleep(1000);

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {
                // And request the sites global usage report (show top most popular items)
                getClient(adminToken)
                    .perform(get("/api/statistics/usagereports/search/object")
                        .param("category", "site-mainReports")
                        .param("uri", "http://localhost:8080/server/api/core/sites/" + site.getID()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.usagereports", not(empty())))
                    .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                        matchUsageReport(site.getID() + "_" + TOTAL_VISITS_REPORT_ID, TOP_ITEMS_REPORT_ID
                            , points),
                        matchUsageReport(site.getID() + "_" + TOP_CITIES_REPORT_ID, TOP_CITIES_REPORT_ID,
                            List.of(pointCity)),
                        matchUsageReport(site.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID, getLastMonthVisitPoints(5)),
                        matchUsageReport(site.getID() + "_" + TOP_CONTINENTS_REPORT_ID,
                            TOP_CONTINENTS_REPORT_ID,
                            List.of(pointContinent)),
                        matchUsageReport(site.getID() + "_" + TOP_CATEGORIES_REPORT_ID,
                            TOP_CATEGORIES_REPORT_ID,
                            categories),
                        matchUsageReport(site.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID,
                            List.of(pointCountry)))));
            }));

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest4))
                .contentType(contentType))
            .andExpect(status().isCreated());
    }

    @Test
    public void usageReportsSearch_Site_downloadReports() throws Exception {

        context.turnOffAuthorisationSystem();

        Site site = SiteBuilder.createSite(context).build();

        Item item1 = ItemBuilder.createItem(context, collectionNotVisited)
            .withTitle("Item 1")
            .build();

        Item item2 = ItemBuilder.createItem(context, collectionNotVisited)
            .withTitle("Item 2")
            .build();

        Item item3 = ItemBuilder.createItem(context, collectionNotVisited)
            .withTitle("Item 3")
            .build();

        Bitstream bitstream1 = createBitstream(item1, "Bitstream 1");
        Bitstream bitstream2 = createBitstream(item1, "Bitstream 2");
        Bitstream bitstream3 = createBitstream(item2, "Bitstream 3");
        Bitstream bitstream4 = createBitstream(item3, "Bitstream 4");

        context.restoreAuthSystemState();
        UsageReportPointDsoTotalVisitsRest expectedPoint1 = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint1.addValue("views", 3);
        expectedPoint1.setType("item");
        expectedPoint1.setLabel("Item 1");
        expectedPoint1.setId(item1.getID().toString());

        UsageReportPointDsoTotalVisitsRest expectedPoint2 = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint2.addValue("views", 3);
        expectedPoint2.setType("item");
        expectedPoint2.setLabel("Item 2");
        expectedPoint2.setId(item2.getID().toString());

        UsageReportPointDsoTotalVisitsRest expectedPoint3 = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint3.addValue("views", 2);
        expectedPoint3.setType("item");
        expectedPoint3.setLabel("Item 3");
        expectedPoint3.setId(item3.getID().toString());

        List<UsageReportPointRest> points = List.of(expectedPoint1, expectedPoint2, expectedPoint3);

        UsageReportPointCityRest pointCity = new UsageReportPointCityRest();
        pointCity.addValue("views", 8);
        pointCity.setId("New York");

        UsageReportPointContinentRest pointContinent = new UsageReportPointContinentRest();
        pointContinent.addValue("views", 8);
        pointContinent.setId("North America");

        UsageReportPointCountryRest pointCountry = new UsageReportPointCountryRest();
        pointCountry.addValue("views", 8);
        pointCountry.setIdAndLabel(Locale.US.getCountry(),
            Locale.US.getDisplayCountry(context.getCurrentLocale()));

        getClient().perform(get("/api/core/bitstreams/" + bitstream1.getID() + "/content"))
            .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream1.getID() + "/content"))
            .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream2.getID() + "/content"))
            .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream3.getID() + "/content"))
            .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream3.getID() + "/content"))
            .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream3.getID() + "/content"))
            .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream4.getID() + "/content"))
            .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream4.getID() + "/content"))
            .andExpect(status().isOk());

        Thread.sleep(1000);

        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                .param("category", "site-downloadReports")
                .param("uri", "http://localhost:8080/server/api/core/sites/" + site.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", not(empty())))
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                matchUsageReport(site.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID, TOP_ITEMS_REPORT_ID, points),
                matchUsageReport(site.getID() + "_" + TOP_DOWNLOAD_CITIES_REPORT_ID,
                    TOP_CITIES_REPORT_ID, List.of(pointCity)),
                matchUsageReport(site.getID() + "_" + TOTAL_DOWNLOAD_PER_MONTH_REPORT_ID,
                    TOTAL_VISITS_PER_MONTH_REPORT_ID, getLastMonthVisitPoints(8)),
                matchUsageReport(site.getID() + "_" + TOP_DOWNLOAD_CONTINENTS_REPORT_ID,
                    TOP_CONTINENTS_REPORT_ID, List.of(pointContinent)),
                matchUsageReport(site.getID() + "_" + TOP_DOWNLOAD_COUNTRIES_REPORT_ID,
                    TOP_COUNTRIES_REPORT_ID, List.of(pointCountry)))));

    }

    private Bitstream createBitstream(Item item, String name) throws Exception {
        return BitstreamBuilder.createBitstream(context, item, nullInputStream())
            .withName(name)
            .build();
    }

    @Test
    public void usageReportsSearch_Community_Visited() throws Exception {
        // ** WHEN **
        // We visit a community
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("community");
        viewEventRest.setTargetId(communityVisited.getID());

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {
                UsageReportPointDsoTotalVisitsRest expectedPointTotalVisits =
                    new UsageReportPointDsoTotalVisitsRest();
                expectedPointTotalVisits.addValue("views", 1);
                expectedPointTotalVisits.setType("community");
                expectedPointTotalVisits.setId(communityVisited.getID().toString());

                UsageReportPointCityRest expectedPointCity = new UsageReportPointCityRest();
                expectedPointCity.addValue("views", 1);
                expectedPointCity.setId("New York");

                UsageReportPointCountryRest expectedPointCountry = new UsageReportPointCountryRest();
                expectedPointCountry.addValue("views", 1);
                expectedPointCountry.setIdAndLabel(Locale.US.getCountry(),
                    Locale.US.getDisplayCountry(context.getCurrentLocale()));

                // And request the community usage reports
                getClient(adminToken)
                    .perform(get("/api/statistics/usagereports/search/object?category=community-mainReports" +
                        "&uri=http://localhost:8080/server/api/core" +
                        "/communities/" + communityVisited.getID()))
                    // ** THEN **
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.usagereports", not(empty())))
                    .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                        UsageReportMatcher.matchUsageReport(
                            communityVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID,
                            List.of(
                                getExpectedDsoViews(communityVisited, 1)
                            )
                        ),
                        UsageReportMatcher.matchUsageReport(
                            communityVisited.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            getLastMonthVisitPoints(1)
                        ),
                        UsageReportMatcher.matchUsageReport(
                            communityVisited.getID() + "_" + TOP_CITIES_REPORT_ID,
                            TOP_CITIES_REPORT_ID,
                            List.of(
                                getExpectedCityViews("New York", 1)
                            )
                        ),
                        UsageReportMatcher.matchUsageReport(
                            communityVisited.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID,
                            List.of(
                                getExpectedCountryViews("US", "United States", 1)
                            )
                        )
                    )));
            }));


        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
                .andExpect(status().isCreated());
    }

    @Test
    public void usageReportsSearch_Collection_NotVisited() throws Exception {
        // ** WHEN **
        // Collection is not visited
        // And request the collection's usage reports
        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object?category=collection-mainReports" +
                "&uri=http://localhost:8080/server/api/core" +
                "/collections/" + collectionNotVisited.getID()))
            // ** THEN **
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", not(empty())))
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                UsageReportMatcher.matchUsageReport(
                    collectionNotVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                    TOTAL_VISITS_REPORT_ID,
                    List.of(
                        getExpectedDsoViews(collectionNotVisited, 0)
                    )
                ),
                UsageReportMatcher.matchUsageReport(
                    collectionNotVisited.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                    TOTAL_VISITS_PER_MONTH_REPORT_ID,
                    getLastMonthVisitPoints(0)
                ),
                UsageReportMatcher.matchUsageReport(
                    collectionNotVisited.getID() + "_" + TOP_CITIES_REPORT_ID,
                    TOP_CITIES_REPORT_ID,
                    List.of()
                ),
                UsageReportMatcher.matchUsageReport(
                    collectionNotVisited.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                    TOP_COUNTRIES_REPORT_ID,
                    List.of()
                )
            )));
    }

    @Test
    public void usageReportsSearch_Item_Visited_FileNotVisited() throws Exception {

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {
                UsageReportPointDsoTotalVisitsRest expectedPointTotalVisits =
                    new UsageReportPointDsoTotalVisitsRest();
                expectedPointTotalVisits.addValue("views", 1);
                expectedPointTotalVisits.setType("item");
                expectedPointTotalVisits.setId(itemVisited.getID().toString());

                UsageReportPointCityRest expectedPointCity = new UsageReportPointCityRest();
                expectedPointCity.addValue("views", 1);
                expectedPointCity.setId("New York");

                UsageReportPointCountryRest expectedPointCountry = new UsageReportPointCountryRest();
                expectedPointCountry.addValue("views", 1);
                expectedPointCountry.setIdAndLabel(Locale.US.getCountry(),
                    Locale.US.getDisplayCountry(context.getCurrentLocale()));

                //views and downloads
                List<UsageReportPointRest> totalDownloadsPoints = new ArrayList<>();
                UsageReportPointDsoTotalVisitsRest expectedPointTotalVisitsBit1 =
                    new UsageReportPointDsoTotalVisitsRest();
                expectedPointTotalVisitsBit1.addValue("views", 1);
                expectedPointTotalVisitsBit1.setType("item");
                UsageReportPointDsoTotalVisitsRest expectedPointTotalVisitsBit2 =
                    new UsageReportPointDsoTotalVisitsRest();
                expectedPointTotalVisitsBit2.addValue("views", 0);
                expectedPointTotalVisitsBit2.setType("bitstream");
                totalDownloadsPoints.add(expectedPointTotalVisitsBit1);
                totalDownloadsPoints.add(expectedPointTotalVisitsBit2);


                // And request the community usage reports
                getClient(adminToken)
                    .perform(
                        get("/api/statistics/usagereports/search/object?uri=http://localhost:8080/server" +
                            "/api/core" +
                            "/items/" + itemVisited.getID()))
                    // ** THEN **
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.usagereports", not(empty())))
                    .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                        UsageReportMatcher.matchUsageReport(
                            itemVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID,
                            List.of(
                                getExpectedDsoViews(itemVisited, 1)
                            )
                        ),
                        UsageReportMatcher.matchUsageReport(
                            itemVisited.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            getLastMonthVisitPoints(1)
                        ),
                        UsageReportMatcher.matchUsageReport(
                            itemVisited.getID() + "_" + TOP_CITIES_REPORT_ID,
                            TOP_CITIES_REPORT_ID,
                            List.of(
                                getExpectedCityViews("New York", 1)
                            )
                        ),
                        UsageReportMatcher.matchUsageReport(
                            itemVisited.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID,
                            List.of(
                                getExpectedCountryViews("US", "United States", 1)
                            )
                        ),
                        UsageReportMatcher.matchUsageReport(
                            itemVisited.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID,
                            TOTAL_DOWNLOADS_REPORT_ID,
                            List.of()
                        )
                    )));
            }));

        // ** WHEN **
        // We visit an item
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("item");
        viewEventRest.setTargetId(itemVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
            .andExpect(status().isCreated());
    }

    @Test
    public void usageReportsSearch_ItemVisited_FilesVisited() throws Exception {
        context.turnOffAuthorisationSystem();
        Bitstream bitstream1 =
                BitstreamBuilder.createBitstream(context, itemVisited,
                        toInputStream("test", UTF_8))
                    .withName("bitstream1")
                    .build();
        Bitstream bitstream2 =
                BitstreamBuilder.createBitstream(context, itemVisited,
                        toInputStream("test", UTF_8))
                    .withName("bitstream2")
                    .build();
        context.restoreAuthSystemState();

        // ** WHEN **
        // We visit an item
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("item");
        viewEventRest.setTargetId(itemVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
                .andExpect(status().isCreated());

        // And its two files, second one twice
        ViewEventRest viewEventRestBit1 = new ViewEventRest();
        viewEventRestBit1.setTargetType("bitstream");
        viewEventRestBit1.setTargetId(bitstream1.getID());
        ViewEventRest viewEventRestBit2 = new ViewEventRest();
        viewEventRestBit2.setTargetType("bitstream");
        viewEventRestBit2.setTargetId(bitstream2.getID());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRestBit1))
                .contentType(contentType))
                .andExpect(status().isCreated());
        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRestBit2))
                .contentType(contentType))
                .andExpect(status().isCreated());

        Thread.sleep(3000);


        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {
                UsageReportPointDsoTotalVisitsRest expectedPointTotalVisits =
                    new UsageReportPointDsoTotalVisitsRest();
                expectedPointTotalVisits.addValue("views", 1);
                expectedPointTotalVisits.setType("item");
                expectedPointTotalVisits.setId(itemVisited.getID().toString());

                UsageReportPointCityRest expectedPointCity = new UsageReportPointCityRest();
                expectedPointCity.addValue("views", 1);
                expectedPointCity.setId("New York");

                UsageReportPointCountryRest expectedPointCountry = new UsageReportPointCountryRest();
                expectedPointCountry.addValue("views", 1);
                expectedPointCountry.setIdAndLabel(Locale.US.getCountry(),
                    Locale.US.getDisplayCountry(context.getCurrentLocale()));

                List<UsageReportPointRest> totalDownloadsPoints = new ArrayList<>();
                UsageReportPointDsoTotalVisitsRest expectedPointTotalVisitsBit1 =
                    new UsageReportPointDsoTotalVisitsRest();
                expectedPointTotalVisitsBit1.addValue("views", 1);
                expectedPointTotalVisitsBit1.setLabel("bitstream1");
                expectedPointTotalVisitsBit1.setId(bitstream1.getID().toString());
                expectedPointTotalVisitsBit1.setType("bitstream");
                UsageReportPointDsoTotalVisitsRest expectedPointTotalVisitsBit2 =
                    new UsageReportPointDsoTotalVisitsRest();
                expectedPointTotalVisitsBit2.addValue("views", 2);
                expectedPointTotalVisitsBit2.setLabel("bitstream2");
                expectedPointTotalVisitsBit2.setId(bitstream2.getID().toString());
                expectedPointTotalVisitsBit2.setType("bitstream");
                totalDownloadsPoints.add(expectedPointTotalVisitsBit1);
                totalDownloadsPoints.add(expectedPointTotalVisitsBit2);


                // first point for views
                UsageReportPointDsoTotalVisitsRest expectedPointTotalVisitsItem =
                    new UsageReportPointDsoTotalVisitsRest();
                expectedPointTotalVisitsItem.addValue("views", 1);
                expectedPointTotalVisitsItem.setType("item");

                //second point for total downlods
                UsageReportPointDsoTotalVisitsRest expectedPointTotalDownloads =
                    new UsageReportPointDsoTotalVisitsRest();
                expectedPointTotalDownloads.addValue("views", 3);
                expectedPointTotalDownloads.setType("bitstream");

                List<UsageReportPointRest> usageReportPointRestsVisitsAndDownloads = new ArrayList<>();
                usageReportPointRestsVisitsAndDownloads.add(expectedPointTotalVisitsItem);
                usageReportPointRestsVisitsAndDownloads.add(expectedPointTotalDownloads);


                // And request the community usage reports
                getClient(adminToken)
                    .perform(
                        get("/api/statistics/usagereports/search/object?uri=http://localhost:8080/server" +
                            "/api/core" +
                            "/items/" + itemVisited.getID()))
                    // ** THEN **
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.usagereports", not(empty())))
                    .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                        UsageReportMatcher.matchUsageReport(
                            itemVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID,
                            List.of(
                                getExpectedDsoViews(itemVisited, 1)
                            )
                        ),
                        UsageReportMatcher.matchUsageReport(
                            itemVisited.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            getLastMonthVisitPoints(1)
                        ),
                        UsageReportMatcher.matchUsageReport(
                            itemVisited.getID() + "_" + TOP_CITIES_REPORT_ID,
                            TOP_CITIES_REPORT_ID,
                            List.of(
                                getExpectedCityViews("New York", 1)
                            )
                        ),
                        UsageReportMatcher.matchUsageReport(
                            itemVisited.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID,
                            List.of(
                                getExpectedCountryViews("US", "United States", 1)
                            )
                        ),
                        UsageReportMatcher.matchUsageReport(
                            itemVisited.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID,
                            TOTAL_DOWNLOADS_REPORT_ID,
                            List.of(
                                getExpectedDsoViews(bitstream1, 1),
                                getExpectedDsoViews(bitstream2, 2)
                            )
                        )
                    )));
            }));

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRestBit2))
                .contentType(contentType))
            .andExpect(status().isCreated());
    }

    @Test
    public void usageReportsSearch_Bitstream_Visited() throws Exception {
        // ** WHEN **
        // We visit a bitstream
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("bitstream");
        viewEventRest.setTargetId(bitstreamVisited.getID());


        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {
                List<UsageReportPointRest> expectedTotalVisits = List.of(
                    getExpectedDsoViews(bitstreamVisited, 1)
                );

                // And request the community usage reports
                getClient(adminToken)
                    .perform(
                        get("/api/statistics/usagereports/search/object?uri=http://localhost:8080/server" +
                            "/api/core" +
                            "/items/" + bitstreamVisited.getID()))
                    // ** THEN **
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.usagereports", not(empty())))
                    .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                        UsageReportMatcher.matchUsageReport(
                            bitstreamVisited.getID() + "_" + TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID,
                            expectedTotalVisits
                        ),
                        UsageReportMatcher.matchUsageReport(
                            bitstreamVisited.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            getLastMonthVisitPoints(1)
                        ),
                        UsageReportMatcher.matchUsageReport(
                            bitstreamVisited.getID() + "_" + TOP_CITIES_REPORT_ID,
                            TOP_CITIES_REPORT_ID,
                            List.of(
                                getExpectedCityViews("New York", 1)
                            )
                        ),
                        UsageReportMatcher.matchUsageReport(
                            bitstreamVisited.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID,
                            List.of(
                                getExpectedCountryViews("US", "United States", 1)
                            )
                        ),
                        UsageReportMatcher.matchUsageReport(
                            bitstreamVisited.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID,
                            TOTAL_DOWNLOADS_REPORT_ID,
                            expectedTotalVisits
                        )
                    )));
            }));

        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
            .andExpect(status().isCreated());
    }

    // This test search for statistics before the moment in which item is visited
    @Test
    public void usageReportsSearch_ItemNotVisited_AtTime() throws Exception {
        context.turnOffAuthorisationSystem();
        Site site = SiteBuilder.createSite(context).build();
        //create new item using ItemBuilder
        context.restoreAuthSystemState();

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {
                //create expected raport points
                List<UsageReportPointRest> points = new ArrayList<>();
                UsageReportPointDsoTotalVisitsRest expectedPoint1 = new UsageReportPointDsoTotalVisitsRest();
                expectedPoint1.addValue("views", 0);
                expectedPoint1.setType("item");
                points.add(expectedPoint1);


                UsageReportPointCategoryRest publicationCategory = new UsageReportPointCategoryRest();
                publicationCategory.addValue("views", 0);
                publicationCategory.setId("publication");

                UsageReportPointCategoryRest patentCategory = new UsageReportPointCategoryRest();
                patentCategory.addValue("views", 0);
                patentCategory.setId("patent");

                UsageReportPointCategoryRest fundingCategory = new UsageReportPointCategoryRest();
                fundingCategory.addValue("views", 0);
                fundingCategory.setId("funding");

                UsageReportPointCategoryRest projectCategory = new UsageReportPointCategoryRest();
                projectCategory.addValue("views", 0);
                projectCategory.setId("project");

                UsageReportPointCategoryRest productCategory = new UsageReportPointCategoryRest();
                productCategory.addValue("views", 0);
                productCategory.setId("product");

                UsageReportPointCategoryRest journalCategory = new UsageReportPointCategoryRest();
                journalCategory.addValue("views", 0);
                journalCategory.setId("journal");

                UsageReportPointCategoryRest personCategory = new UsageReportPointCategoryRest();
                personCategory.addValue("views", 0);
                personCategory.setId("person");

                UsageReportPointCategoryRest orgUnitCategory = new UsageReportPointCategoryRest();
                orgUnitCategory.addValue("views", 0);
                orgUnitCategory.setId("orgunit");

                UsageReportPointCategoryRest equipmentCategory = new UsageReportPointCategoryRest();
                equipmentCategory.addValue("views", 0);
                equipmentCategory.setId("equipment");

                UsageReportPointCategoryRest eventCategory = new UsageReportPointCategoryRest();
                eventCategory.addValue("views", 0);
                eventCategory.setId("event");

                List<UsageReportPointRest> categories = List.of(publicationCategory, patentCategory,
                    fundingCategory,
                    projectCategory, productCategory, journalCategory,
                    personCategory, orgUnitCategory,
                    equipmentCategory, eventCategory);

                UsageReportPointRest pointPerMonth = new UsageReportPointDateRest();
                pointPerMonth.setId("June 2019");
                pointPerMonth.addValue("views", 0);

                List<UsageReportPointRest> pointsPerMonth = List.of(pointPerMonth);

                // And request the sites global usage report (show top most popular items) for a specific date range
                // we expect no points becase we are searching in a moment before the view of item happened
                getClient(adminToken)
                    .perform(
                        get("/api/statistics/usagereports/search/object?uri=http://localhost:8080/server" +
                            "/api/core" +
                            "/sites/" + site.getID() +
                            "&startDate=2019-06-01&endDate=2019-06-02&category=site-mainReports"))
                    // ** THEN **
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.usagereports", not(empty())))
                    .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                        matchUsageReport(site.getID() + "_" + TOTAL_VISITS_REPORT_ID, TOP_ITEMS_REPORT_ID
                            , points),
                        matchUsageReport(site.getID() + "_" + TOP_CITIES_REPORT_ID, TOP_CITIES_REPORT_ID,
                            List.of()),
                        matchUsageReport(site.getID() + "_" + TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID, pointsPerMonth),
                        matchUsageReport(site.getID() + "_" + TOP_CONTINENTS_REPORT_ID,
                            TOP_CONTINENTS_REPORT_ID, List.of()),
                        matchUsageReport(site.getID() + "_" + TOP_CATEGORIES_REPORT_ID,
                            TOP_CATEGORIES_REPORT_ID, categories),
                        matchUsageReport(site.getID() + "_" + TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID, List.of()))));
            }));

        //visit first item now
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("item");
        viewEventRest.setTargetId(itemVisited.getID());
        ObjectMapper mapper = new ObjectMapper();
        //add visit for first item
        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
            .andExpect(status().isCreated());
    }

    // This test search for statistics one day after the moment in which community is visited
    @Test
    public void usageReportsSearch_Community_VisitedAtTime() throws Exception {

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {
                getExpectedDsoViews(communityVisited, 1);
                UsageReportPointDsoTotalVisitsRest expectedPointTotalVisits =
                    getExpectedDsoViews(communityVisited, 1);

                UsageReportPointCityRest expectedPointCity = getExpectedCityViews("New York", 1);

                UsageReportPointCountryRest expectedPointCountry = getExpectedCountryViews(Locale.US.getCountry(),
                    Locale.US.getDisplayCountry(
                        context.getCurrentLocale()),
                    1);

                //add one day to the moment when we visit the community
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, 1);
                String endDate = dateFormat.format(cal.getTime());
                // And request the community usage reports
                getClient(adminToken)
                    .perform(get("/api/statistics/usagereports/search/object?category=community-mainReports" +
                        "&uri=http://localhost:8080/server/api/core" +
                        "/communities/" + communityVisited.getID() + "&startDate=2019-06-01&endDate=" +
                        endDate))
                    // ** THEN **
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.usagereports", not(empty())))
                    .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                        UsageReportMatcher
                                .matchUsageReport(communityVisited.getID() + "_" +
                                        TOTAL_VISITS_REPORT_ID, TOTAL_VISITS_REPORT_ID,
                                    List.of(expectedPointTotalVisits)),
                        UsageReportMatcher.matchUsageReport(communityVisited.getID() + "_" +
                                                                TOTAL_VISITS_PER_MONTH_REPORT_ID,
                                                            TOTAL_VISITS_PER_MONTH_REPORT_ID,
                                                            getListOfVisitsPerMonthsPoints(1, "2019-06-01")),
                        UsageReportMatcher.matchUsageReport(communityVisited.getID() + "_" +
                                                                TOP_CITIES_REPORT_ID, TOP_CITIES_REPORT_ID,
                                                            Arrays.asList(expectedPointCity)),
                        UsageReportMatcher.matchUsageReport(communityVisited.getID() + "_" +
                                                                TOP_COUNTRIES_REPORT_ID,
                                                            TOP_COUNTRIES_REPORT_ID,
                                                            Arrays.asList(expectedPointCountry))
                    )));
            }));

        // ** WHEN **
        // We visit a community
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("community");
        viewEventRest.setTargetId(communityVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
            .andExpect(status().isCreated());
    }

    // filter bitstream only with  start date
    @Test
    public void usageReportsSearch_Bitstream_VisitedFromTime() throws Exception {

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {
                UsageReportPointDsoTotalVisitsRest expectedPointTotalVisits =
                    new UsageReportPointDsoTotalVisitsRest();
                expectedPointTotalVisits.addValue("views", 1);
                expectedPointTotalVisits.setType("bitstream");
                expectedPointTotalVisits.setLabel("BitstreamVisitedName");
                expectedPointTotalVisits.setId(bitstreamVisited.getID().toString());

                UsageReportPointCityRest expectedPointCity = new UsageReportPointCityRest();
                expectedPointCity.addValue("views", 1);
                expectedPointCity.setId("New York");

                UsageReportPointCountryRest expectedPointCountry = new UsageReportPointCountryRest();
                expectedPointCountry.addValue("views", 1);
                expectedPointCountry.setIdAndLabel(Locale.US.getCountry(),
                    Locale.US.getDisplayCountry(context.getCurrentLocale()));

                //downloads and views expected points
                List<UsageReportPointRest> totalDownloadsPoints = new ArrayList<>();
                UsageReportPointDsoTotalVisitsRest expectedPointTotalVisitsBit1 =
                    new UsageReportPointDsoTotalVisitsRest();
                expectedPointTotalVisitsBit1.addValue("views", 1);
                expectedPointTotalVisitsBit1.setType("bitstream");
                UsageReportPointDsoTotalVisitsRest expectedPointTotalVisitsBit2 =
                    new UsageReportPointDsoTotalVisitsRest();
                expectedPointTotalVisitsBit2.addValue("views", 0);
                expectedPointTotalVisitsBit2.setType("bitstream");
                totalDownloadsPoints.add(expectedPointTotalVisitsBit1);
                totalDownloadsPoints.add(expectedPointTotalVisitsBit2);
                //  And request the community usage reports
                getClient(adminToken)
                    .perform(
                        get("/api/statistics/usagereports/search/object?uri=http://localhost:8080/server" +
                            "/api/core" +
                            "/items/" + bitstreamVisited.getID() + "&startDate=2019-05-01"))
                    // ** THEN **
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.usagereports", not(empty())))
                    .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                        UsageReportMatcher.matchUsageReport(bitstreamVisited.getID() + "_" +
                                                                TOTAL_VISITS_REPORT_ID, TOTAL_VISITS_REPORT_ID,
                                                            Arrays.asList(expectedPointTotalVisits)),
                        UsageReportMatcher.matchUsageReport(bitstreamVisited.getID() + "_" +
                                                                TOTAL_VISITS_PER_MONTH_REPORT_ID,
                                                            TOTAL_VISITS_PER_MONTH_REPORT_ID,
                                                            getListOfVisitsPerMonthsPoints(1, "2019-05-01")),
                        UsageReportMatcher.matchUsageReport(bitstreamVisited.getID() + "_" +
                                                                TOP_CITIES_REPORT_ID, TOP_CITIES_REPORT_ID,
                                                            Arrays.asList(expectedPointCity)),
                        UsageReportMatcher.matchUsageReport(bitstreamVisited.getID() + "_" +
                                                                TOP_COUNTRIES_REPORT_ID,
                                                            TOP_COUNTRIES_REPORT_ID,
                                                            Arrays.asList(expectedPointCountry)),
                        UsageReportMatcher.matchUsageReport(bitstreamVisited.getID() + "_" +
                                                                TOTAL_DOWNLOADS_REPORT_ID,
                                                            TOTAL_DOWNLOADS_REPORT_ID,
                                                            Arrays.asList(expectedPointTotalVisits))
                    )));
            }));

        // ** WHEN **
        // We visit a bitstream
        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("bitstream");
        viewEventRest.setTargetId(bitstreamVisited.getID());

        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
            .andExpect(status().isCreated());
    }

    //test for inverse relation between person and publication
    @Test
    public void usageReportsSearch_PersonWithPublicationVisited() throws Exception {
        //visit the person
        ViewEventRest viewEventRestItem = new ViewEventRest();
        viewEventRestItem.setTargetType("item");
        viewEventRestItem.setTargetId(person.getID());

        //visit the first publication
        ViewEventRest viewEventRestFirstPublicationOfPerson = new ViewEventRest();
        viewEventRestFirstPublicationOfPerson.setTargetType("item");
        viewEventRestFirstPublicationOfPerson.setTargetId(publicationVisited1.getID());

        //visit the second publication
        ViewEventRest viewEventRestSecondPublicationOfPerson = new ViewEventRest();
        viewEventRestSecondPublicationOfPerson.setTargetType("item");
        viewEventRestSecondPublicationOfPerson.setTargetId(publicationVisited2.getID());

        //first bitstream visit
        ViewEventRest viewEventRestFirstPublicationBitstream = new ViewEventRest();
        viewEventRestFirstPublicationBitstream.setTargetType("bitstream");
        viewEventRestFirstPublicationBitstream.setTargetId(bitstreampublication_first.getID());

        //second bitstream visit
        ViewEventRest viewEventRestSecondPublicationBitstream = new ViewEventRest();
        viewEventRestSecondPublicationBitstream.setTargetType("bitstream");
        viewEventRestSecondPublicationBitstream.setTargetId(bitstreampublication_second.getID());


        //create expected report points for visits
        UsageReportPointDsoTotalVisitsRest totalVisitRelation = new UsageReportPointDsoTotalVisitsRest();
        totalVisitRelation.addValue("views", 3);
        totalVisitRelation.setType("item");
        totalVisitRelation.setLabel("Views");
        totalVisitRelation.setId(person.getID().toString());
        //create expected report points for visits with relation
        UsageReportPointDsoTotalVisitsRest expectedPointTotal = new UsageReportPointDsoTotalVisitsRest();
        expectedPointTotal.addValue("views", 1);
        expectedPointTotal.setType("item");
        expectedPointTotal.setLabel(person.getName());
        expectedPointTotal.setId(person.getID().toString());

        UsageReportPointDsoTotalVisitsRest totalVisitRelationProjects =
            new UsageReportPointDsoTotalVisitsRest();
        totalVisitRelationProjects.addValue("views", 0);
        totalVisitRelationProjects.setType("item");
        totalVisitRelationProjects.setLabel("Views");
        totalVisitRelationProjects.setId(person.getID().toString());

        //create expected report points for city visits
        UsageReportPointCityRest expectedPointCity = new UsageReportPointCityRest();
        expectedPointCity.addValue("views", 1);
        expectedPointCity.setId("New York");
        //create expected report points for city visits with relation
        UsageReportPointCityRest expectedPointCityWithRelation = new UsageReportPointCityRest();
        expectedPointCityWithRelation.addValue("views", 3);
        expectedPointCityWithRelation.setId("New York");
        //create expected report points for contry visits
        UsageReportPointCountryRest expectedPointCountry = new UsageReportPointCountryRest();
        expectedPointCountry.addValue("views", 1);
        expectedPointCountry.setIdAndLabel(Locale.US.getCountry(),
                Locale.US.getDisplayCountry(context.getCurrentLocale()));
        //create expected report points for country visits with relation
        UsageReportPointCountryRest expectedPointCountryWithRelation = new UsageReportPointCountryRest();
        expectedPointCountryWithRelation.addValue("views", 3);
        expectedPointCountryWithRelation.setIdAndLabel(Locale.US.getCountry(),
                Locale.US.getDisplayCountry(context.getCurrentLocale()));

        //create viewevents for all of items and bistreams
        ObjectMapper mapper = new ObjectMapper();
        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRestItem))
                .contentType(contentType))
            .andExpect(status().isCreated());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRestFirstPublicationOfPerson))
                .contentType(contentType))
            .andExpect(status().isCreated());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRestSecondPublicationOfPerson))
                .contentType(contentType))
            .andExpect(status().isCreated());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRestSecondPublicationOfPerson))
                .contentType(contentType))
            .andExpect(status().isCreated());
        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRestFirstPublicationBitstream))
                .contentType(contentType))
            .andExpect(status().isCreated());

        Thread.sleep(1000);

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {
                getClient(adminToken)
                    .perform(
                        get("/api/statistics/usagereports/search/object?uri=http://localhost:8080/server" +
                            "/api/core" +
                            "/items/" + person.getID().toString()))
                    // ** THEN **
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.usagereports", not(empty())))
                    .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                        UsageReportMatcher.matchUsageReport(person.getID() + "_" +
                                TOTAL_VISITS_REPORT_ID,
                            TOTAL_VISITS_REPORT_ID,
                            List.of(expectedPointTotal)),
                        UsageReportMatcher.matchUsageReport(person.getID() + "_" +
                                TOTAL_VISITS_REPORT_ID_RELATION_PERSON_RESEARCHOUTPUTS,
                            TOTAL_VISITS_REPORT_ID,
                            List.of(totalVisitRelation)),
                        UsageReportMatcher.matchUsageReport(person.getID() + "_" +
                                TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            getLastMonthVisitPoints(1)),
                        UsageReportMatcher.matchUsageReport(person.getID() + "_" +
                                TOTAL_VISITS_PER_MONTH_REPORT_ID_RELATION_PERSON_RESEARCHOUTPUTS,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            getLastMonthVisitPoints(3)),
                        UsageReportMatcher.matchUsageReport(person.getID() + "_" +
                                TOTAL_VISITS_PER_MONTH_REPORT_ID_RELATION_PERSON_PROJECTS,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            getLastMonthVisitPoints(0)),
                        UsageReportMatcher.matchUsageReport(person.getID() + "_" +
                                TOP_CITIES_REPORT_ID, TOP_CITIES_REPORT_ID,
                            List.of(expectedPointCity)),
                        UsageReportMatcher.matchUsageReport(person.getID() + "_" +
                                TOP_CITIES_REPORT_ID_RELATION_PERSON_RESEARCHOUTPUTS,
                            TOP_CITIES_REPORT_ID,
                            List.of(expectedPointCityWithRelation)),
                        UsageReportMatcher.matchUsageReport(person.getID() + "_" +
                                TOP_CITIES_REPORT_ID_RELATION_PERSON_PROJECTS,
                            TOP_CITIES_REPORT_ID,
                            Collections.emptyList()),
                        UsageReportMatcher.matchUsageReport(person.getID() + "_" +
                                TOP_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID,
                            List.of(expectedPointCountry)),
                        UsageReportMatcher.matchUsageReport(person.getID() + "_" +
                                TOP_COUNTRIES_REPORT_ID_RELATION_PERSON_RESEARCHOUTPUTS,
                            TOP_COUNTRIES_REPORT_ID,
                            List.of(expectedPointCountryWithRelation)),
                        UsageReportMatcher.matchUsageReport(person.getID() + "_" +
                                TOP_COUNTRIES_REPORT_ID_RELATION_PERSON_PROJECTS,
                            TOP_COUNTRIES_REPORT_ID,
                            Collections.emptyList()),
                        UsageReportMatcher.matchUsageReport(person.getID() + "_" +
                                TOTAL_VISITS_REPORT_ID_RELATION_PERSON_PROJECTS,
                            TOTAL_VISITS_REPORT_ID,
                            List.of(totalVisitRelationProjects))
                    )));
            }));

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRestSecondPublicationBitstream))
                .contentType(contentType))
            .andExpect(status().isCreated());
    }

    //test for inverse relation between orgunit and publication
    @Test
    public void usageReportsSearch_OrgUnitWithPublicationVisited() throws Exception {
        //visit the orgunit
        ViewEventRest viewEventRestItemOrgUnit = new ViewEventRest();
        viewEventRestItemOrgUnit.setTargetType("item");
        viewEventRestItemOrgUnit.setTargetId(orgUnit.getID());

        //visit the person
        ViewEventRest viewEventRestItem = new ViewEventRest();
        viewEventRestItem.setTargetType("item");
        viewEventRestItem.setTargetId(person.getID());

        //visit the first publication
        ViewEventRest viewEventRestFirstPublicationOfPerson = new ViewEventRest();
        viewEventRestFirstPublicationOfPerson.setTargetType("item");
        viewEventRestFirstPublicationOfPerson.setTargetId(publicationVisited1.getID());

        //visit the second publication
        ViewEventRest viewEventRestSecondPublicationOfPerson = new ViewEventRest();
        viewEventRestSecondPublicationOfPerson.setTargetType("item");
        viewEventRestSecondPublicationOfPerson.setTargetId(publicationVisited2.getID());

        //first bitstream visit
        ViewEventRest viewEventRestFirstPublicationBitstream = new ViewEventRest();
        viewEventRestFirstPublicationBitstream.setTargetType("bitstream");
        viewEventRestFirstPublicationBitstream.setTargetId(bitstreampublication_first.getID());

        //second bitstream visit
        ViewEventRest viewEventRestSecondPublicationBitstream = new ViewEventRest();
        viewEventRestSecondPublicationBitstream.setTargetType("bitstream");
        viewEventRestSecondPublicationBitstream.setTargetId(bitstreampublication_second.getID());

        //create viewevents for all of items and bistreams
        ObjectMapper mapper = new ObjectMapper();
        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRestItemOrgUnit))
                .contentType(contentType))
                .andExpect(status().isCreated());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRestItem))
                .contentType(contentType))
                .andExpect(status().isCreated());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRestFirstPublicationOfPerson))
                .contentType(contentType))
                .andExpect(status().isCreated());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRestSecondPublicationOfPerson))
                .contentType(contentType))
                .andExpect(status().isCreated());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRestSecondPublicationOfPerson))
                .contentType(contentType))
                .andExpect(status().isCreated());
        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRestFirstPublicationBitstream))
                .contentType(contentType))
                .andExpect(status().isCreated());

        Thread.sleep(1000);

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {

                //create expected report points for visits
                UsageReportPointDsoTotalVisitsRest totalVisitRelation = new UsageReportPointDsoTotalVisitsRest();
                totalVisitRelation.addValue("views", 3);
                totalVisitRelation.setType("item");
                totalVisitRelation.setLabel("Views");
                totalVisitRelation.setId(orgUnit.getID().toString());

                //create expected report points for city visits with relation
                UsageReportPointCityRest expectedPointCityWithRelation = getExpectedCityViews("New York", 3);

                //create expected report points for country visits with relation
                UsageReportPointCountryRest expectedPointCountryWithRelation =
                    getExpectedCountryViews(Locale.US.getCountry(),
                        Locale.US.getDisplayCountry(context.getCurrentLocale()), 3);

                //top items expected report points
                List<UsageReportPointRest> points = new ArrayList<>();
                //first publication
                UsageReportPointDsoTotalVisitsRest expectedPoint1 = getExpectedDsoViews(publicationVisited2, 2);
                points.add(expectedPoint1);
                //second publication
                UsageReportPointDsoTotalVisitsRest expectedPoint2 = getExpectedDsoViews(publicationVisited1, 1);
                points.add(expectedPoint2);

                //total downloads expected points
                List<UsageReportPointRest> totalDownloadsPoints = new ArrayList<>();
                UsageReportPointDsoTotalVisitsRest expectedPointTotalVisitsBit1 = getExpectedDsoViews(
                    bitstreampublication_first, 1);

                UsageReportPointDsoTotalVisitsRest expectedPointTotalVisitsBit2 = getExpectedDsoViews(
                    bitstreampublication_second, 1);

                totalDownloadsPoints.add(expectedPointTotalVisitsBit1);
                totalDownloadsPoints.add(expectedPointTotalVisitsBit2);

                //total downloads and views expected points
                //views
                List<UsageReportPointRest> totalDownloadsAndViewsPoints = new ArrayList<>();
                UsageReportPointDsoTotalVisitsRest views = new UsageReportPointDsoTotalVisitsRest();
                views.addValue("views", 3);
                views.setType("item");
                views.setLabel("Item visits");
                //downloads
                UsageReportPointDsoTotalVisitsRest downloads = new UsageReportPointDsoTotalVisitsRest();
                downloads.addValue("views", 2);
                downloads.setType("bitstream");
                downloads.setLabel("File visits");
                totalDownloadsAndViewsPoints.add(views);
                totalDownloadsAndViewsPoints.add(downloads);

                getClient(adminToken)
                    .perform(
                        get("/api/statistics/usagereports/search/object?uri=http://localhost:8080/server" +
                            "/api/core" +
                            "/items/" + orgUnit.getID().toString())
                            .param("size", "50"))
                    // ** THEN **
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.usagereports", not(empty())))
                    .andExpect(jsonPath("$._embedded.usagereports", Matchers.hasItems(
                        UsageReportMatcher.matchUsageReport(orgUnit.getID() + "_" +
                                TOTAL_VISITS_REPORT_ID_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS,
                            TOTAL_VISITS_REPORT_ID,
                            List.of(totalVisitRelation)),
                        UsageReportMatcher.matchUsageReport(orgUnit.getID() + "_" +
                                TOTAL_VISITS_PER_MONTH_REPORT_ID_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID,
                            getLastMonthVisitPoints(3)),
                        UsageReportMatcher.matchUsageReport(orgUnit.getID() + "_" +
                                TOP_CITIES_REPORT_ID_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS,
                            TOP_CITIES_REPORT_ID,
                            List.of(expectedPointCityWithRelation)),
                        UsageReportMatcher.matchUsageReport(orgUnit.getID() + "_" +
                                TOP_COUNTRIES_REPORT_ID_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS,
                            TOP_COUNTRIES_REPORT_ID,
                            List.of(expectedPointCountryWithRelation)),
                        UsageReportMatcher.matchUsageReport(orgUnit.getID() + "_" +
                                TOP_ITEMS_REPORT_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS,
                            TOP_ITEMS_REPORT_ID, points),
                        UsageReportMatcher.matchUsageReport(orgUnit.getID() + "_" +
                                TOTAL_DOWNLOADS_REPORT_ID_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS,
                            TOTAL_DOWNLOADS_REPORT_ID, totalDownloadsPoints),
                        UsageReportMatcher.matchUsageReport(orgUnit.getID() + "_" +
                                TOTAL_VISITS_TOTAL_DOWNLOADS_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS,
                            TOTAL_VISITS_TOTAL_DOWNLOADS,
                            totalDownloadsAndViewsPoints)
                    )));
            }));

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRestSecondPublicationBitstream))
                .contentType(contentType))
            .andExpect(status().isCreated());
    }

    @Test
    public void usageReportsSearch_Collection_ItemReports() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context).build();
        collectionNotVisited = CollectionBuilder.createCollection(context, community)
                                                .withEntityType("Publication")
                                                .build();

        Item item = ItemBuilder.createItem(context, collectionNotVisited)
                               .withTitle("My item")
                               .withType("Controlled Vocabulary for Resource Type Genres::image")
                               .build();
        Item item2 = ItemBuilder.createItem(context, collectionNotVisited)
                                .withTitle("My item 2")
                                .withType("Controlled Vocabulary for Resource Type Genres::thesis")
                                .build();
        Item item3 = ItemBuilder.createItem(context, collectionNotVisited)
                                .withTitle("My item 3")
                                .withType("Controlled Vocabulary for Resource Type Genres::thesis::bachelor thesis")
                                .build();
        Item item4 = ItemBuilder.createItem(context, collectionNotVisited)
                                .withTitle("My item 4")
                                .withType("Controlled Vocabulary for Resource Type Genres::text::periodical::"
                                    + "journal::contribution to journal::journal article")
                                .build();
        context.restoreAuthSystemState();

        ObjectMapper mapper = new ObjectMapper();

        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("item");
        viewEventRest.setTargetId(item.getID());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
                   .andExpect(status().isCreated());

        ViewEventRest viewEventRest2 = new ViewEventRest();
        viewEventRest2.setTargetType("item");
        viewEventRest2.setTargetId(item2.getID());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest2))
                .contentType(contentType))
                   .andExpect(status().isCreated());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest2))
                .contentType(contentType))
                   .andExpect(status().isCreated());

        ViewEventRest viewEventRest3 = new ViewEventRest();
        viewEventRest3.setTargetType("item");
        viewEventRest3.setTargetId(item3.getID());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest3))
                .contentType(contentType))
                   .andExpect(status().isCreated());

        Thread.sleep(1000);

        ViewEventRest viewEventRest4 = new ViewEventRest();
        viewEventRest4.setTargetType("item");
        viewEventRest4.setTargetId(item4.getID());

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {

                UsageReportPointDsoTotalVisitsRest expectedPoint1 = new UsageReportPointDsoTotalVisitsRest();
                expectedPoint1.addValue("views", 1);
                expectedPoint1.setType("item");
                expectedPoint1.setLabel("My item");
                expectedPoint1.setId(item.getID().toString());

                UsageReportPointDsoTotalVisitsRest expectedPoint2 = new UsageReportPointDsoTotalVisitsRest();
                expectedPoint2.addValue("views", 2);
                expectedPoint2.setType("item");
                expectedPoint2.setLabel("My item 2");
                expectedPoint2.setId(item2.getID().toString());

                UsageReportPointDsoTotalVisitsRest expectedPoint3 = new UsageReportPointDsoTotalVisitsRest();
                expectedPoint3.addValue("views", 1);
                expectedPoint3.setType("item");
                expectedPoint3.setLabel("My item 3");
                expectedPoint3.setId(item3.getID().toString());

                UsageReportPointDsoTotalVisitsRest expectedPoint4 = new UsageReportPointDsoTotalVisitsRest();
                expectedPoint4.addValue("views", 1);
                expectedPoint4.setType("item");
                expectedPoint4.setLabel("My item 4");
                expectedPoint4.setId(item4.getID().toString());

                List<UsageReportPointRest> points =
                    List.of(expectedPoint1, expectedPoint2, expectedPoint3, expectedPoint4);

                UsageReportPointCityRest pointCity = new UsageReportPointCityRest();
                pointCity.addValue("views", 5);
                pointCity.setId("New York");

                UsageReportPointContinentRest pointContinent = new UsageReportPointContinentRest();
                pointContinent.addValue("views", 5);
                pointContinent.setId("North America");

                UsageReportPointCountryRest pointCountry = new UsageReportPointCountryRest();
                pointCountry.addValue("views", 5);
                pointCountry.setIdAndLabel(Locale.US.getCountry(),
                    Locale.US.getDisplayCountry(context.getCurrentLocale()));

                UsageReportPointCategoryRest articleCategory = new UsageReportPointCategoryRest();
                articleCategory.addValue("views", 1);
                articleCategory.setId("article");

                UsageReportPointCategoryRest thesisCategory = new UsageReportPointCategoryRest();
                thesisCategory.addValue("views", 3);
                thesisCategory.setId("thesis");

                UsageReportPointCategoryRest otherCategory = new UsageReportPointCategoryRest();
                otherCategory.addValue("views", 1);
                otherCategory.setId("other");

                UsageReportPointCategoryRest bookCategory = new UsageReportPointCategoryRest();
                bookCategory.addValue("views", 0);
                bookCategory.setId("book");

                UsageReportPointCategoryRest bookChapterCategory = new UsageReportPointCategoryRest();
                bookChapterCategory.addValue("views", 0);
                bookChapterCategory.setId("bookChapter");

                UsageReportPointCategoryRest datasetCategory = new UsageReportPointCategoryRest();
                datasetCategory.addValue("views", 0);
                datasetCategory.setId("dataset");

                List<UsageReportPointRest> categories =
                    List.of(articleCategory, thesisCategory, otherCategory, bookCategory,
                        bookChapterCategory, datasetCategory);

                // And request the collections global usage report (show top most popular items)
                getClient(adminToken)
                    .perform(get("/api/statistics/usagereports/search/object")
                        .param("category", "publicationCollection-itemReports")
                        .param("uri", "http://localhost:8080/server/api/core/collections/" +
                            collectionNotVisited.getID()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.usagereports", not(empty())))
                    .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                        matchUsageReport(collectionNotVisited.getID() + "_" + TOTAL_ITEMS_VISITS_REPORT_ID,
                            TOP_ITEMS_REPORT_ID, points),
                        matchUsageReport(collectionNotVisited.getID() + "_" + TOP_ITEMS_CITIES_REPORT_ID,
                            TOP_CITIES_REPORT_ID, List.of(pointCity)),
                        matchUsageReport(collectionNotVisited.getID() + "_" + TOTAL_ITEMS_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID, getLastMonthVisitPoints(5)),
                        matchUsageReport(collectionNotVisited.getID() + "_" + TOP_ITEMS_CONTINENTS_REPORT_ID,
                            TOP_CONTINENTS_REPORT_ID, List.of(pointContinent)),
                        matchUsageReport(collectionNotVisited.getID() + "_" + TOP_ITEMS_CATEGORIES_REPORT_ID,
                            TOP_CATEGORIES_REPORT_ID, categories),
                        matchUsageReport(collectionNotVisited.getID() + "_" + TOP_ITEMS_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID, List.of(pointCountry)))));
            }));

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest4))
                .contentType(contentType))
            .andExpect(status().isCreated());
    }

    @Test
    public void usageReportsSearch_Collection_DownloadReports() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item1 = ItemBuilder.createItem(context, collectionNotVisited)
                                .withTitle("Item 1")
                                .build();

        Item item2 = ItemBuilder.createItem(context, collectionNotVisited)
                                .withTitle("Item 2")
                                .build();

        Item item3 = ItemBuilder.createItem(context, collectionNotVisited)
                                .withTitle("Item 3")
                                .build();

        Bitstream bitstream1 = createBitstream(item1, "Bitstream 1");
        Bitstream bitstream2 = createBitstream(item1, "Bitstream 2");
        Bitstream bitstream3 = createBitstream(item2, "Bitstream 3");
        Bitstream bitstream4 = createBitstream(item3, "Bitstream 4");

        getClient().perform(get("/api/core/bitstreams/" + bitstream1.getID() + "/content"))
                   .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream1.getID() + "/content"))
                   .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream2.getID() + "/content"))
                   .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream3.getID() + "/content"))
                   .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream3.getID() + "/content"))
                   .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream3.getID() + "/content"))
                   .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream4.getID() + "/content"))
                   .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream4.getID() + "/content"))
                   .andExpect(status().isOk());

        context.restoreAuthSystemState();

        UsageReportPointDsoTotalVisitsRest expectedPoint1 = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint1.addValue("views", 3);
        expectedPoint1.setType("item");
        expectedPoint1.setLabel("Item 1");
        expectedPoint1.setId(item1.getID().toString());

        UsageReportPointDsoTotalVisitsRest expectedPoint2 = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint2.addValue("views", 3);
        expectedPoint2.setType("item");
        expectedPoint2.setLabel("Item 2");
        expectedPoint2.setId(item2.getID().toString());

        UsageReportPointDsoTotalVisitsRest expectedPoint3 = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint3.addValue("views", 2);
        expectedPoint3.setType("item");
        expectedPoint3.setLabel("Item 3");
        expectedPoint3.setId(item3.getID().toString());

        List<UsageReportPointRest> points = List.of(expectedPoint1, expectedPoint2, expectedPoint3);

        UsageReportPointCityRest pointCity = new UsageReportPointCityRest();
        pointCity.addValue("views", 8);
        pointCity.setId("New York");

        UsageReportPointContinentRest pointContinent = new UsageReportPointContinentRest();
        pointContinent.addValue("views", 8);
        pointContinent.setId("North America");

        UsageReportPointCountryRest pointCountry = new UsageReportPointCountryRest();
        pointCountry.addValue("views", 8);
        pointCountry.setIdAndLabel(Locale.US.getCountry(), Locale.US.getDisplayCountry(context.getCurrentLocale()));

        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                .param("category", "collection-downloadReports")
                .param("uri",
                    "http://localhost:8080/server/api/core/collections/" + collectionNotVisited.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", not(empty())))
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                matchUsageReport(collectionNotVisited.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID,
                    TOP_ITEMS_REPORT_ID, points),
                matchUsageReport(collectionNotVisited.getID() + "_" + TOP_DOWNLOAD_CITIES_REPORT_ID,
                    TOP_CITIES_REPORT_ID, List.of(pointCity)),
                matchUsageReport(collectionNotVisited.getID() + "_" + TOTAL_DOWNLOAD_PER_MONTH_REPORT_ID,
                    TOTAL_VISITS_PER_MONTH_REPORT_ID, getLastMonthVisitPoints(8)),
                matchUsageReport(collectionNotVisited.getID() + "_" + TOP_DOWNLOAD_CONTINENTS_REPORT_ID,
                    TOP_CONTINENTS_REPORT_ID, List.of(pointContinent)),
                matchUsageReport(collectionNotVisited.getID() + "_" + TOP_DOWNLOAD_COUNTRIES_REPORT_ID,
                    TOP_COUNTRIES_REPORT_ID, List.of(pointCountry)))));
    }

    @Test
    public void usageReportsSearch_Community_ItemReports() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context).build();
        collectionNotVisited = CollectionBuilder.createCollection(context, community).build();

        Item item = ItemBuilder.createItem(context, collectionNotVisited)
                               .withEntityType("Publication")
                               .withTitle("My item")
                               .build();
        Item item2 = ItemBuilder.createItem(context, collectionNotVisited)
                                .withEntityType("Patent")
                                .withTitle("My item 2")
                                .build();
        Item item3 = ItemBuilder.createItem(context, collectionNotVisited)
                                .withEntityType("Funding")
                                .withTitle("My item 3")
                                .build();
        Item item4 = ItemBuilder.createItem(context, collectionNotVisited)
                                .withEntityType("Project")
                                .withTitle("My item 4")
                                .build();
        context.restoreAuthSystemState();

        ObjectMapper mapper = new ObjectMapper();

        ViewEventRest viewEventRest = new ViewEventRest();
        viewEventRest.setTargetType("item");
        viewEventRest.setTargetId(item.getID());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest))
                .contentType(contentType))
                   .andExpect(status().isCreated());

        ViewEventRest viewEventRest2 = new ViewEventRest();
        viewEventRest2.setTargetType("item");
        viewEventRest2.setTargetId(item2.getID());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest2))
                .contentType(contentType))
                   .andExpect(status().isCreated());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest2))
                .contentType(contentType))
                   .andExpect(status().isCreated());

        ViewEventRest viewEventRest3 = new ViewEventRest();
        viewEventRest3.setTargetType("item");
        viewEventRest3.setTargetId(item3.getID());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest3))
                .contentType(contentType))
                   .andExpect(status().isCreated());

        Thread.sleep(1000);

        this.statisticsEventListener.addConsumer(
            throwingConsumerWrapper((event) -> {

                UsageReportPointDsoTotalVisitsRest expectedPoint1 = new UsageReportPointDsoTotalVisitsRest();
                expectedPoint1.addValue("views", 1);
                expectedPoint1.setType("item");
                expectedPoint1.setLabel("My item");
                expectedPoint1.setId(item.getID().toString());

                UsageReportPointDsoTotalVisitsRest expectedPoint2 = new UsageReportPointDsoTotalVisitsRest();
                expectedPoint2.addValue("views", 2);
                expectedPoint2.setType("item");
                expectedPoint2.setLabel("My item 2");
                expectedPoint2.setId(item2.getID().toString());

                UsageReportPointDsoTotalVisitsRest expectedPoint3 = new UsageReportPointDsoTotalVisitsRest();
                expectedPoint3.addValue("views", 1);
                expectedPoint3.setType("item");
                expectedPoint3.setLabel("My item 3");
                expectedPoint3.setId(item3.getID().toString());

                UsageReportPointDsoTotalVisitsRest expectedPoint4 = new UsageReportPointDsoTotalVisitsRest();
                expectedPoint4.addValue("views", 1);
                expectedPoint4.setType("item");
                expectedPoint4.setLabel("My item 4");
                expectedPoint4.setId(item4.getID().toString());

                List<UsageReportPointRest> points =
                    List.of(expectedPoint1, expectedPoint2, expectedPoint3, expectedPoint4);

                UsageReportPointCityRest pointCity = new UsageReportPointCityRest();
                pointCity.addValue("views", 5);
                pointCity.setId("New York");

                UsageReportPointContinentRest pointContinent = new UsageReportPointContinentRest();
                pointContinent.addValue("views", 5);
                pointContinent.setId("North America");

                UsageReportPointCountryRest pointCountry = new UsageReportPointCountryRest();
                pointCountry.addValue("views", 5);
                pointCountry.setIdAndLabel(Locale.US.getCountry(),
                    Locale.US.getDisplayCountry(context.getCurrentLocale()));

                UsageReportPointCategoryRest publicationCategory = new UsageReportPointCategoryRest();
                publicationCategory.addValue("views", 1);
                publicationCategory.setId("publication");

                UsageReportPointCategoryRest patentCategory = new UsageReportPointCategoryRest();
                patentCategory.addValue("views", 2);
                patentCategory.setId("patent");

                UsageReportPointCategoryRest fundingCategory = new UsageReportPointCategoryRest();
                fundingCategory.addValue("views", 1);
                fundingCategory.setId("funding");

                UsageReportPointCategoryRest projectCategory = new UsageReportPointCategoryRest();
                projectCategory.addValue("views", 1);
                projectCategory.setId("project");

                UsageReportPointCategoryRest productCategory = new UsageReportPointCategoryRest();
                productCategory.addValue("views", 0);
                productCategory.setId("product");

                UsageReportPointCategoryRest journalCategory = new UsageReportPointCategoryRest();
                journalCategory.addValue("views", 0);
                journalCategory.setId("journal");

                UsageReportPointCategoryRest personCategory = new UsageReportPointCategoryRest();
                personCategory.addValue("views", 0);
                personCategory.setId("person");

                UsageReportPointCategoryRest orgUnitCategory = new UsageReportPointCategoryRest();
                orgUnitCategory.addValue("views", 0);
                orgUnitCategory.setId("orgunit");

                UsageReportPointCategoryRest equipmentCategory = new UsageReportPointCategoryRest();
                equipmentCategory.addValue("views", 0);
                equipmentCategory.setId("equipment");

                UsageReportPointCategoryRest eventCategory = new UsageReportPointCategoryRest();
                eventCategory.addValue("views", 0);
                eventCategory.setId("event");

                List<UsageReportPointRest> categories = List.of(publicationCategory, patentCategory,
                    fundingCategory,
                    projectCategory, productCategory, journalCategory,
                    personCategory, orgUnitCategory,
                    equipmentCategory, eventCategory);
                // And request the collections global usage report (show top most popular items)
                getClient(adminToken)
                    .perform(get("/api/statistics/usagereports/search/object")
                        .param("category", "community-itemReports")
                        .param("uri",
                            "http://localhost:8080/server/api/core/communities/" + community.getID()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.usagereports", not(empty())))
                    .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                        matchUsageReport(community.getID() + "_" + TOTAL_ITEMS_VISITS_REPORT_ID,
                            TOP_ITEMS_REPORT_ID, points),
                        matchUsageReport(community.getID() + "_" + TOP_ITEMS_CITIES_REPORT_ID,
                            TOP_CITIES_REPORT_ID, List.of(pointCity)),
                        matchUsageReport(community.getID() + "_" + TOTAL_ITEMS_VISITS_PER_MONTH_REPORT_ID,
                            TOTAL_VISITS_PER_MONTH_REPORT_ID, getLastMonthVisitPoints(5)),
                        matchUsageReport(community.getID() + "_" + TOP_ITEMS_CONTINENTS_REPORT_ID,
                            TOP_CONTINENTS_REPORT_ID, List.of(pointContinent)),
                        matchUsageReport(community.getID() + "_" + TOP_ITEMS_CATEGORIES_REPORT_ID,
                            TOP_CATEGORIES_REPORT_ID, categories),
                        matchUsageReport(community.getID() + "_" + TOP_ITEMS_COUNTRIES_REPORT_ID,
                            TOP_COUNTRIES_REPORT_ID, List.of(pointCountry)))));
            }));

        ViewEventRest viewEventRest4 = new ViewEventRest();
        viewEventRest4.setTargetType("item");
        viewEventRest4.setTargetId(item4.getID());

        getClient().perform(post("/api/statistics/viewevents")
                .content(mapper.writeValueAsBytes(viewEventRest4))
                .contentType(contentType))
            .andExpect(status().isCreated());
    }

    @Test
    public void usageReportsSearch_Community_DownloadReports() throws Exception {

        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context).build();
        collectionNotVisited = CollectionBuilder.createCollection(context, community).build();

        Item item1 = ItemBuilder.createItem(context, collectionNotVisited)
                                .withTitle("Item 1")
                                .build();

        Item item2 = ItemBuilder.createItem(context, collectionNotVisited)
                                .withTitle("Item 2")
                                .build();

        Item item3 = ItemBuilder.createItem(context, collectionNotVisited)
                                .withTitle("Item 3")
                                .build();

        Bitstream bitstream1 = createBitstream(item1, "Bitstream 1");
        Bitstream bitstream2 = createBitstream(item1, "Bitstream 2");
        Bitstream bitstream3 = createBitstream(item2, "Bitstream 3");
        Bitstream bitstream4 = createBitstream(item3, "Bitstream 4");

        getClient().perform(get("/api/core/bitstreams/" + bitstream1.getID() + "/content"))
                   .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream1.getID() + "/content"))
                   .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream2.getID() + "/content"))
                   .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream3.getID() + "/content"))
                   .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream3.getID() + "/content"))
                   .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream3.getID() + "/content"))
                   .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream4.getID() + "/content"))
                   .andExpect(status().isOk());

        getClient().perform(get("/api/core/bitstreams/" + bitstream4.getID() + "/content"))
                   .andExpect(status().isOk());

        context.restoreAuthSystemState();

        UsageReportPointDsoTotalVisitsRest expectedPoint1 = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint1.addValue("views", 3);
        expectedPoint1.setType("item");
        expectedPoint1.setLabel("Item 1");
        expectedPoint1.setId(item1.getID().toString());

        UsageReportPointDsoTotalVisitsRest expectedPoint2 = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint2.addValue("views", 3);
        expectedPoint2.setType("item");
        expectedPoint2.setLabel("Item 2");
        expectedPoint2.setId(item2.getID().toString());

        UsageReportPointDsoTotalVisitsRest expectedPoint3 = new UsageReportPointDsoTotalVisitsRest();
        expectedPoint3.addValue("views", 2);
        expectedPoint3.setType("item");
        expectedPoint3.setLabel("Item 3");
        expectedPoint3.setId(item3.getID().toString());

        List<UsageReportPointRest> points = List.of(expectedPoint1, expectedPoint2, expectedPoint3);

        UsageReportPointCityRest pointCity = new UsageReportPointCityRest();
        pointCity.addValue("views", 8);
        pointCity.setId("New York");

        UsageReportPointContinentRest pointContinent = new UsageReportPointContinentRest();
        pointContinent.addValue("views", 8);
        pointContinent.setId("North America");

        UsageReportPointCountryRest pointCountry = new UsageReportPointCountryRest();
        pointCountry.addValue("views", 8);
        pointCountry.setIdAndLabel(Locale.US.getCountry(), Locale.US.getDisplayCountry(context.getCurrentLocale()));

        getClient(adminToken)
            .perform(get("/api/statistics/usagereports/search/object")
                .param("category", "community-downloadReports")
                .param("uri", "http://localhost:8080/server/api/core/communities/" + community.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.usagereports", not(empty())))
            .andExpect(jsonPath("$._embedded.usagereports", Matchers.containsInAnyOrder(
                matchUsageReport(community.getID() + "_" + TOTAL_DOWNLOADS_REPORT_ID, TOP_ITEMS_REPORT_ID, points),
                matchUsageReport(community.getID() + "_" + TOP_DOWNLOAD_CITIES_REPORT_ID,
                    TOP_CITIES_REPORT_ID, List.of(pointCity)),
                matchUsageReport(community.getID() + "_" + TOTAL_DOWNLOAD_PER_MONTH_REPORT_ID,
                    TOTAL_VISITS_PER_MONTH_REPORT_ID, getLastMonthVisitPoints(8)),
                matchUsageReport(community.getID() + "_" + TOP_DOWNLOAD_CONTINENTS_REPORT_ID,
                    TOP_CONTINENTS_REPORT_ID, List.of(pointContinent)),
                matchUsageReport(community.getID() + "_" + TOP_DOWNLOAD_COUNTRIES_REPORT_ID,
                    TOP_COUNTRIES_REPORT_ID, List.of(pointCountry)))));
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate();
    }

    private List<UsageReportPointRest> getLastMonthVisitPoints(int viewsLastMonth) {
        return getListOfVisitsPerMonthsPoints(viewsLastMonth, 0);
    }

    private List<UsageReportPointRest> getListOfVisitsPerMonthsPoints(int viewsLastMonth, String monthsBack) {
        LocalDate startDate = toLocalDate(MultiFormatDateParser.parse(monthsBack)).with(ChronoField.DAY_OF_MONTH, 1L);
        LocalDate endDate = LocalDate.now().with(ChronoField.DAY_OF_MONTH, 1L);
        int nrOfMonthsBack = (int) ChronoUnit.MONTHS.between(startDate, endDate);
        return getListOfVisitsPerMonthsPoints(viewsLastMonth, nrOfMonthsBack);
    }

    public static final class StatisticsEventListener implements EventListener {

        public Queue<Consumer<Event>> consumers = new LinkedList<>();

        /* (non-Javadoc)
         * @see org.dspace.services.model.EventListener#getEventNamePrefixes()
         */
        public String[] getEventNamePrefixes() {
            return null;
        }

        /* (non-Javadoc)
         * @see org.dspace.services.model.EventListener#getResourcePrefix()
         */
        public String getResourcePrefix() {
            return null;
        }

        public void addConsumer(Consumer<Event>... consumers) {
            this.consumers.addAll(List.of(consumers));
        }

        public Queue<Consumer<Event>> getConsumers() {
            return this.consumers;
        }

        public void clearConsumers() {
            this.consumers.clear();
        }

        /* (non-Javadoc)
         * @see org.dspace.services.model.EventListener#receiveEvent(org.dspace.services.model.Event)
         */
        public void receiveEvent(Event event) {
            Consumer<Event> poll = this.consumers.poll();
            if (poll != null) {
                poll.accept(event);
            }
        }
    }

    private List<UsageReportPointRest> getListOfVisitsPerMonthsPoints(int viewsLastMonth, int nrOfMonthsBack) {
        List<UsageReportPointRest> expectedPoints = new ArrayList<>();
        Calendar cal = Calendar.getInstance(context.getCurrentLocale());
        for (int i = 0; i <= nrOfMonthsBack; i++) {
            UsageReportPointDateRest expectedPoint = new UsageReportPointDateRest();
            if (i > 0) {
                expectedPoint.addValue("views", 0);
            } else {
                expectedPoint.addValue("views", viewsLastMonth);
            }
            String month = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, context.getCurrentLocale());
            expectedPoint.setId(month + " " + cal.get(Calendar.YEAR));

            expectedPoints.add(expectedPoint);
            cal.add(Calendar.MONTH, -1);
        }
        return expectedPoints;
    }

    private UsageReportPointDsoTotalVisitsRest getExpectedDsoViews(DSpaceObject dso, int views) {
        UsageReportPointDsoTotalVisitsRest point = new UsageReportPointDsoTotalVisitsRest();

        point.addValue("views", views);
        point.setType(StringUtils.lowerCase(Constants.typeText[dso.getType()]));
        point.setId(dso.getID().toString());
        point.setLabel(dso.getName());

        return point;
    }

    private UsageReportPointCountryRest getExpectedCountryViews(String id, String label, int views) {
        UsageReportPointCountryRest point = new UsageReportPointCountryRest();

        point.addValue("views", views);
        point.setIdAndLabel(id, label);

        return point;
    }

    private UsageReportPointCityRest getExpectedCityViews(String id, int views) {
        UsageReportPointCityRest point = new UsageReportPointCityRest();

        point.addValue("views", views);
        point.setId(id);

        return point;
    }
}
