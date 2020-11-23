package org.dspace.app.cris.integration.orcid;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.ws.rs.NotFoundException;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;
import org.apache.log4j.Logger;
import org.dspace.app.itemimport.BTEBatchImportService;
import org.dspace.authority.orcid.OrcidService;
import org.dspace.core.Context;
import org.dspace.submit.lookup.NetworkSubmissionLookupDataLoader;
import org.dspace.submit.util.SubmissionLookupPublication;
import org.dspace.utils.DSpace;
import org.orcid.jaxb.model.common_v3.CreditName;
import org.orcid.jaxb.model.common_v3.ExternalId;
import org.orcid.jaxb.model.common_v3.ExternalIds;
import org.orcid.jaxb.model.common_v3.FuzzyDate;
import org.orcid.jaxb.model.common_v3.OrcidId;
import org.orcid.jaxb.model.common_v3.SourceType;
import org.orcid.jaxb.model.common_v3.Url;
import org.orcid.jaxb.model.record_v3.Citation;
import org.orcid.jaxb.model.record_v3.Contributor;
import org.orcid.jaxb.model.record_v3.NameType;
import org.orcid.jaxb.model.record_v3.NameType.GivenNames;
import org.orcid.jaxb.model.record_v3.PersonalDetails;
import org.orcid.jaxb.model.record_v3.Work;
import org.orcid.jaxb.model.record_v3.WorkBulk;
import org.orcid.jaxb.model.record_v3.WorkContributors;
import org.orcid.jaxb.model.record_v3.WorkGroup;
import org.orcid.jaxb.model.record_v3.WorkSummary;
import org.orcid.jaxb.model.record_v3.WorkTitle;
import org.orcid.jaxb.model.record_v3.Works;

import com.google.api.client.util.Charsets;
import com.google.common.io.Files;

import gr.ekt.bte.core.DataLoader;
import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.RecordSet;
import gr.ekt.bte.core.StringValue;
import gr.ekt.bte.core.Value;
import gr.ekt.bte.dataloader.FileDataLoader;

public class OrcidOnlineDataLoader extends NetworkSubmissionLookupDataLoader
{
    private static final int MAX_BULK_WORK = 100;

    private static final Logger log = Logger.getLogger(OrcidOnlineDataLoader.class);

    public final static String PLACEHOLER_NO_DATA = "#NODATA#";

    private int cooldown = 0;
    private int numberOfThread = 1;
    
    @Override
    public List<String> getSupportedIdentifiers()
    {
        return Arrays.asList(new String[] { ORCID });
    }

    @Override
    public boolean isSearchProvider()
    {
        return false;
    }

    @Override
    public List<Record> search(Context context, String title, String author,
            int year) throws HttpException, IOException
    {
        // TODO not supported yet
        return null;
    }

