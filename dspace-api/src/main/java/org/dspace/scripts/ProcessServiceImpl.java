/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.ProcessStatus;
import org.dspace.content.dao.ProcessDAO;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.scripts.service.ProcessService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The implementation for the {@link ProcessService} class
 */
public class ProcessServiceImpl implements ProcessService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ProcessService.class);

    @Autowired
    private ProcessDAO processDAO;

    @Autowired
    private GroupService groupService;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private BitstreamFormatService bitstreamFormatService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public Process create(Context context, EPerson ePerson, String scriptName,
                          List<DSpaceCommandLineParameter> parameters,
                          final Set<Group> specialGroups) throws SQLException {

        Process process = new Process();
        process.setEPerson(ePerson);
        process.setName(scriptName);
        process.setParameters(DSpaceCommandLineParameter.concatenate(parameters));
        process.setCreationTime(new Date());
        Optional.ofNullable(specialGroups)
            .ifPresent(sg -> {
                // we use a set to be sure no duplicated special groups are stored with process
                Set<Group> specialGroupsSet = new HashSet<>(sg);
                process.setGroups(new ArrayList<>(specialGroupsSet));
            });

        Process createdProcess = processDAO.create(context, process);
        String message = Objects.nonNull(ePerson) ? "eperson with email " + ePerson.getEmail() : "an Anonymous user";
        log.info(LogHelper.getHeader(context, "process_create",
                                      "Process has been created for " + message
                                          + " with process ID " + createdProcess.getID() + " and scriptName " +
                                          scriptName + " and parameters " + parameters));
        return createdProcess;
    }

    @Override
    public Process find(Context context, int processId) throws SQLException {
        return processDAO.findByID(context, Process.class, processId);
    }

    @Override
    public List<Process> findAll(Context context) throws SQLException {
        return processDAO.findAll(context, Process.class);
    }

    @Override
    public List<Process> findAll(Context context, int limit, int offset) throws SQLException {
        return processDAO.findAll(context, limit, offset);
    }

    @Override
    public List<Process> findAllSortByScript(Context context) throws SQLException {
        return processDAO.findAllSortByScript(context);
    }

    @Override
    public List<Process> findAllSortByStartTime(Context context) throws SQLException {
        List<Process> processes = findAll(context);
        Comparator<Process> comparing = Comparator
            .comparing(Process::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()));
        comparing = comparing.thenComparing(Process::getID);
        processes.sort(comparing);
        return processes;
    }

    @Override
    public List<Process> findByUser(Context context, EPerson eperson, int limit, int offset) throws SQLException {
        return processDAO.findByUser(context, eperson, limit, offset);
    }

    @Override
    public void start(Context context, Process process) throws SQLException {
        process.setProcessStatus(ProcessStatus.RUNNING);
        process.setStartTime(new Date());
        update(context, process);
        log.info(LogHelper.getHeader(context, "process_start", "Process with ID " + process.getID()
            + " and name " + process.getName() + " has started"));

    }

    @Override
    public void fail(Context context, Process process) throws SQLException {
        process.setProcessStatus(ProcessStatus.FAILED);
        process.setFinishedTime(new Date());
        update(context, process);
        log.info(LogHelper.getHeader(context, "process_fail", "Process with ID " + process.getID()
            + " and name " + process.getName() + " has failed"));

    }

    @Override
    public void complete(Context context, Process process) throws SQLException {
        process.setProcessStatus(ProcessStatus.COMPLETED);
        process.setFinishedTime(new Date());
        update(context, process);
        log.info(LogHelper.getHeader(context, "process_complete", "Process with ID " + process.getID()
            + " and name " + process.getName() + " has been completed"));

    }

    @Override
    public void appendFile(Context context, Process process, InputStream is, String type, String fileName)
        throws IOException, SQLException, AuthorizeException {
        Bitstream bitstream = createFileBitstream(context, process, is, type, fileName);
        Map<Integer, Group> groupPolicy = null;
        Map<Integer, EPerson> userPolicy = null;
        if (Objects.isNull(context.getCurrentUser())) {
            Group anonymous = groupService.findByName(context, Group.ANONYMOUS);
            groupPolicy = new HashMap<>();
            groupPolicy.put(Constants.READ, anonymous);
        } else {
            userPolicy = new HashMap<>();
            userPolicy.put(Constants.READ, context.getCurrentUser());
            userPolicy.put(Constants.WRITE, context.getCurrentUser());
            userPolicy.put(Constants.DELETE, context.getCurrentUser());
        }
        this.addBitstream(context, process, bitstream, type, groupPolicy, userPolicy);
    }

    private Bitstream createFileBitstream(Context context, Process process, InputStream is, String type,
            String fileName)
            throws IOException, SQLException {
        Bitstream bitstream = bitstreamService.create(context, is);
        if (getBitstream(context, process, type) != null) {
            throw new IllegalArgumentException("Cannot create another file of type: " + type + " for this process" +
                                                   " with id: " + process.getID());
        }
        bitstream.setName(context, fileName);
        bitstreamService.setFormat(context, bitstream, bitstreamFormatService.guessFormat(context, bitstream));
        MetadataField dspaceProcessFileTypeField = metadataFieldService
            .findByString(context, Process.BITSTREAM_TYPE_METADATAFIELD, '.');
        bitstreamService.addMetadata(context, bitstream, dspaceProcessFileTypeField, null, type);
        return bitstream;
    }

    @Override
    public void appendFile(Context context, Process process, InputStream is, String type, String fileName,
            Map<Integer, Group> groupPolicy, Map<Integer, EPerson> userPolicy)
            throws IOException, SQLException, AuthorizeException {
        this.addBitstream(context, process, createFileBitstream(context, process, is, type, fileName), type,
                groupPolicy, userPolicy);
    }

    private void addBitstream(Context context, Process process, Bitstream bitstream, String type,
            Map<Integer, Group> groupPolicy, Map<Integer, EPerson> userPolicy)
            throws IOException, SQLException, AuthorizeException {

        if (Objects.nonNull(groupPolicy)) {
            for (Map.Entry<Integer, Group> entry : groupPolicy.entrySet()) {
                this.authorizeService.addPolicy(context, bitstream, entry.getKey(), entry.getValue());
            }
        }

        if (Objects.nonNull(userPolicy)) {
            for (Map.Entry<Integer, EPerson> entry : userPolicy.entrySet()) {
                this.authorizeService.addPolicy(context, bitstream, entry.getKey(), entry.getValue());
            }
        }

        try {
            context.turnOffAuthorisationSystem();
            bitstreamService.update(context, bitstream);
            context.restoreAuthSystemState();
        } catch (SQLException | AuthorizeException e) {
            log.info(e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }
        process.addBitstream(bitstream);
        update(context, process);
    }

    @Override
    public void delete(Context context, Process process) throws SQLException, IOException, AuthorizeException {

        for (Bitstream bitstream : ListUtils.emptyIfNull(process.getBitstreams())) {
            bitstreamService.delete(context, bitstream);
        }
        processDAO.delete(context, process);
        log.info(LogHelper.getHeader(context, "process_delete", "Process with ID " + process.getID()
            + " and name " + process.getName() + " has been deleted"));
    }

    @Override
    public void update(Context context, Process process) throws SQLException {
        processDAO.save(context, process);
    }

    @Override
    public List<DSpaceCommandLineParameter> getParameters(Process process) {
        if (StringUtils.isBlank(process.getParameters())) {
            return Collections.emptyList();
        }

        String[] parameterArray = process.getParameters().split(Pattern.quote(DSpaceCommandLineParameter.SEPARATOR));
        List<DSpaceCommandLineParameter> parameterList = new ArrayList<>();

        for (String parameter : parameterArray) {
            parameterList.add(new DSpaceCommandLineParameter(parameter));
        }

        return parameterList;
    }

    @Override
    public Bitstream getBitstreamByName(Context context, Process process, String bitstreamName) {
        for (Bitstream bitstream : getBitstreams(context, process)) {
            if (StringUtils.equals(bitstream.getName(), bitstreamName)) {
                return bitstream;
            }
        }

        return null;
    }

    @Override
    public Bitstream getBitstream(Context context, Process process, String type) {
        List<Bitstream> allBitstreams = process.getBitstreams();

        if (type == null) {
            return null;
        } else {
            if (allBitstreams != null) {
                for (Bitstream bitstream : allBitstreams) {
                    if (StringUtils.equals(bitstreamService.getMetadata(bitstream,
                                                                        Process.BITSTREAM_TYPE_METADATAFIELD), type)) {
                        return bitstream;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public List<Bitstream> getBitstreams(Context context, Process process) {
        return process.getBitstreams();
    }

    public int countTotal(Context context) throws SQLException {
        return processDAO.countRows(context);
    }

    @Override
    public List<String> getFileTypesForProcessBitstreams(Context context, Process process) {
        List<Bitstream> list = getBitstreams(context, process);
        Set<String> fileTypesSet = new HashSet<>();
        for (Bitstream bitstream : list) {
            List<MetadataValue> metadata = bitstreamService.getMetadata(bitstream,
                                                                        Process.BITSTREAM_TYPE_METADATAFIELD, Item.ANY);
            if (metadata != null && !metadata.isEmpty()) {
                fileTypesSet.add(metadata.get(0).getValue());
            }
        }
        return new ArrayList<>(fileTypesSet);
    }

    @Override
    public List<Process> search(Context context, ProcessQueryParameterContainer processQueryParameterContainer,
                                int limit, int offset) throws SQLException {
        return processDAO.search(context, processQueryParameterContainer, limit, offset);
    }

    @Override
    public int countSearch(Context context, ProcessQueryParameterContainer processQueryParameterContainer)
        throws SQLException {
        return processDAO.countTotalWithParameters(context, processQueryParameterContainer);
    }

    @Override
    public int countByUser(Context context, EPerson user) throws SQLException {
        return processDAO.countByUser(context, user);
    }

    @Override
    public void appendLog(int processId, String scriptName, String output, ProcessLogLevel processLogLevel)
            throws IOException {
        File logsDir = getLogsDirectory();
        File tempFile = new File(logsDir, processId + "-" + scriptName + ".log");
        FileWriter out = new FileWriter(tempFile, true);
        try {
            try (BufferedWriter writer = new BufferedWriter(out)) {
                writer.append(formatLogLine(processId, scriptName, output, processLogLevel));
                writer.newLine();
            }
        } finally {
            out.close();
        }
    }

    @Override
    public void createLogBitstream(Context context, Process process)
            throws IOException, SQLException, AuthorizeException {
        File logsDir = getLogsDirectory();
        File tempFile = new File(logsDir, process.getID() + "-" + process.getName() + ".log");
        if (tempFile.exists()) {
            FileInputStream inputStream = FileUtils.openInputStream(tempFile);
            appendFile(context, process, inputStream, Process.OUTPUT_TYPE,
                       process.getID() + "-" + process.getName() + ".log");
            inputStream.close();
            tempFile.delete();
        }
    }

    @Override
    public List<Process> findByStatusAndCreationTimeOlderThan(Context context, List<ProcessStatus> statuses,
        Date date) throws SQLException {
        return this.processDAO.findByStatusAndCreationTimeOlderThan(context, statuses, date);
    }

    @Override
    public void failRunningProcesses(Context context) throws SQLException, IOException, AuthorizeException {
        List<Process> processesToBeFailed = findByStatusAndCreationTimeOlderThan(
                context, List.of(ProcessStatus.RUNNING, ProcessStatus.SCHEDULED), new Date());
        for (Process process : processesToBeFailed) {
            if (isOrchestratorProcess(process)) {
                continue;
            }
            context.setCurrentUser(process.getEPerson());
            // Fail the process.
            log.info("Process with ID {} did not complete before tomcat shutdown, failing it now.", process.getID());
            fail(context, process);
            // But still attach its log to the process.
            appendLog(process.getID(), process.getName(), "Process did not complete before tomcat shutdown.",
                      ProcessLogLevel.ERROR);
            createLogBitstream(context, process);
        }
    }

    private boolean isOrchestratorProcess(Process process) {
        String taskExecutorBeanName = configurationService.getProperty("dspace.task.executor");
        if (!StringUtils.equals(taskExecutorBeanName, "orchestratorTaskExecutor")) {
            return false;
        }

        if (process == null || StringUtils.isBlank(process.getName())) {
            return false;
        }

        List<String> processIgnoreByOrchestrator =
            Arrays.asList(configurationService.getArrayProperty("orchestrator.ignore-script"));
        return !processIgnoreByOrchestrator.contains(process.getName());
    }

    private String formatLogLine(int processId, String scriptName, String output, ProcessLogLevel processLogLevel) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        StringBuilder sb = new StringBuilder();
        sb.append(sdf.format(new Date()));
        sb.append(" ");
        sb.append(processLogLevel);
        sb.append(" ");
        sb.append(scriptName);
        sb.append(" - ");
        sb.append(processId);
        sb.append(" @ ");
        sb.append(output);
        return  sb.toString();
    }

    private File getLogsDirectory() {
        String pathStr = configurationService.getProperty("dspace.dir")
            + File.separator + "log" + File.separator + "processes";
        File logsDir = new File(pathStr);
        if (!logsDir.exists()) {
            if (!logsDir.mkdirs()) {
                throw new RuntimeException("Couldn't create [dspace.dir]/log/processes/ directory.");
            }
        }
        return logsDir;
    }
}
