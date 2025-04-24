/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.ItemBuilder.createItem;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.dspace.util.WorkbookUtils.getRowValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import net.minidev.json.JSONArray;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dspace.app.rest.converter.DSpaceRunnableParameterConverter;
import org.dspace.app.rest.matcher.BitstreamMatcher;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.matcher.ProcessMatcher;
import org.dspace.app.rest.matcher.ScriptMatcher;
import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ProcessBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.ProcessStatus;
import org.dspace.content.authority.DCInputAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.integration.crosswalks.ReferCrosswalk;
import org.dspace.content.integration.crosswalks.StreamDisseminationCrosswalkMapper;
import org.dspace.core.CrisConstants;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.Process;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.service.ProcessService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

public class ScriptRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ProcessService processService;

    @Autowired
    private ResourcePolicyService resourcePolicyService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private List<ScriptConfiguration<?>> scriptConfigurations;

    @Autowired
    private DSpaceRunnableParameterConverter dSpaceRunnableParameterConverter;

    @Autowired
    private MetadataAuthorityService metadataAuthorityService;

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    @After
    public void after() throws SubmissionConfigReaderException {
        DSpaceServicesFactory.getInstance().getConfigurationService().reloadConfig();
        metadataAuthorityService.clearCache();
        choiceAuthorityService.clearCache();
        // the DCInputAuthority has an internal cache of the DCInputReader
        DCInputAuthority.reset();
        DCInputAuthority.getPluginNames();
    }

    @Test
    public void givenMultilanguageItemsWhenSchedulingExportThenUseRequestLanguageWhileSearching() throws Exception {
        context.turnOffAuthorisationSystem();

        String italianLanguage = "it";
        String ukranianLanguage = "uk";
        String[] supportedLanguage = { italianLanguage, ukranianLanguage };
        configurationService.setProperty("webui.supported.locales", supportedLanguage);
        metadataAuthorityService.clearCache();
        choiceAuthorityService.clearCache();
        // the DCInputAuthority has an internal cache of the DCInputReader
        DCInputAuthority.reset();
        DCInputAuthority.getPluginNames();

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
        parameters.add(new DSpaceCommandLineParameter("-t", "Publication"));
        parameters.add(new DSpaceCommandLineParameter("-f", "publication-json"));
        parameters.add(new DSpaceCommandLineParameter("-sf", "language=Iталiйська,equals"));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                          .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        String token = getAuthToken(admin.getEmail(), password);
        List<ProcessStatus> acceptableProcessStatuses = new LinkedList<>();
        acceptableProcessStatuses.addAll(Arrays.asList(ProcessStatus.SCHEDULED,
                                                       ProcessStatus.RUNNING,
                                                       ProcessStatus.COMPLETED));

        List<AtomicReference<Integer>> processes = new ArrayList<>();
        AtomicReference<Integer> idRef1 = new AtomicReference<>();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity, "123456789/language-test-1")
                .withName("Collection 1")
                .withEntityType("Publication")
                .build();

            String italianTitle = "Item Italiano";
            ItemBuilder.createItem(context, col1)
                .withTitle(italianTitle)
                .withIssueDate("2022-07-12")
                .withAuthor("Italiano, Multilanguage")
                .withLanguage(italianLanguage)
                .build();

            String ukranianTitle = "Item Yкраїнська";
            ItemBuilder.createItem(context, col1)
                .withTitle(ukranianTitle)
                .withIssueDate("2022-07-12")
                .withAuthor("Yкраїнська, Multilanguage")
                .withLanguage(ukranianLanguage)
                .build();

        context.restoreAuthSystemState();

        try {

            getClient(token)
                    .perform(
                            multipart("/api/system/scripts/bulk-item-export/processes")
                             .param("properties", new Gson().toJson(list))
                             .header("Accept-Language", ukranianLanguage)
                     )
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("bulk-item-export",
                                                        String.valueOf(admin.getID()),
                                                        parameters,
                                                        acceptableProcessStatuses))))
                    .andDo(result -> idRef1
                            .set(read(result.getResponse().getContentAsString(), "$.processId")));
            MvcResult mvcResult = getClient(token)
                    .perform(get("/api/system/processes/" + idRef1.get() + "/files"))
                    .andReturn();

            processes.add(idRef1);

            JSONArray publicationsJsonId = read(mvcResult.getResponse().getContentAsString(),
                    "$._embedded.files[?(@.name=='publications.json')].id");
            getClient(token)
                    .perform(get("/api/core/bitstreams/" + publicationsJsonId.get(0).toString() + "/content"))
                    .andExpect(
                            jsonPath(
                                "$.items",
                                allOf(
                                    hasJsonPath("[*].language", contains(italianLanguage)),
                                    hasJsonPath("[*].title", contains(italianTitle)),
                                    not(hasJsonPath("[*].language", contains(ukranianLanguage))),
                                    not(hasJsonPath("[*].title", contains(ukranianTitle)))
                                )
                            )
                    );

            AtomicReference<Integer> idRef2 = new AtomicReference<>();
            parameters.clear();

            parameters.add(new DSpaceCommandLineParameter("-t", "Publication"));
            parameters.add(new DSpaceCommandLineParameter("-f", "publication-json"));
            parameters.add(new DSpaceCommandLineParameter("-sf", "language=Italiano,equals"));

            list = parameters
                    .stream()
                    .map(
                            dSpaceCommandLineParameter ->
                                dSpaceRunnableParameterConverter
                                    .convert(dSpaceCommandLineParameter, Projection.DEFAULT)
                    )
                    .collect(Collectors.toList());

            getClient(token)
            .perform(
                    multipart("/api/system/scripts/bulk-item-export/processes")
                     .param("properties", new Gson().toJson(list))
                     .header("Accept-Language", italianLanguage)
             )
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$", is(
                    ProcessMatcher.matchProcess("bulk-item-export",
                                                String.valueOf(admin.getID()),
                                                parameters,
                                                acceptableProcessStatuses))))
            .andDo(result -> idRef2
                    .set(read(result.getResponse().getContentAsString(), "$.processId")));

            processes.add(idRef2);

            mvcResult = getClient(token)
                    .perform(get("/api/system/processes/" + idRef2.get() + "/files"))
                    .andReturn();
            publicationsJsonId = read(mvcResult.getResponse().getContentAsString(),
                    "$._embedded.files[?(@.name=='publications.json')].id");
            getClient(token)
                    .perform(
                            get("/api/core/bitstreams/" + publicationsJsonId.get(0).toString() + "/content")
                    )
                    .andExpect(
                            jsonPath(
                                "$.items",
                                allOf(
                                    hasJsonPath("[*].language", contains(italianLanguage)),
                                    hasJsonPath("[*].title", contains(italianTitle)),
                                    not(hasJsonPath("[*].language", contains(ukranianLanguage))),
                                    not(hasJsonPath("[*].title", contains(ukranianTitle)))
                                )
                            )
                    );
        } finally {
            for (AtomicReference<Integer> atomicReference : processes) {
                ProcessBuilder.deleteProcess(atomicReference .get());
            }
        }

    }

    @Test
    public void findAllScriptsWithAdminTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/scripts")
                        .param("size", "100"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.scripts", containsInAnyOrder(
                            scriptConfigurations
                                .stream()
                                .map(scriptConfiguration -> ScriptMatcher.matchScript(
                                    scriptConfiguration.getName(),
                                    scriptConfiguration.getDescription()
                                ))
                                .collect(Collectors.toList())
                        )));
    }

    @Test
    public void findAllScriptsSortedAlphabeticallyTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/scripts")
                        .param("size", String.valueOf(scriptConfigurations.size())))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.scripts", contains(
                            scriptConfigurations
                                .stream()
                                .sorted(Comparator.comparing(ScriptConfiguration::getName))
                                .map(scriptConfiguration -> ScriptMatcher.matchScript(
                                    scriptConfiguration.getName(),
                                    scriptConfiguration.getDescription()
                                ))
                                .collect(Collectors.toList())
                        )));
    }

    @Test
    public void findAllScriptsGenericLoggedInUserTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/system/scripts"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page",
                                            is(PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2))));
    }

    @Test
    public void findAllScriptsAnonymousUserTest() throws Exception {
        // this should be changed once we allow anonymous user to execute some scripts
        getClient().perform(get("/api/system/scripts"))
                   .andExpect(status().isOk());
    }

    @Test
    public void findAllScriptsLocalAdminsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson comAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("comAdmin@example.com")
                .withPassword(password).build();
        EPerson colAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("colAdmin@example.com")
                .withPassword(password).build();
        EPerson itemAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("itemAdmin@example.com")
                .withPassword(password).build();
        Community community = CommunityBuilder.createCommunity(context)
                                          .withName("Community")
                                          .withAdminGroup(comAdmin)
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                                                .withName("Collection")
                                                .withAdminGroup(colAdmin)
                                                .build();
        ItemBuilder.createItem(context, collection).withAdminUser(itemAdmin)
                .withTitle("Test item to curate").build();
        context.restoreAuthSystemState();
        ScriptConfiguration curateScriptConfiguration =
                scriptConfigurations.stream().filter(scriptConfiguration
                        -> scriptConfiguration.getName().equals("curate"))
            .findAny().get();

        // the local admins have at least access to the curate script
        // and not access to process-cleaner script
        String comAdminToken = getAuthToken(comAdmin.getEmail(), password);
        getClient(comAdminToken).perform(get("/api/system/scripts").param("size", "100"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.scripts", Matchers.hasItem(
                                ScriptMatcher.matchScript(curateScriptConfiguration.getName(),
                                        curateScriptConfiguration.getDescription()))))
                        .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(1)));
        String colAdminToken = getAuthToken(colAdmin.getEmail(), password);
        getClient(colAdminToken).perform(get("/api/system/scripts").param("size", "100"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.scripts", Matchers.hasItem(
                                ScriptMatcher.matchScript(curateScriptConfiguration.getName(),
                                        curateScriptConfiguration.getDescription()))))
                        .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(1)));
        String itemAdminToken = getAuthToken(itemAdmin.getEmail(), password);
        getClient(itemAdminToken).perform(get("/api/system/scripts").param("size", "100"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.scripts", Matchers.hasItem(
                                ScriptMatcher.matchScript(curateScriptConfiguration.getName(),
                                        curateScriptConfiguration.getDescription()))))
                        .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    public void findAllScriptsPaginationTest() throws Exception {
        List<ScriptConfiguration> alphabeticScripts =
            scriptConfigurations.stream()
                                .sorted(Comparator.comparing(ScriptConfiguration::getName))
                                .collect(Collectors.toList());

        int totalPages = scriptConfigurations.size();
        int lastPage = totalPages - 1;

        String token = getAuthToken(admin.getEmail(), password);

        // NOTE: the scripts are always returned in alphabetical order by fully qualified class name.
        getClient(token).perform(get("/api/system/scripts").param("size", "1"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.scripts", Matchers.not(Matchers.hasItem(
                ScriptMatcher.matchScript(scriptConfigurations.get(10).getName(),
                    scriptConfigurations.get(10).getDescription())
                        ))))
                        .andExpect(jsonPath("$._embedded.scripts", hasItem(
                                ScriptMatcher.matchScript(alphabeticScripts.get(0).getName(),
                                                          alphabeticScripts.get(0).getDescription())
                        )))
                        .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                            Matchers.containsString("/api/system/scripts?"),
                            Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
                        .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                            Matchers.containsString("/api/system/scripts?"),
                            Matchers.containsString("size=1"))))
                        .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                            Matchers.containsString("/api/system/scripts?"),
                            Matchers.containsString("page=1"), Matchers.containsString("size=1"))))
                        .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                                Matchers.containsString("/api/system/scripts?"),
                                Matchers.containsString("page=" + lastPage), Matchers.containsString("size=1"))))
                        .andExpect(jsonPath("$.page.size", is(1)))
                        .andExpect(jsonPath("$.page.number", is(0)))
                        .andExpect(jsonPath("$.page.totalPages", is(totalPages)))
                        .andExpect(jsonPath("$.page.totalElements", is(totalPages)));


        getClient(token).perform(get("/api/system/scripts").param("size", "1").param("page", "1"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$._embedded.scripts",
                                not(
                                    hasItem(
                                        ScriptMatcher.matchScript(
                                            scriptConfigurations.get(10).getName(),
                                            scriptConfigurations.get(10).getDescription()
                                        )
                                    )
                                )
                            )
                        )
                        .andExpect(jsonPath("$._embedded.scripts", hasItem(
                                ScriptMatcher.matchScript(alphabeticScripts.get(1).getName(),
                                                          alphabeticScripts.get(1).getDescription())
                        )))
                        .andExpect(jsonPath("$._links.first.href", Matchers.allOf(
                            Matchers.containsString("/api/system/scripts?"),
                            Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
                        .andExpect(jsonPath("$._links.prev.href", Matchers.allOf(
                            Matchers.containsString("/api/system/scripts?"),
                            Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
                        .andExpect(jsonPath("$._links.self.href", Matchers.allOf(
                            Matchers.containsString("/api/system/scripts?"),
                            Matchers.containsString("page=1"), Matchers.containsString("size=1"))))
                        .andExpect(jsonPath("$._links.next.href", Matchers.allOf(
                            Matchers.containsString("/api/system/scripts?"),
                            Matchers.containsString("page=2"), Matchers.containsString("size=1"))))
                        .andExpect(jsonPath("$._links.last.href", Matchers.allOf(
                                Matchers.containsString("/api/system/scripts?"),
                                Matchers.containsString("page=" + lastPage), Matchers.containsString("size=1"))))
                        .andExpect(jsonPath("$.page.size", is(1)))
                        .andExpect(jsonPath("$.page.number", is(1)))
                        .andExpect(jsonPath("$.page.totalPages", is(totalPages)))
                        .andExpect(jsonPath("$.page.totalElements", is(totalPages)));
    }

    @Test
    public void findOneScriptByNameTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/scripts/mock-script"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", ScriptMatcher
                            .matchMockScript(
                                scriptConfigurations
                                    .stream()
                                    .filter(scriptConfiguration
                                                -> scriptConfiguration.getName().equals("mock-script"))
                                    .findAny()
                                    .orElseThrow()
                                    .getOptions()
                            )
                        ));
    }

    @Test
    public void findOneScriptByNameLocalAdminsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson comAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("comAdmin@example.com")
                .withPassword(password).build();
        EPerson colAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("colAdmin@example.com")
                .withPassword(password).build();
        EPerson itemAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("itemAdmin@example.com")
                .withPassword(password).build();
        Community community = CommunityBuilder.createCommunity(context)
                                          .withName("Community")
                                          .withAdminGroup(comAdmin)
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                                                .withName("Collection")
                                                .withAdminGroup(colAdmin)
                                                .build();
        ItemBuilder.createItem(context, collection).withAdminUser(itemAdmin)
                .withTitle("Test item to curate").build();
        context.restoreAuthSystemState();
        ScriptConfiguration curateScriptConfiguration =
                scriptConfigurations.stream().filter(scriptConfiguration
                        -> scriptConfiguration.getName().equals("curate"))
            .findAny().get();

        String comAdminToken = getAuthToken(comAdmin.getEmail(), password);
        String colAdminToken = getAuthToken(colAdmin.getEmail(), password);
        String itemAdminToken = getAuthToken(itemAdmin.getEmail(), password);
        getClient(comAdminToken).perform(get("/api/system/scripts/" + curateScriptConfiguration.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", ScriptMatcher
                .matchScript(
                        curateScriptConfiguration.getName(),
                        curateScriptConfiguration.getDescription())));
        getClient(colAdminToken).perform(get("/api/system/scripts/" + curateScriptConfiguration.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", ScriptMatcher
                .matchScript(
                        curateScriptConfiguration.getName(),
                        curateScriptConfiguration.getDescription())));
        getClient(itemAdminToken).perform(get("/api/system/scripts/" + curateScriptConfiguration.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", ScriptMatcher
                .matchScript(
                        curateScriptConfiguration.getName(),
                        curateScriptConfiguration.getDescription())));
    }

    @Test
    public void findBulkImportScriptByAdminsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson comAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("comAdmin@example.com")
                .withPassword(password).build();
        EPerson colAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("colAdmin@example.com")
                .withPassword(password).build();
        EPerson itemAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("itemAdmin@example.com")
                .withPassword(password).build();
        Community community = CommunityBuilder.createCommunity(context)
                                          .withName("Community")
                                          .withAdminGroup(comAdmin)
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                                                .withName("Collection")
                                                .withAdminGroup(colAdmin)
                                                .build();
        ItemBuilder.createItem(context, collection).withAdminUser(itemAdmin)
                .withTitle("Test item").build();
        context.restoreAuthSystemState();
        ScriptConfiguration bulkImportScriptConfiguration =
                scriptConfigurations.stream().filter(scriptConfiguration
                        -> scriptConfiguration.getName().equals("bulk-import"))
            .findAny().get();

        String comAdminToken = getAuthToken(comAdmin.getEmail(), password);
        String colAdminToken = getAuthToken(colAdmin.getEmail(), password);
        String itemAdminToken = getAuthToken(itemAdmin.getEmail(), password);
        getClient(comAdminToken).perform(get("/api/system/scripts/" + bulkImportScriptConfiguration.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", ScriptMatcher
                .matchScript(
                        bulkImportScriptConfiguration.getName(),
                        bulkImportScriptConfiguration.getDescription())));
        getClient(colAdminToken).perform(get("/api/system/scripts/" + bulkImportScriptConfiguration.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", ScriptMatcher
                .matchScript(
                        bulkImportScriptConfiguration.getName(),
                        bulkImportScriptConfiguration.getDescription())));
        getClient(itemAdminToken).perform(get("/api/system/scripts/" + bulkImportScriptConfiguration.getName()))
            .andExpect(status().isForbidden());
    }

    @Test
    public void findBulkAccessControlScriptByAdminsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson comAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("comAdmin@example.com")
                .withPassword(password).build();
        EPerson colAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("colAdmin@example.com")
                .withPassword(password).build();
        EPerson itemAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("itemAdmin@example.com")
                .withPassword(password).build();
        Community community = CommunityBuilder.createCommunity(context)
                .withName("Community")
                .withAdminGroup(comAdmin)
                .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                .withName("Collection")
                .withAdminGroup(colAdmin)
                .build();
        ItemBuilder.createItem(context, collection).withAdminUser(itemAdmin)
                .withTitle("Test item").build();
        context.restoreAuthSystemState();
        ScriptConfiguration scriptConfiguration =
                scriptConfigurations.stream().filter(configuration
                                -> configuration.getName().equals("bulk-access-control"))
                        .findAny().get();

        String comAdminToken = getAuthToken(comAdmin.getEmail(), password);
        String colAdminToken = getAuthToken(colAdmin.getEmail(), password);
        String itemAdminToken = getAuthToken(itemAdmin.getEmail(), password);
        getClient(comAdminToken).perform(get("/api/system/scripts/" + scriptConfiguration.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", ScriptMatcher
                        .matchScript(
                                scriptConfiguration.getName(),
                                scriptConfiguration.getDescription())));
        getClient(colAdminToken).perform(get("/api/system/scripts/" + scriptConfiguration.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", ScriptMatcher
                        .matchScript(
                                scriptConfiguration.getName(),
                                scriptConfiguration.getDescription())));
        getClient(itemAdminToken).perform(get("/api/system/scripts/" + scriptConfiguration.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", ScriptMatcher
                        .matchScript(
                                scriptConfiguration.getName(),
                                scriptConfiguration.getDescription())));
    }

    @Test
    public void findCollectionExportScriptByAdminsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson comAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("comAdmin@example.com")
                .withPassword(password).build();
        EPerson colAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("colAdmin@example.com")
                .withPassword(password).build();
        EPerson itemAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("itemAdmin@example.com")
                .withPassword(password).build();
        Community community = CommunityBuilder.createCommunity(context)
                .withName("Community")
                .withAdminGroup(comAdmin)
                .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                .withName("Collection")
                .withAdminGroup(colAdmin)
                .build();
        ItemBuilder.createItem(context, collection).withAdminUser(itemAdmin)
                .withTitle("Test item").build();
        context.restoreAuthSystemState();
        ScriptConfiguration scriptConfiguration =
                scriptConfigurations.stream().filter(configuration
                                -> configuration.getName().equals("collection-export"))
                        .findAny().get();

        String comAdminToken = getAuthToken(comAdmin.getEmail(), password);
        String colAdminToken = getAuthToken(colAdmin.getEmail(), password);
        String itemAdminToken = getAuthToken(itemAdmin.getEmail(), password);
        getClient(comAdminToken).perform(get("/api/system/scripts/" + scriptConfiguration.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", ScriptMatcher
                        .matchScript(
                                scriptConfiguration.getName(),
                                scriptConfiguration.getDescription())));
        getClient(colAdminToken).perform(get("/api/system/scripts/" + scriptConfiguration.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", ScriptMatcher
                        .matchScript(
                                scriptConfiguration.getName(),
                                scriptConfiguration.getDescription())));
        getClient(itemAdminToken).perform(get("/api/system/scripts/" + scriptConfiguration.getName()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void findItemExportScriptTest() throws Exception {
        ScriptConfiguration scriptConfiguration =
                scriptConfigurations.stream().filter(configuration
                                -> configuration.getName().equals("item-export"))
                        .findAny().get();

        getClient().perform(get("/api/system/scripts/" + scriptConfiguration.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", ScriptMatcher
                        .matchScript(
                                scriptConfiguration.getName(),
                                scriptConfiguration.getDescription())));
    }

    @Test
    public void findBulkItemExportScriptByAdminsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson comAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("comAdmin@example.com")
                .withPassword(password).build();
        EPerson colAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("colAdmin@example.com")
                .withPassword(password).build();
        EPerson itemAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("itemAdmin@example.com")
                .withPassword(password).build();
        Community community = CommunityBuilder.createCommunity(context)
                .withName("Community")
                .withAdminGroup(comAdmin)
                .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                .withName("Collection")
                .withAdminGroup(colAdmin)
                .build();
        ItemBuilder.createItem(context, collection).withAdminUser(itemAdmin)
                .withTitle("Test item").build();
        context.restoreAuthSystemState();
        ScriptConfiguration scriptConfiguration =
                scriptConfigurations.stream().filter(configuration
                                -> configuration.getName().equals("bulk-item-export"))
                        .findAny().get();

        String comAdminToken = getAuthToken(comAdmin.getEmail(), password);
        String colAdminToken = getAuthToken(colAdmin.getEmail(), password);
        String itemAdminToken = getAuthToken(itemAdmin.getEmail(), password);
        String loggedInToken = getAuthToken(eperson.getEmail(), password);
        getClient(comAdminToken).perform(get("/api/system/scripts/" + scriptConfiguration.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", ScriptMatcher
                        .matchScript(
                                scriptConfiguration.getName(),
                                scriptConfiguration.getDescription())));
        getClient(colAdminToken).perform(get("/api/system/scripts/" + scriptConfiguration.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", ScriptMatcher
                        .matchScript(
                                scriptConfiguration.getName(),
                                scriptConfiguration.getDescription())));
        getClient(itemAdminToken).perform(get("/api/system/scripts/" + scriptConfiguration.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", ScriptMatcher
                        .matchScript(
                                scriptConfiguration.getName(),
                                scriptConfiguration.getDescription())));
        getClient(loggedInToken).perform(get("/api/system/scripts/" + scriptConfiguration.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", ScriptMatcher
                        .matchScript(
                                scriptConfiguration.getName(),
                                scriptConfiguration.getDescription())));
        getClient().perform(get("/api/system/scripts/" + scriptConfiguration.getName()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneScriptByNameNotAuthenticatedTest() throws Exception {
        getClient().perform(get("/api/system/scripts/mock-script"))
                        .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneScriptByNameTestAccessDenied() throws Exception {
        String[] excludedScripts = new String[] {"curate", "bulk-import",
                "item-export", "bulk-item-export", "bulk-access-control",
                "collection-export"};

        String token = getAuthToken(eperson.getEmail(), password);
        scriptConfigurations.stream().filter(scriptConfiguration ->
                        !StringUtils.equalsAny(scriptConfiguration.getName(), excludedScripts))
                .forEach(scriptConfiguration -> {
                    try {
                        getClient(token).perform(get("/api/system/scripts/" + scriptConfiguration.getName()))
                                .andExpect(status().isForbidden());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    @Test
    public void findOneScriptByInvalidNameBadRequestExceptionTest() throws Exception {
        getClient().perform(get("/api/system/scripts/mock-script-invalid"))
                   .andExpect(status().isNotFound());
    }

    /**
     * This test will create a basic structure of communities, collections and items with some local admins at each
     * level and verify that the local admins, nor generic users can run scripts reserved to administrator
     * (i.e. default one that don't override the default
     * {@link ScriptConfiguration#isAllowedToExecute(org.dspace.core.Context, List)} method implementation
     */
    @Test
    public void postProcessNonAdminAuthorizeException() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson comAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("comAdmin@example.com")
                .withPassword(password).build();
        EPerson colAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("colAdmin@example.com")
                .withPassword(password).build();
        EPerson itemAdmin = EPersonBuilder.createEPerson(context)
                .withEmail("itemAdmin@example.com")
                .withPassword(password).build();
        Community community = CommunityBuilder.createCommunity(context)
                                          .withName("Community")
                                          .withAdminGroup(comAdmin)
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                                                .withName("Collection")
                                                .withAdminGroup(colAdmin)
                                                .build();
        Item item = ItemBuilder.createItem(context, collection).withAdminUser(itemAdmin)
                                .withTitle("Test item to curate").build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        String comAdmin_token = getAuthToken(eperson.getEmail(), password);
        String colAdmin_token = getAuthToken(eperson.getEmail(), password);
        String itemAdmin_token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(multipart("/api/system/scripts/mock-script/processes"))
                        .andExpect(status().isForbidden());
        getClient(comAdmin_token).perform(multipart("/api/system/scripts/mock-script/processes"))
                        .andExpect(status().isForbidden());
        getClient(colAdmin_token).perform(multipart("/api/system/scripts/mock-script/processes"))
                        .andExpect(status().isForbidden());
        getClient(itemAdmin_token).perform(multipart("/api/system/scripts/mock-script/processes"))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void postProcessAnonymousAuthorizeException() throws Exception {
        getClient().perform(multipart("/api/system/scripts/mock-script/processes"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void postProcessAdminWrongOptionsException() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        AtomicReference<Integer> idRef = new AtomicReference<>();

        try {
            getClient(token)
                    .perform(multipart("/api/system/scripts/mock-script/processes"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("mock-script",
                                                        String.valueOf(admin.getID()), new LinkedList<>(),
                                                        ProcessStatus.FAILED))))
                    .andDo(result -> idRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId")));
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }


    }

    @Test
    public void postProcessAdminNoOptionsFailedStatus() throws Exception {

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-z", "test"));
        parameters.add(new DSpaceCommandLineParameter("-q", null));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                          .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        String token = getAuthToken(admin.getEmail(), password);

        AtomicReference<Integer> idRef = new AtomicReference<>();

        try {
            getClient(token)
                    .perform(multipart("/api/system/scripts/mock-script/processes")
                                 .param("properties", new ObjectMapper().writeValueAsString(list)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("mock-script",
                                                        String.valueOf(admin.getID()), parameters,
                                                        ProcessStatus.FAILED))))
                    .andDo(result -> idRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId")));
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }

    @Test
    public void postProcessNonExistingScriptNameException() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(multipart("/api/system/scripts/mock-script-invalid/processes"))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void postProcessAdminWithOptionsSuccess() throws Exception {
        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-r", "test"));
        parameters.add(new DSpaceCommandLineParameter("-i", null));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                          .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        String token = getAuthToken(admin.getEmail(), password);
        List<ProcessStatus> acceptableProcessStatuses = new LinkedList<>();
        acceptableProcessStatuses.addAll(Arrays.asList(ProcessStatus.SCHEDULED,
                                                       ProcessStatus.RUNNING,
                                                       ProcessStatus.COMPLETED));

        AtomicReference<Integer> idRef = new AtomicReference<>();

        try {
            getClient(token)
                    .perform(multipart("/api/system/scripts/mock-script/processes")
                                 .param("properties", new ObjectMapper().writeValueAsString(list)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("mock-script",
                                                        String.valueOf(admin.getID()),
                                                        parameters,
                                                        acceptableProcessStatuses))))
                    .andDo(result -> idRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId")));
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }

    @Test
    public void postProcessAndVerifyOutput() throws Exception {
        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-r", "test"));
        parameters.add(new DSpaceCommandLineParameter("-i", null));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                          .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        String token = getAuthToken(admin.getEmail(), password);
        List<ProcessStatus> acceptableProcessStatuses = new LinkedList<>();
        acceptableProcessStatuses.addAll(Arrays.asList(ProcessStatus.SCHEDULED,
                                                       ProcessStatus.RUNNING,
                                                       ProcessStatus.COMPLETED));

        AtomicReference<Integer> idRef = new AtomicReference<>();

        try {
            getClient(token)
                    .perform(multipart("/api/system/scripts/mock-script/processes")
                                 .param("properties", new ObjectMapper().writeValueAsString(list)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("mock-script",
                                                        String.valueOf(admin.getID()),
                                                        parameters,
                                                        acceptableProcessStatuses))))
                    .andDo(result -> idRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId")));


            Process process = processService.find(context, idRef.get());
            Bitstream bitstream = processService.getBitstream(context, process, Process.OUTPUT_TYPE);


            getClient(token).perform(get("/api/system/processes/" + idRef.get() + "/output"))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$", BitstreamMatcher
                                    .matchBitstreamEntryWithoutEmbed(bitstream.getID(), bitstream.getSizeBytes())));


            MvcResult mvcResult = getClient(token)
                    .perform(get("/api/core/bitstreams/" + bitstream.getID() + "/content")).andReturn();
            String content = mvcResult.getResponse().getContentAsString();

            assertThat(content,
                CoreMatchers.containsString("INFO mock-script - " + process.getID() + " @ The script has started"));
            assertThat(content, CoreMatchers.containsString(
                               "INFO mock-script - " + process.getID() + " @ Logging INFO for Mock DSpace Script"));
            assertThat(content,
                       CoreMatchers.containsString(
                               "ERROR mock-script - " + process.getID() + " @ Logging ERROR for Mock DSpace Script"));
            assertThat(content,
                       CoreMatchers.containsString("WARNING mock-script - " + process
                               .getID() + " @ Logging WARNING for Mock DSpace Script"));
            assertThat(content, CoreMatchers
                    .containsString("INFO mock-script - " + process.getID() + " @ The script has completed"));




        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }




    @Test
    public void postProcessAdminWithWrongContentTypeBadRequestException() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token)
                .perform(post("/api/system/scripts/mock-script/processes"))
                .andExpect(status().isBadRequest());

        getClient(token).perform(post("/api/system/scripts/mock-script-invalid/processes"))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void postProcessAdminWithFileSuccess() throws Exception {
        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-r", "test"));
        parameters.add(new DSpaceCommandLineParameter("-i", null));


        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        String bitstreamContent = "Hello, World!";
        MockMultipartFile bitstreamFile = new MockMultipartFile("file",
                                                                "helloProcessFile.txt", MediaType.TEXT_PLAIN_VALUE,
                                                                bitstreamContent.getBytes());
        parameters.add(new DSpaceCommandLineParameter("-f", "helloProcessFile.txt"));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                          .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());

        String token = getAuthToken(admin.getEmail(), password);
        List<ProcessStatus> acceptableProcessStatuses = new LinkedList<>();
        acceptableProcessStatuses.addAll(Arrays.asList(ProcessStatus.SCHEDULED,
                                                       ProcessStatus.RUNNING,
                                                       ProcessStatus.COMPLETED));

        AtomicReference<Integer> idRef = new AtomicReference<>();

        try {
            getClient(token)
                    .perform(multipart("/api/system/scripts/mock-script/processes")
                                 .file(bitstreamFile)
                                 .characterEncoding("UTF-8")
                                 .param("properties", new ObjectMapper().writeValueAsString(list)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$", is(
                            ProcessMatcher.matchProcess("mock-script",
                                                        String.valueOf(admin.getID()),
                                                        parameters,
                                                        acceptableProcessStatuses))))
                    .andDo(result -> idRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId")));
        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }

    @Test
    public void TrackSpecialGroupduringprocessSchedulingTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group specialGroup = GroupBuilder.createGroup(context)
                                         .withName("Special Group")
                                         .addMember(admin)
                                         .build();

        context.restoreAuthSystemState();

        configurationService.setProperty("authentication-password.login.specialgroup", specialGroup.getName());

        LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-r", "test"));
        parameters.add(new DSpaceCommandLineParameter("-i", null));

        List<ParameterValueRest> list = parameters.stream()
                                                  .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                                                  .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
                                                  .collect(Collectors.toList());



        String token = getAuthToken(admin.getEmail(), password);
        List<ProcessStatus> acceptableProcessStatuses = new LinkedList<>();
        acceptableProcessStatuses.addAll(Arrays.asList(ProcessStatus.SCHEDULED,
                                                       ProcessStatus.RUNNING,
                                                       ProcessStatus.COMPLETED));

        AtomicReference<Integer> idRef = new AtomicReference<>();

        try {
            getClient(token).perform(post("/api/system/scripts/mock-script/processes")
                            .contentType("multipart/form-data")
                            .param("properties", new Gson().toJson(list)))
                            .andExpect(status().isAccepted())
                            .andExpect(jsonPath("$", is(ProcessMatcher.matchProcess("mock-script",
                                                        String.valueOf(admin.getID()),
                                                        parameters, acceptableProcessStatuses))))
                            .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.processId")));

            Process process = processService.find(context, idRef.get());
            List<Group> groups = process.getGroups();
            boolean isPresent = false;
            for (Group group : groups) {
                if (group.getID().equals(specialGroup.getID())) {
                    isPresent = true;
                }
            }
            assertTrue(isPresent);

        } finally {
            ProcessBuilder.deleteProcess(idRef.get());
        }
    }

    @Test
    public void postProcessWithAnonymousUser() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson user = EPersonBuilder.createEPerson(context)
            .withEmail("test@user.it")
            .withNameInMetadata("Test", "User")
            .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection 1")
            .withEntityType("Publication")
            .build();

        Item item = ItemBuilder.createItem(context, col1)
            .withTitle("Public item 1")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald")
            .withAuthor("Doe, John")
            .build();

        context.restoreAuthSystemState();

        File xml = new File(System.getProperty("java.io.tmpdir"), "item-export-test.xml");
        xml.deleteOnExit();

        List<DSpaceCommandLineParameter> parameters = new LinkedList<>();

        parameters.add(new DSpaceCommandLineParameter("-n", xml.getAbsolutePath()));
        parameters.add(new DSpaceCommandLineParameter("-i", item.getID().toString()));
        parameters.add(new DSpaceCommandLineParameter("-f", "publication-cerif-xml"));

        List<ParameterValueRest> list = parameters.stream()
            .map(dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                .convert(dSpaceCommandLineParameter, Projection.DEFAULT))
            .collect(Collectors.toList());

        AtomicReference<Integer> idRef = new AtomicReference<>();

        try {

            getClient().perform(post("/api/system/scripts/item-export/processes")
                .contentType("multipart/form-data")
                .param("properties", new Gson().toJson(list)))
                .andExpect(status().isAccepted())
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.processId")));

            Process process = processService.find(context, idRef.get());
            assertNull(process.getEPerson());

        } finally {
            if (idRef.get() != null) {
                ProcessBuilder.deleteProcess(idRef.get());
            }
        }
    }

    @Test
    public void scriptTypeConversionTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/system/scripts/type-conversion-test"))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath(
                    "$", ScriptMatcher
                        .matchScript(
                            "type-conversion-test",
                            "Test the type conversion different option types"
                        )
                )
            )
            .andExpect(
                jsonPath(
                    "$.parameters", containsInAnyOrder(
                        allOf(
                            hasJsonPath("$.name", is("-b")),
                            hasJsonPath("$.description", is("option set to the boolean class")),
                            hasJsonPath("$.type", is("boolean")),
                            hasJsonPath("$.mandatory", is(false)),
                            hasJsonPath("$.nameLong", is("--boolean"))
                        ),
                        allOf(
                            hasJsonPath("$.name", is("-s")),
                            hasJsonPath("$.description", is("string option with an argument")),
                            hasJsonPath("$.type", is("String")),
                            hasJsonPath("$.mandatory", is(false)),
                            hasJsonPath("$.nameLong", is("--string"))
                        ),
                        allOf(
                            hasJsonPath("$.name", is("-n")),
                            hasJsonPath("$.description", is("string option without an argument")),
                            hasJsonPath("$.type", is("boolean")),
                            hasJsonPath("$.mandatory", is(false)),
                            hasJsonPath("$.nameLong", is("--noargument"))
                        ),
                        allOf(
                            hasJsonPath("$.name", is("-f")),
                            hasJsonPath("$.description", is("file option with an argument")),
                            hasJsonPath("$.type", is("InputStream")),
                            hasJsonPath("$.mandatory", is(false)),
                            hasJsonPath("$.nameLong", is("--file"))
                        )
                    )
                )
            );
    }

    //@Ignore
    @Test
    public void exportPubliclyAvailableItemsTest() throws Exception {
        String adminLimit = configurationService.getProperty("bulk-export.limit.admin");
        String notLoggedInLimit = configurationService.getProperty("bulk-export.limit.notLoggedIn");
        String loggedInLimit = configurationService.getProperty("bulk-export.limit.loggedIn");
        ReferCrosswalk publicationCerif = null;
        Boolean isPubliclyReadable = false;
        try {
            context.turnOffAuthorisationSystem();

            publicationCerif =
                (ReferCrosswalk) new DSpace()
                    .getSingletonService(StreamDisseminationCrosswalkMapper.class)
                    .getByType("publication-cerif-xml");
            isPubliclyReadable = publicationCerif.isPubliclyReadable();

            publicationCerif.setPubliclyReadable(true);

            configurationService.setProperty("bulk-export.limit.admin", "2");

            parentCommunity =
                CommunityBuilder.createCommunity(context)
                    .withName("Parent Community")
                    .build();

            Collection collection =
                CollectionBuilder.createCollection(context, parentCommunity)
                    .withName("Collection 1")
                    .build();

            Item firstPerson =
                createItem(context, collection)
                    .withEntityType("Person")
                    .withTitle("Smith, John")
                    .withVariantName("J.S.")
                    .withVariantName("Smith John")
                    .withGender("M")
                    .withPersonMainAffiliation("University")
                    .withOrcidIdentifier("0000-0002-9079-5932")
                    .withScopusAuthorIdentifier("SA-01")
                    .withPersonEmail("test@test.com")
                    .withResearcherIdentifier("R-01")
                    .withResearcherIdentifier("R-02")
                    .withPersonAffiliation("Company")
                    .withPersonAffiliationStartDate("2018-01-01")
                    .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
                    .withPersonAffiliationRole("Developer")
                    .withPersonAffiliation("Another Company")
                    .withPersonAffiliationStartDate("2017-01-01")
                    .withPersonAffiliationEndDate("2017-12-31")
                    .withPersonAffiliationRole("Developer")
                    .build();

            Item secondPerson =
                createItem(context, collection)
                    .withEntityType("Person")
                    .withTitle("White, Walter")
                    .withGender("M")
                    .withPersonMainAffiliation("University")
                    .withOrcidIdentifier("0000-0002-9079-5938")
                    .withPersonEmail("w.w@test.com")
                    .withResearcherIdentifier("R-03")
                    .withPersonAffiliation("Company")
                    .withPersonAffiliationStartDate("2018-01-01")
                    .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
                    .withPersonAffiliationRole("Developer")
                    .build();

            Item project =
                ItemBuilder.createItem(context, collection)
                    .withEntityType("Project")
                    .withTitle("Test Project")
                    .withInternalId("111-222-333")
                    .withAcronym("TP")
                    .withProjectStartDate("2020-01-01")
                    .withProjectEndDate("2020-04-01")
                    .build();

            ItemBuilder.createItem(context, collection)
                .withEntityType("Funding")
                .withTitle("Test Funding")
                .withType("Internal Funding")
                .withFunder("Test Funder")
                .withRelationProject("Test Project", project.getID().toString())
                .build();

            Item funding =
                ItemBuilder.createItem(context, collection)
                    .withEntityType("Funding")
                    .withTitle("Another Test Funding")
                    .withType("Contract")
                    .withFunder("Another Test Funder")
                    .withAcronym("ATF-01")
                    .build();

            ItemBuilder.createItem(context, collection)
                .withEntityType("Publication")
                .withTitle("First Publication")
                .withAlternativeTitle("Alternative publication title")
                .withRelationPublication("Published in publication")
                .withRelationDoi("doi:10.3972/test")
                .withDoiIdentifier("doi:111.111/publication")
                .withIsbnIdentifier("978-3-16-148410-0")
                .withIssnIdentifier("2049-3630")
                .withIsiIdentifier("111-222-333")
                .withScopusIdentifier("99999999")
                .withLanguage("en")
                .withPublisher("Publication publisher")
                .withVolume("V.01")
                .withIssue("Issue")
                .withSubject("test")
                .withSubject("export")
                .withIssueDate("2022-08-22")
                .withAuthor("John Smith", firstPerson.getID().toString())
                .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
                .withAuthor("Walter White")
                .withAuthorAffiliation("Company")
                .withEditor("Editor")
                .withEditorAffiliation("Editor Affiliation")
                .withRelationProject("Test Project", project.getID().toString())
                .withRelationFunding("Another Test Funding", funding.getID().toString())
                .withRelationConference("The best Conference")
                .withRelationProduct("DataSet")
                .build();

            ItemBuilder.createItem(context, collection)
                .withEntityType("Publication")
                .withTitle("Second Publication")
                .withAlternativeTitle("Alternative publication title")
                .withRelationPublication("Published in publication")
                .withRelationDoi("doi:10.3973/test")
                .withDoiIdentifier("doi:111.222/publication")
                .withIsbnIdentifier("978-3-16-148410-0")
                .withIssnIdentifier("2049-3630")
                .withIsiIdentifier("111-222-333")
                .withScopusIdentifier("99999999")
                .withLanguage("en")
                .withPublisher("Publication publisher")
                .withVolume("V.01")
                .withIssue("Issue")
                .withSubject("test")
                .withSubject("export")
                .withType("Controlled Vocabulary for Resource Type Genres::text::review")
                .withIssueDate("2022-08-22")
                .withAuthor("Jessie Pinkman", secondPerson.getID().toString())
                .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
                .withAuthor("Walter White")
                .withAuthorAffiliation("Company")
                .withEditor("Editor")
                .withEditorAffiliation("Editor Affiliation")
                .withRelationProject("Test Project", project.getID().toString())
                .withRelationFunding("Another Test Funding", funding.getID().toString())
                .withRelationConference("The best Conference")
                .withRelationProduct("DataSet")
                .build();

            Item restrictedItem =
                ItemBuilder.createItem(context, collection)
                    .withEntityType("Publication")
                    .withTitle("Third Publication")
                    .withSubject("export")
                    .withAuthor("EPerson", eperson.getID().toString())
                    .build();

            Item restrictedItem2 =
                ItemBuilder.createItem(context, collection)
                    .withEntityType("Publication")
                    .withTitle("Fourth Publication")
                    .withSubject("export")
                    .build();

            resourcePolicyService.removeAllPolicies(context, restrictedItem);
            resourcePolicyService.removeAllPolicies(context, restrictedItem2);

            LinkedList<DSpaceCommandLineParameter> parameters = new LinkedList<>();
            parameters.add(new DSpaceCommandLineParameter("-t", "Publication"));
            parameters.add(new DSpaceCommandLineParameter("-f", "publication-cerif-xml"));

            List<ParameterValueRest> list =
                parameters.stream()
                    .map(
                        dSpaceCommandLineParameter -> dSpaceRunnableParameterConverter
                            .convert(dSpaceCommandLineParameter, Projection.DEFAULT)
                    )
                    .collect(Collectors.toList());

            String adminToken = getAuthToken(admin.getEmail(), password);
            List<ProcessStatus> acceptableProcessStatuses = new LinkedList<>();
            acceptableProcessStatuses.addAll(
                Arrays.asList(
                    ProcessStatus.SCHEDULED,
                    ProcessStatus.RUNNING,
                    ProcessStatus.COMPLETED
                )
            );

            AtomicReference<Integer> idRef = new AtomicReference<>();

            context.restoreAuthSystemState();

            String[] includedContents =
                {
                        "First Publication",
                        "Second Publication"
                };
            String[] excludedContents =
                {
                        "Third Publication",
                        "Fourth Publication"
                };
            try {
                getClient(adminToken)
                    .perform(
                        multipart("/api/system/scripts/bulk-item-export/processes")
                            .param("properties", new Gson().toJson(list))
                    )
                    .andExpect(status().isAccepted())
                    .andExpect(
                        jsonPath(
                            "$", is(
                                ProcessMatcher.matchProcess(
                                    "bulk-item-export",
                                    String.valueOf(admin.getID()),
                                    parameters,
                                    acceptableProcessStatuses
                                )
                            )
                        )
                    )
                    .andDo(
                        result -> idRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId"))
                    );
                checkExportOutput(adminToken, null, idRef, includedContents, excludedContents, true);
            } finally {
                if (idRef.get() != null) {
                    ProcessBuilder.deleteProcess(idRef.get());
                }
            }
            configurationService.setProperty("bulk-export.limit.notLoggedIn", "0");
            // anonymous export
            getClient()
                .perform(
                    multipart("/api/system/scripts/bulk-item-export/processes")
                        .param("properties", new Gson().toJson(list))
                )
                // this is acceptable here because the process
                .andExpect(status().isUnauthorized());
            configurationService.setProperty("bulk-export.limit.loggedIn", "2");
            configurationService.setProperty("bulk-export.limit.notLoggedIn", "2");
            try {
                // eperson export
                String epToken = getAuthToken(eperson.getEmail(), password);
                getClient(epToken)
                    .perform(
                        multipart("/api/system/scripts/bulk-item-export/processes")
                            .param("properties", new Gson().toJson(list))
                    )
                    .andExpect(status().isAccepted())
                    .andExpect(
                        jsonPath(
                            "$", is(
                                ProcessMatcher.matchProcess(
                                    "bulk-item-export",
                                    String.valueOf(eperson.getID()),
                                    parameters,
                                    acceptableProcessStatuses
                                )
                            )
                        )
                    )
                    .andDo(
                        result -> idRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId"))
                    );
                checkExportOutput(epToken, null, idRef, includedContents, excludedContents, true);
            } finally {
                if (idRef.get() != null) {
                    ProcessBuilder.deleteProcess(idRef.get());
                }
            }

            // set the export results as not public, we should get reserved content in it
            ReferCrosswalk expCross =
                (ReferCrosswalk) new DSpace()
                    .getSingletonService(StreamDisseminationCrosswalkMapper.class)
                    .getByType("publication-cerif-xml");
            // allow anonymous users to run the export
            configurationService.setProperty("bulk-export.limit.admin", "10");
            expCross.setPubliclyReadable(false);
            includedContents =
                new String[] {
                        "First Publication",
                        "Second Publication",
                        "Third Publication",
                        "Fourth Publication"
                };
            excludedContents = new String[] {};
            try {
                getClient(adminToken)
                    .perform(
                        multipart("/api/system/scripts/bulk-item-export/processes")
                            .param("properties", new Gson().toJson(list))
                    )
                    .andExpect(status().isAccepted())
                    .andExpect(
                        jsonPath(
                            "$", is(
                                ProcessMatcher.matchProcess(
                                    "bulk-item-export",
                                    String.valueOf(admin.getID()),
                                    parameters,
                                    acceptableProcessStatuses
                                )
                            )
                        )
                    )
                    .andDo(
                        result -> idRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId"))
                    );
                checkExportOutput(adminToken, null, idRef, includedContents, excludedContents, false);
            } finally {
                if (idRef.get() != null) {
                    ProcessBuilder.deleteProcess(idRef.get());
                }
            }
            configurationService.setProperty("bulk-export.limit.notLoggedIn", "2");
            includedContents =
                new String[] {
                        "First Publication",
                        "Second Publication"
                };
            excludedContents =
                new String[] {
                        "Third Publication",
                        "Fourth Publication"
                };
            try {
                // anonymous export
                getClient()
                    .perform(
                        multipart("/api/system/scripts/bulk-item-export/processes")
                            .param("properties", new Gson().toJson(list))
                    )
                    .andExpect(status().isAccepted())
                    .andExpect(
                        jsonPath(
                            "$", is(
                                ProcessMatcher.matchProcess(
                                    "bulk-item-export",
                                    null,
                                    parameters,
                                    acceptableProcessStatuses
                                )
                            )
                        )
                    )
                    .andDo(
                        result -> idRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId"))
                    );
                checkExportOutput(null, null, idRef, includedContents, excludedContents, false);
            } finally {
                if (idRef.get() != null) {
                    ProcessBuilder.deleteProcess(idRef.get());
                }
            }
            // lower the allowed limit of item to export and check again
            configurationService.setProperty("bulk-export.limit.notLoggedIn", 1);
            includedContents =
                new String[] {
                        "First Publication"
                };
            excludedContents =
                new String[] {
                        "Second Publication",
                        "Third Publication",
                        "Fourth Publication"
                };
            try {
                // anonymous export
                getClient()
                    .perform(
                        multipart("/api/system/scripts/bulk-item-export/processes")
                            .param("properties", new Gson().toJson(list))
                    )
                    .andExpect(status().isAccepted())
                    .andExpect(
                        jsonPath(
                            "$", is(
                                ProcessMatcher.matchProcess(
                                    "bulk-item-export",
                                    null,
                                    parameters,
                                    acceptableProcessStatuses
                                )
                            )
                        )
                    )
                    .andDo(
                        result -> idRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId"))
                    );
                checkExportOutput(null, null, idRef, includedContents, excludedContents, false);
            } finally {
                if (idRef.get() != null) {
                    ProcessBuilder.deleteProcess(idRef.get());
                }
            }

            configurationService.setProperty("bulk-export.limit.loggedIn", "2");
            includedContents =
                new String[] {
                        "First Publication",
                        "Second Publication"
                };
            excludedContents =
                new String[] {
                        "Fourth Publication",
                        "Third Publication"
                };
            try {
                // eperson export
                String epToken = getAuthToken(eperson.getEmail(), password);
                getClient(epToken)
                    .perform(
                        multipart("/api/system/scripts/bulk-item-export/processes")
                            .param("properties", new Gson().toJson(list))
                    )
                    .andExpect(status().isAccepted())
                    .andExpect(
                        jsonPath(
                            "$", is(
                                ProcessMatcher.matchProcess(
                                    "bulk-item-export",
                                    String.valueOf(eperson.getID()),
                                    parameters,
                                    acceptableProcessStatuses
                                )
                            )
                        )
                    )
                    .andDo(
                        result -> idRef
                            .set(read(result.getResponse().getContentAsString(), "$.processId"))
                    );
                checkExportOutput(epToken, null, idRef, includedContents, excludedContents, false);
            } finally {
                if (idRef.get() != null) {
                    ProcessBuilder.deleteProcess(idRef.get());
                }
            }
        } finally {
            configurationService.setProperty("bulk-export.limit.admin", adminLimit);
            configurationService.setProperty("bulk-export.limit.notLoggedIn", notLoggedInLimit);
            configurationService.setProperty("bulk-export.limit.loggedIn", loggedInLimit);
            if (publicationCerif != null) {
                publicationCerif.setPubliclyReadable(isPubliclyReadable);
            }
        }

    }


    /**
     * schedules collection-export process through {@link org.dspace.app.rest.repository.ScriptRestRepository}
     * that uses an user that is configured as admin collection and then check for its valid status!
     */
    @Test
    public void collectionExportProcessExecutionWithCollectionAdmin() throws Exception {

        context.turnOffAuthorisationSystem();
        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("Parent Community")
                            .build();

        Collection collection =
            createCollection(context, parentCommunity)
                .withSubmissionDefinition("patent")
                .withAdminGroup(eperson)
                .build();

        Item item =
            ItemBuilder.createItem(context, collection)
                       .withTitle("Test patent")
                       .withAuthor("White, Walter")
                       .withIssueDate("2020-01-01")
                       .withLanguage("it")
                       .withSubject("test")
                       .withSubject("export")
                       .build();

        context.commit();
        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef = new AtomicReference<>();

        try {


            String epersonToken = getAuthToken(eperson.getEmail(), password);

            DSpaceCommandLineParameter collectionParam =
                new DSpaceCommandLineParameter("-c", collection.getID().toString());

            var parameters =
                Stream.of(collectionParam)
                      .map(lineParam -> dSpaceRunnableParameterConverter.convert(lineParam, Projection.DEFAULT))
                      .collect(Collectors.toList());

            getClient(epersonToken)
                .perform(
                    post("/api/system/scripts/collection-export/processes")
                        .contentType("multipart/form-data")
                        .param("properties", new Gson().toJson(parameters))
                )
                .andExpect(status().isAccepted())
                .andExpect(
                    jsonPath(
                        "$", is(
                            ProcessMatcher.matchProcess(
                                "collection-export",
                                String.valueOf(eperson.getID()),
                                List.of(collectionParam),
                                List.of(
                                    ProcessStatus.SCHEDULED,
                                    ProcessStatus.RUNNING,
                                    ProcessStatus.COMPLETED
                                )
                            )
                        )
                    )
                )
                .andDo(
                    result ->
                        idRef.set(read(result.getResponse().getContentAsString(), "$.processId"))
                );

            AtomicReference<String> resultBitstreamId = new AtomicReference<>();

            getClient(epersonToken)
                .perform(
                    get("/api/system/processes/" + idRef.get() + "/files")
                )
                .andExpect(status().isOk())
                .andExpect(
                    response ->
                        hasJsonPath("$._embedded.files[?(@.name=='items.xls')].id")
                )
                .andDo(
                    response ->
                        resultBitstreamId.set(
                            ((JSONArray) read(
                                response.getResponse().getContentAsString(),
                                "$._embedded.files[?(@.name=='items.xls')].id"
                            )).get(0).toString()
                        )
                );

            MvcResult processResult =
                getClient(epersonToken)
                    .perform(get("/api/core/bitstreams/" + resultBitstreamId.get() + "/content"))
                    .andExpect(status().isOk())
                    .andReturn();

            try (ByteArrayInputStream bis =
                     new ByteArrayInputStream(processResult.getResponse().getContentAsByteArray())) {

                Workbook workbook = WorkbookFactory.create(bis);
                assertThat(workbook.getNumberOfSheets(), equalTo(2));

                Sheet sheet = workbook.getSheetAt(0);
                assertThat(sheet.getPhysicalNumberOfRows(), equalTo(2));
                assertThat(
                    getRowValues(sheet.getRow(0), 19),
                    contains(
                        "ID", "DISCOVERABLE", "dc.title",
                        "dcterms.dateAccepted", "dc.date.issued", "dc.contributor.author", "dcterms.rightsHolder",
                        "dc.publisher", "dc.identifier.patentno", "dc.identifier.patentnumber", "dc.type",
                        "dc.identifier.applicationnumber", "dc.date.filled", "dc.language.iso",
                        "dc.subject", "dc.description.abstract", "dc.relation", "dc.relation.patent",
                        "dc.relation.references"
                    )
                );
                assertThat(
                    getRowValues(sheet.getRow(1), 19),
                    contains(
                        item.getID().toString(), "Y", "Test patent", "",
                        "2020-01-01", "White, Walter", "", "", "", "", "", "", "", "it", "test||export", "", "", "",
                        ""
                    )
                );

            }

        } finally {
            if (idRef.get() != null) {
                ProcessBuilder.deleteProcess(idRef.get());
            }
        }

    }


    /**
     * schedules collection-export process through {@link org.dspace.app.rest.repository.ScriptRestRepository}
     * with an admin user and checks its validity!
     */
    @Test
    public void collectionExportProcessExecutionWithAdmin() throws Exception {

        context.turnOffAuthorisationSystem();
        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("Parent Community")
                            .build();

        Collection collection =
            createCollection(context, parentCommunity)
                .withSubmissionDefinition("patent")
                .build();

        Item item =
            ItemBuilder.createItem(context, collection)
                       .withTitle("Test patent")
                       .withAuthor("White, Walter")
                       .withIssueDate("2020-01-01")
                       .withLanguage("it")
                       .withSubject("test")
                       .withSubject("export")
                       .build();

        context.commit();
        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef = new AtomicReference<>();

        try {


            String adminToken = getAuthToken(admin.getEmail(), password);

            DSpaceCommandLineParameter collectionParam =
                new DSpaceCommandLineParameter("-c", collection.getID().toString());

            var parameters =
                Stream.of(collectionParam)
                      .map(lineParam -> dSpaceRunnableParameterConverter.convert(lineParam, Projection.DEFAULT))
                      .collect(Collectors.toList());

            getClient(adminToken)
                .perform(
                    post("/api/system/scripts/collection-export/processes")
                        .contentType("multipart/form-data")
                        .param("properties", new Gson().toJson(parameters))
                )
                .andExpect(status().isAccepted())
                .andExpect(
                    jsonPath(
                        "$", is(
                            ProcessMatcher.matchProcess(
                                "collection-export",
                                String.valueOf(admin.getID()),
                                List.of(collectionParam),
                                List.of(
                                    ProcessStatus.SCHEDULED,
                                    ProcessStatus.RUNNING,
                                    ProcessStatus.COMPLETED
                                )
                            )
                        )
                    )
                )
                .andDo(
                    result ->
                        idRef.set(read(result.getResponse().getContentAsString(), "$.processId"))
                );

        } finally {
            if (idRef.get() != null) {
                ProcessBuilder.deleteProcess(idRef.get());
            }
        }

    }

    /**
     * Fails to schedule the collection-export process through {@link org.dspace.app.rest.repository.ScriptRestRepository}
     * whenever launched with a non collectionAdmin user.!
     */
    @Test
    public void collectionExportProcessExecutionFailsWithNonAdmin() throws Exception {

        context.turnOffAuthorisationSystem();
        parentCommunity =
            CommunityBuilder.createCommunity(context)
                            .withName("Parent Community")
                            .build();

        Collection collection =
            createCollection(context, parentCommunity)
                .withSubmissionDefinition("patent")
                .build();

        context.commit();
        context.restoreAuthSystemState();

        String epersonToken = getAuthToken(eperson.getEmail(), password);

        DSpaceCommandLineParameter collectionParam =
            new DSpaceCommandLineParameter("-c", collection.getID().toString());

        var parameters =
            Stream.of(collectionParam)
                  .map(lineParam -> dSpaceRunnableParameterConverter.convert(lineParam, Projection.DEFAULT))
                  .collect(Collectors.toList());

        getClient(epersonToken)
            .perform(
                post("/api/system/scripts/collection-export/processes")
                    .contentType("multipart/form-data")
                    .param("properties", new Gson().toJson(parameters))
            )
            .andExpect(status().isForbidden());

    }

    private void checkExportOutput(
        String processToken,
        String fileToken,
        AtomicReference<Integer> idRef,
        String[] includedContents,
        String[] excludedContents,
        boolean publicFile
    ) throws Exception {
        String contentAsString = null;
        MvcResult mvcResult = null;
        // wait and retry up to 3 sec to get the process completed
        for (int i = 0; i < 6; i++) {
            Thread.sleep(500);
            mvcResult =
                getClient(processToken)
                    .perform(get("/api/system/processes/" + idRef.get() + "/files"))
                    .andReturn();
            contentAsString = mvcResult.getResponse().getContentAsString();
            if (StringUtils.isNotBlank(contentAsString)) {
                break;
            }
        }
        JSONArray publicationsId =
            read(
                contentAsString,
                "$._embedded.files[?(@.name=='publication.xml')].id"
            );

        assertNotNull("The publication.xml file must be present", publicationsId);
        String publicationJsonId = publicationsId.get(0).toString();
        getClient(processToken)
            .perform(get("/api/core/bitstreams/" + publicationJsonId))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath(
                    "$",
                    allOf(
                        hasJsonPath("name", is("publication.xml")),
                        hasJsonPath("id", is(publicationJsonId))
                    )
                )
            );

        ResultMatcher anonymousDownload =
            publicFile || processToken == null ? status().isOk() : status().isUnauthorized();
        getClient(fileToken)
            .perform(get("/api/core/bitstreams/" + publicationJsonId + "/content"))
            .andExpect(anonymousDownload);
        mvcResult =
            getClient(processToken)
                .perform(get("/api/core/bitstreams/" + publicationJsonId + "/content"))
                .andExpect(status().isOk())
                .andReturn();

        String exportContent = mvcResult.getResponse().getContentAsString();
        for (String includedContent : includedContents) {
            assertThat(
                "The following content must be present " + includedContent,
                exportContent.contains(includedContent)
            );
        }
        for (String excludedContent : excludedContents) {
            assertThat(
                "The following content must be NOT present " + excludedContent,
                !exportContent.contains(excludedContent)
            );
        }
    }


    @Override
    @After
    public void destroy() throws Exception {
        context.turnOffAuthorisationSystem();
        CollectionUtils.emptyIfNull(processService.findAll(context)).stream().forEach(process -> {
            try {
                processService.delete(context, process);
            } catch (SQLException | AuthorizeException | IOException e) {
                throw new RuntimeException(e);
            }
        });
        context.restoreAuthSystemState();
        super.destroy();
    }

}
