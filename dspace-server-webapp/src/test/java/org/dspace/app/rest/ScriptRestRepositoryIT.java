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
import static org.dspace.builder.ItemBuilder.createItem;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import net.minidev.json.JSONArray;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.converter.DSpaceRunnableParameterConverter;
import org.dspace.app.rest.matcher.BitstreamMatcher;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.matcher.ProcessMatcher;
import org.dspace.app.rest.matcher.ScriptMatcher;
import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
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
import org.junit.Assume;
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
    public void after() {
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
    public void findAllScriptsWithNoAdminTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/system/scripts"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page",
                                            is(PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 4))));

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
                        .andExpect(jsonPath("$._embedded.scripts", hasItem(
                ScriptMatcher.matchScript(scriptConfigurations.get(10).getName(),
                    scriptConfigurations.get(10).getDescription())
                        )))
                        .andExpect(jsonPath("$._embedded.scripts", Matchers.not(hasItem(
                                ScriptMatcher.matchScript(alphabeticScripts.get(0).getName(),
                                                          alphabeticScripts.get(0).getDescription())
                        ))))
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
    public void findOneScriptByNameTestAccessDenied() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/system/scripts/mock-script"))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void findOneScriptByInvalidNameBadRequestExceptionTest() throws Exception {
        getClient().perform(get("/api/system/scripts/mock-script-invalid"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void postProcessNonAdminAuthorizeException() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(multipart("/api/system/scripts/mock-script/processes"))
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

//        List<ParameterValueRest> list = new LinkedList<>();
//
//        ParameterValueRest parameterValueRest = new ParameterValueRest();
//        parameterValueRest.setName("-z");
//        parameterValueRest.setValue("test");
//        ParameterValueRest parameterValueRest1 = new ParameterValueRest();
//        parameterValueRest1.setName("-q");
//        list.add(parameterValueRest);
//        list.add(parameterValueRest1);

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
                        .andExpect(status().isBadRequest());
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
        getClient(token).perform(post("/api/system/scripts/mock-script-invalid/processes"))
                        .andExpect(status().isBadRequest());
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
        // skip test based on configuration
        Assume.assumeFalse(configurationService.getBooleanProperty("test.skip.cris", false));

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

    private void checkExportOutput(
        String processToken,
        String fileToken,
        AtomicReference<Integer> idRef,
        String[] includedContents,
        String[] excludedContents,
        boolean publicFile
    ) throws Exception, SQLException, UnsupportedEncodingException {
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
        CollectionUtils.emptyIfNull(processService.findAll(context)).stream().forEach(process -> {
            try {
                processService.delete(context, process);
            } catch (SQLException | AuthorizeException | IOException e) {
                throw new RuntimeException(e);
            }
        });
        super.destroy();
    }

}
