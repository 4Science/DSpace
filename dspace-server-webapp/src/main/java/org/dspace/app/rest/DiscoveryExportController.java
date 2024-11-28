/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest;

import static org.apache.commons.lang.StringUtils.defaultIfBlank;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.app.rest.scripts.handler.impl.RestDSpaceRunnableHandler;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.HttpHeadersInitializer;
import org.dspace.content.Bitstream;
import org.dspace.content.ProcessStatus;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.Process;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.service.ScriptService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller perform a query as if it were performed on DSpace search page, accepting same parameters, and returns
 * search results (only publications in this implementation) in their marc xml representation.
 */
@RestController
@RequestMapping("/api/" + SearchResultsRest.CATEGORY)
public class DiscoveryExportController {

    private static final int BUFFER_SIZE = 4096 * 10;
    @Autowired
    private ScriptService scriptService;

    @Autowired
    private BitstreamService bitstreamService;
    @Autowired
    private ConfigurationService configurationService;

    @GetMapping(produces = "application/xml", path = "/export")
    public ResponseEntity export(HttpServletRequest request, HttpServletResponse response,
                                 @RequestParam(value = "query", required = false) String query,
                                 @RequestParam(value = "scope", required = false) String scope,
                                 @RequestParam(value = "spc.sf", required = false) String sort,
                                 @RequestParam(value = "spc.sd", required = false) String sortDirection,
                                 @RequestParam(value = "configuration", required = false) String configuration,
                                 @RequestParam(value = "spc.page", required = false) String spcPage,
                                 @RequestParam(value = "spc.rpp", required = false) String resultsPerPage,
                                 List<SearchFilter> searchFilters,
                                 Pageable page) {

        // FIXME: try to reuse as much parameter as possible as in original discovery request, all parameter set,
        //  mapping search page frontend request could be handled in a different way.

        ScriptConfiguration scriptToExecute = scriptService.getScriptConfiguration("bulk-item-export");
        Context context = ContextUtil.obtainContext(request);
        EPerson user = context.getCurrentUser();

        String sorting = defaultIfBlank(sort, "dc.title") + "," +
            defaultIfBlank(sortDirection, "ASC");

        Integer pageNumber = Objects.nonNull(spcPage) ? Integer.parseInt(spcPage) :
            page.getPageNumber();

        resultsPerPage = StringUtils.defaultIfBlank(resultsPerPage, "10");

        int limit = Integer.parseInt(resultsPerPage);

        List<DSpaceCommandLineParameter> dSpaceCommandLineParameters = parameters(
            defaultIfBlank(query, "*"),
            defaultIfBlank(configuration, "default"),
            sorting,
            scope,
            buildFilters(searchFilters),
            (Math.max(0,pageNumber - 1)) * limit,
            limit);

        try {
            RestDSpaceRunnableHandler restDSpaceRunnableHandler = new RestDSpaceRunnableHandler(
                user,
                scriptToExecute.getName(),
                dSpaceCommandLineParameters,
                context.getSpecialGroups(),
                context.getCurrentLocale()
            );
            List<String> args = constructArgs(dSpaceCommandLineParameters);
            Process process = runProcess(scriptToExecute, context, user, restDSpaceRunnableHandler, args);
            if (ProcessStatus.FAILED.equals(process.getProcessStatus())) {
                throw new RuntimeException("An error occurred during export");
            }
            Bitstream bitstream = responseFromBitstreams(context, process.getBitstreams());
            if (Objects.isNull(bitstream)) {
                throw new RuntimeException("Process did not produce any output");
            }
            return toResponseEntity(context, bitstream, request, response);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private List<DSpaceCommandLineParameter> parameters(String query, String configuration,
                                                        String sorting, String scope, String filters,
                                                        Integer offset, Integer limit) {
        List<DSpaceCommandLineParameter> result = new LinkedList<>();
        result.add(new DSpaceCommandLineParameter("-t", "Publication"));
        result.add(new DSpaceCommandLineParameter("-f", "epfl-publication-marc-xml"));
        result.add(new DSpaceCommandLineParameter("-q", query));
        result.add(new DSpaceCommandLineParameter("-c", configuration));
        result.add(new DSpaceCommandLineParameter("-so", sorting));
        result.add(new DSpaceCommandLineParameter("-o", String.valueOf(offset)));

        if (StringUtils.isNotBlank(scope)) {
            result.add(new DSpaceCommandLineParameter("-s", scope));
        }

        if (StringUtils.isNotBlank(filters)) {
            result.add(new DSpaceCommandLineParameter("-sf", filters));
        }

        if (limit > 0) {
            result.add(new DSpaceCommandLineParameter("-l", String.valueOf(limit)));
        }

        return result;
    }

    private String buildFilters(List<SearchFilter> searchFilters) {
        return searchFilters.stream()
            .map(sf -> sf.getName() + "=" + sf.getValue() + "," + sf.getOperator())
            .collect(Collectors.joining("&"));
    }

    private Bitstream responseFromBitstreams(Context context, List<Bitstream> bitstreams)
        throws SQLException {

        Bitstream bitstream = findExport(context, bitstreams);
        if (Objects.isNull(bitstream)) {
            throw new RuntimeException("Export did not produce any output");
        }
        return bitstream;
    }

    private Bitstream findExport(Context context, List<Bitstream> bitstreams) throws SQLException {
        for (Bitstream bitstream : bitstreams) {
            if (MediaType.TEXT_XML.equals(bitstream.getFormat(context).getMIMEType())) {
                return bitstream;
            }
        }
        return null;
    }

    private ResponseEntity toResponseEntity(Context context, Bitstream bitstream, HttpServletRequest request,
                                            HttpServletResponse response) throws SQLException, IOException {

        //FIXME: part of this logic is similar to one in org.dspace.app.rest.BitstreamRestController, as further step it
        // might be centralized and refactored.

        HttpHeadersInitializer httpHeadersInitializer = new HttpHeadersInitializer()
            .withBufferSize(BUFFER_SIZE)
            .withFileName(bitstream.getName())
            .withChecksum(bitstream.getChecksum())
            .withLength(bitstream.getSizeBytes())
            .withMimetype(bitstream.getFormat(context).getMIMEType())
            .with(request)
            .with(response);

        Long lastModified = bitstreamService.getLastModified(bitstream);
        if (lastModified != null) {
            httpHeadersInitializer.withLastModified(lastModified);
        }

        EPerson currentUser = context.getCurrentUser();
        org.dspace.app.rest.utils.BitstreamResource bitstreamResource =
            new org.dspace.app.rest.utils.BitstreamResource(
            bitstream.getName(), bitstream.getID(), currentUser != null ? currentUser.getID() : null,
            context.getSpecialGroupUuids(), false, false);

        context.complete();

        //Send the data
        if (httpHeadersInitializer.isValid()) {
            HttpHeaders httpHeaders = httpHeadersInitializer.initialiseHeaders();
            return ResponseEntity.ok().headers(httpHeaders).body(bitstreamResource);
        }
        throw new RuntimeException("Invalid headers for response");
    }

    private Process runProcess(ScriptConfiguration scriptToExecute, Context context, EPerson user,
                               RestDSpaceRunnableHandler restDSpaceRunnableHandler, List<String> args)
        throws InterruptedException, InstantiationException, IllegalAccessException {
        runDSpaceScript(user, scriptToExecute, restDSpaceRunnableHandler, args);
        Process process = restDSpaceRunnableHandler.getProcess(context);
        int attempts = 1;
        while (notFinished(process) && attempts++ <= 50) {
            Thread.sleep(1000L);
            process = restDSpaceRunnableHandler.getProcess(context);
        }

        return process;
    }

    private static boolean notFinished(Process process) {
        return Stream.of(ProcessStatus.RUNNING, ProcessStatus.SCHEDULED)
            .anyMatch(p -> p.equals(process.getProcessStatus()));
    }

    private void runDSpaceScript(EPerson user,
                                 ScriptConfiguration scriptToExecute,
                                 RestDSpaceRunnableHandler restDSpaceRunnableHandler,
                                 List<String> args)
        throws InstantiationException, IllegalAccessException {
        DSpaceRunnable dSpaceRunnable = scriptService.createDSpaceRunnableForScriptConfiguration(scriptToExecute);
        try {
            dSpaceRunnable.initialize(args.toArray(new String[0]), restDSpaceRunnableHandler, user);
//            restDSpaceRunnableHandler.schedule(dSpaceRunnable);
            dSpaceRunnable.run();
        } catch (ParseException e) {
            dSpaceRunnable.printHelp();
            try {
                restDSpaceRunnableHandler.handleException(
                    "Failed to parse the arguments given to the script with name: "
                        + scriptToExecute.getName() + " and args: " + args, e
                );
            } catch (Exception re) {
                // ignore re-thrown exception
            }
        }
    }

    private List<String> constructArgs(List<DSpaceCommandLineParameter> dSpaceCommandLineParameters) {
        List<String> args = new ArrayList<>();
        for (DSpaceCommandLineParameter parameter : dSpaceCommandLineParameters) {
            args.add(parameter.getName());
            if (parameter.getValue() != null) {
                args.add(parameter.getValue());
            }
        }
        return args;
    }



}