    @Override
    public List<Record> getByIdentifier(Context context,
            Map<String, Set<String>> keys) throws HttpException, IOException
    {
        Set<String> orcids = keys != null ? keys.get(ORCID) : null;
        List<DTOBulkPutCode> bulkCallList = new ArrayList<DTOBulkPutCode>();
        List<Thread> threads = new ArrayList<Thread>();
        final ConcurrentLinkedQueue<Record> q = new ConcurrentLinkedQueue<Record>();
        List<Record> results = new ArrayList<Record>();

        final OrcidService orcidService = OrcidService.getOrcid();
        String sourceName = orcidService.getSourceClientName();

        if (orcids != null)
        {
            for (final String orcid : orcids)
            {
                try
                {
                    final PersonalDetails profile = orcidService
                            .getPersonalDetails(orcid, null);
                    if (profile != null)
                    {
                        List<String> putCodes = new ArrayList<>();
                        Works orcidWorks = orcidService.getWorks(orcid, null);
                        workgroup: for (WorkGroup orcidGroup : orcidWorks.getGroup())
                        {
                            
                            List<WorkSummary> workSummaries = orcidGroup.getWorkSummary();
                            if(workSummaries!=null) { 
                                int higher = orcidService.higherDisplayIndex(orcidGroup);
                                // take the Work with highest display index value (the preferred item)
                                worksummary : for (final WorkSummary orcidSummary : workSummaries)
                                {
                                    if (StringUtils.isNotBlank(orcidSummary.getDisplayIndex()))
                                    {
                                        int current = Integer.parseInt(orcidSummary.getDisplayIndex());
                                        if (current < higher)
                                        {
                                            continue worksummary;
                                        }
                                    }
                                    putCodes.add(orcidSummary.getPutCode().toString());
                                    SourceType source = orcidSummary.getSource();
                                    String sourceNameWork = "";
                                    if (source != null)
                                    {
                                        sourceNameWork = source.getSourceName()
                                                .getContent();
                                    }
                                    if (StringUtils.isBlank(sourceNameWork)
                                            || !StringUtils.equals(sourceNameWork,
                                                    sourceName))
                                    {
                                        
                                        if (putCodes.size() == MAX_BULK_WORK) {
                                            DTOBulkPutCode bulkObject = new DTOBulkPutCode(orcid, putCodes, profile);
                                            bulkCallList.add(bulkObject);
                                            putCodes.clear();
                                        }
                                    }
                                }
                            }
                        }
                        if (putCodes.size() > 0) {
                            DTOBulkPutCode bulkObject = new DTOBulkPutCode(orcid, putCodes, profile);
                            bulkCallList.add(bulkObject);
                            putCodes.clear();
                        }
                    }
                }
                catch (NotFoundException ex)
                {
                    return results;
                }
            }
        }

        
        if(bulkCallList!=null && !bulkCallList.isEmpty()) {
            Double res = Math.ceil((double)bulkCallList.size() / getNumberOfThread());
            Integer maxBulkCallForThread = res.intValue();
            
            List<List<DTOBulkPutCode>> bulkCallListPartitioned = ListUtils.partition(bulkCallList, maxBulkCallForThread);
            
            for (final List<DTOBulkPutCode> bulkCallListThread : bulkCallListPartitioned)
            {
                threads.add(new Thread()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            for (DTOBulkPutCode bulkObject : bulkCallListThread)
                            {
                                final WorkBulk workBulk = orcidService.getWorkBulk(
                                        bulkObject.getOrcid(), null,
                                        bulkObject.getPutCode());
                                List<Serializable> ss = workBulk.getWorkOrError();
                                for (Serializable s : ss)
                                {
                                    if (s instanceof Work)
                                    {
                                        q.add(convertOrcidWorkToRecord(
                                                bulkObject.getProfile(),
                                                bulkObject.getOrcid(), (Work) s));
                                    }
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            log.error(e.getMessage(), e);
                        }
                    }
                });
            }
            
            
            List<Thread> threadsStarted = new ArrayList<Thread>();
            
            while (!threads.isEmpty() || !threadsStarted.isEmpty())
            {
                if (!threads.isEmpty()
                        && threadsStarted.size() < getNumberOfThread())
                {
                    Thread t = threads.remove(0);
                    t.start();
                    threadsStarted.add(t);
                    // sleep only if there is a cooldown and if there are works left in queue
                    if (!threads.isEmpty() && getCooldown() != 0)
                    {
                        try
                        {
                            Thread.sleep(getCooldown());
                        }
                        catch (InterruptedException e)
                        {
                            log.error(e);
                        }
                    }
                }
                else
                {
                    while (!threadsStarted.isEmpty())
                    {
                        Thread t = threadsStarted.remove(0);
                        try
                        {
                            t.join();
                        }
                        catch (InterruptedException e)
                        {
                            log.error(e);
                        }
                    }
                }            
            }        
        }
        
        while (!q.isEmpty())
        {
        	results.add(q.remove());
        }
        
        return results;
    }

    private Record convertOrcidWorkToRecord(PersonalDetails personalDetails,
            String orcid, Work orcidWork) throws Exception
    {
        MutableRecord record = new SubmissionLookupPublication("");

        Url url = orcidWork.getUrl();
        if (url != null)
        {
            record.addValue("url", new StringValue(url.getValue()));
        }
        WorkTitle workTitle = orcidWork.getTitle();
        if (workTitle != null)
        {
            record.addValue("title", new StringValue(workTitle.getTitle()));
        }

        String workType = orcidWork.getType();
        if (workType != null && StringUtils.isNotBlank(workType))
        {
            record.addValue("providerType", new StringValue(workType));
        }

        ExternalIds identifiers = orcidWork.getExternalIds();
        if (identifiers != null)
        {
            for (ExternalId identifier : identifiers.getExternalId())
            {
                String extType = identifier.getExternalIdType();
                String extIdentifier = identifier.getExternalIdValue();
                record.addValue(extType, new StringValue(extIdentifier));
            }
        }

        String journalTitle = orcidWork.getJournalTitle();
        if (StringUtils.isNotBlank(journalTitle))
        {
            record.addValue("sourceTitle", new StringValue(journalTitle));
        }

        String abs = orcidWork.getShortDescription();
        if (StringUtils.isNotBlank(abs))
        {
            record.addValue("abstract", new StringValue(abs));
        }

        FuzzyDate issued = orcidWork.getPublicationDate();
        if (issued != null)
        {
            record.addValue("issued",
                    new StringValue(String.valueOf(issued.getYear().getValue())));
        }

        String language = orcidWork.getLanguageCode();
        if (language != null)
        {
            record.addValue("language", new StringValue(language));
        }

        LinkedList<Value> authNames = new LinkedList<Value>();
        LinkedList<Value> authOrcid = new LinkedList<Value>();

        WorkContributors contributors = orcidWork.getContributors();
        if (contributors != null)
        {
            for (Contributor contributor : contributors.getContributor())
            {
                CreditName cname = contributor.getCreditName();
                if (cname != null)
                {
                    authNames.add(new StringValue(cname.getValue()));
                }
                OrcidId orcidid = contributor.getContributorOrcid();
                if (orcidid != null)
                {
                    authOrcid.add(new StringValue(orcidid.getUriPath()));
                }
                else
                {
                    authOrcid.add(new StringValue(PLACEHOLER_NO_DATA));
                }
            }

        }
        if (authNames.isEmpty())
        {
            NameType name = personalDetails.getName();
            if (name != null)
            {
            	String value = "Undefined";
                CreditName cname = name.getCreditName();
                if (cname != null)
                {
                    value = cname.getValue();
					
                }
                else
                {
                    try {
						GivenNames givenNames = name.getGivenNames();
						value = name.getFamilyName().getValue()
								+ (givenNames != null ? ", " + givenNames.getValue() : "");
					} catch (NullPointerException e) {
						// the family name is missing! left it as Undefined
					}
                }
                authNames.add(new StringValue(value));
            }
            authOrcid.add(new StringValue(orcid));
        }
        record.addField("authors", authNames);
        record.addField("orcid", authOrcid);
        if(orcidWork.getPutCode()!=null) {
            record.addValue("putcode", new StringValue(orcidWork.getPutCode().toString()));
        }
        
        Citation citation = orcidWork.getCitation();
        if (citation != null)
        {
            String citationType = citation.getCitationType();
            if (citationType != null)
            {
                // Get all the possible data loaders from the Spring
                // configuration
                BTEBatchImportService dls = new DSpace()
                        .getSingletonService(BTEBatchImportService.class);
                List<String> dataLoaderTypes = dls.getFileDataLoaders();
                for (String dataLoaderType : dataLoaderTypes)
                {
                    if (dataLoaderType.equals(citationType))
                    {
                        File file = File.createTempFile("tmp", ".json");
                        Files.write(citation.getCitationValue(), file,
                                Charsets.UTF_8);
                        DataLoader dataLoader = dls.getDataLoaders()
                                .get(dataLoaderType);
                        if (dataLoader instanceof FileDataLoader)
                        {
                            FileDataLoader fdl = (FileDataLoader) dataLoader;
                            fdl.setFilename(file.getAbsolutePath());
                            try{
	                            RecordSet citationRecord = dataLoader.getRecords();
	                            for (Record rr : citationRecord.getRecords())
	                            {
	                                compare(rr, record);
	                            }
                            }catch(Exception e){
                            	
                            }catch(org.jbibtex.TokenMgrError t){
                            	
                            }

                        }
                        break;
                    }
                }
            }
        }
        return convertFields(record);
    }

    private void compare(Record rr, MutableRecord record)
    {
        for (String field : rr.getFields())
        {
            if (!record.hasField(field))
            {
                record.addField(field, rr.getValues(field));
            }
        }
    }

    public int getCooldown()
    {
        return cooldown;
    }

    public void setCooldown(int cooldown)
    {
        this.cooldown = cooldown;
    }

    public int getNumberOfThread()
    {
        if(numberOfThread<=0) {
            numberOfThread=1;
        }
        return numberOfThread;
    }

    public void setNumberOfThread(int numberOfThread)
    {
        this.numberOfThread = numberOfThread;
    }
    
    public class DTOBulkPutCode {
        
        private String orcid;
        private List<String> putCode;
        private PersonalDetails profile;
        
        public DTOBulkPutCode(String orcid, List<String> putCodes,
                PersonalDetails profile)
        {
            this.orcid = orcid;
            this.putCode = new ArrayList<>(putCodes);
            this.profile = profile;
        }
        
        public String getOrcid()
        {
            return orcid;
        }
        public void setOrcid(String orcid)
        {
            this.orcid = orcid;
        }
        public List<String> getPutCode()
        {
            return putCode;
        }
        public void setPutCode(List<String> putCode)
        {
            this.putCode = putCode;
        }
        public PersonalDetails getProfile()
        {
            return profile;
        }
        public void setProfile(PersonalDetails profile)
        {
            this.profile = profile;
        }
        
    }
}
