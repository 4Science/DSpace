/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.orcid.OrcidAuthorityValue;
import org.dspace.authority.orcid.OrcidService;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

/**
 * 
 * Authority to aggregate "extra" value to single choice 
 * 
 * @author Pascarelli Luigi Andrea
 *
 */
public class ORCIDMultiAuthority extends RPMultiAuthority {

	private static final int DEFAULT_MAX_ROWS = 10;

	private static Logger log = Logger.getLogger(ORCIDMultiAuthority.class);

	private OrcidService source = new DSpace().getServiceManager().getServiceByName("OrcidSource", OrcidService.class);

	public List<OrcidAuthorityExtraMetadataGenerator> generators = new DSpace().getServiceManager().getServicesByType(OrcidAuthorityExtraMetadataGenerator.class);
	
	@Override
	public Choices getMatches(Context context, String field, String query, int collection, int start, int limit, String locale) {
		Choices choices = super.getMatches( context, field, query, collection, start, limit, locale);		
		return new Choices(addExternalResults(field, query, choices, start, limit==0?DEFAULT_MAX_ROWS:limit), choices.start, choices.total, choices.confidence, choices.more);
	}

	protected Choice[] addExternalResults(String field, String text, Choices choices, int start, int max) {
		if (source != null) {
			try {
				List<Choice> results = new ArrayList<Choice>();
				List<AuthorityValue> values = null;
				if(Pattern.matches(ORCIDAuthority.ORCID_REGEX, text)) {
					values = source.queryOrcidByOrcidId(text);
				} else {
					values = source.queryOrcidBioByFamilyNameAndGivenName(text, start, max);
				}
				
				int maxThreads = ConfigurationManager.getIntProperty("orcid.addexternalresults.thread.max", 5);
				
				final String fieldInWork = field;
				final Integer maxItems;
		        Double size = (double) values.size();
		        Double res = Math.ceil(size / maxThreads);
		        maxItems = res.intValue();
		        
		        List<Thread> threads = new ArrayList<Thread>();
		        final Map<Integer, List<Choice>> threadResultsMap = new HashMap<>();
		        
		        for (int i = 0; i < maxThreads; i++) {

		        	final List<AuthorityValue> valuesToWork = new ArrayList<>();

		        	size = (double) values.size();
		        	for (int j = 0; j < maxItems; j++) {

		        		if (values.size() <= 0) {
		        			break;
		        		}
		        		if ((j == (maxItems - 1)) && ((size / (maxThreads - i)) <= (maxItems - 1))) {
		        			break;
		        		}
		        		valuesToWork.add(values.remove(0));
		        	}

		        	final Integer threadNumber = i;
		        	threads.add(new Thread() {

		        		String field = fieldInWork;
		        		int num = threadNumber;
		        		List<AuthorityValue> values = valuesToWork;

		        		@Override
		        		public void run()
		        		{
		        			threadResultsMap.put(num, new ArrayList<Choice>());
		        			for (AuthorityValue value : values) 
		        			{
                                Map<String, String> extras = ((OrcidAuthorityValue)value).choiceSelectMap();
                                extras.put("insolr", "false");
                                extras.put("link", getLink((OrcidAuthorityValue)value));
		    					threadResultsMap.get(num).addAll(buildAggregateByExtra(value, field));
		        				Thread.yield();
		        			}
		        		}
		        	});
		        }
		        
		        List<Thread> threadsStarted = new ArrayList<Thread>();

		        while (!threads.isEmpty() || !threadsStarted.isEmpty()) {
		        	if (!threads.isEmpty() && threadsStarted.size() < maxThreads) {
		        		Thread t = threads.remove(0);
		        		t.start();
		        		threadsStarted.add(t);
		        	}else {
		        		Thread t = threadsStarted.remove(0);
		        		try {
		        			t.join();
		        		} catch (InterruptedException e) {
		        			log.error(e.getMessage(), e);
		        		}
		        	}
		        }
		        
		        for (int i = 0; i < threadResultsMap.size(); i++) {
		        	if(!threadResultsMap.get(i).isEmpty())
		        	{
	        			results.addAll(threadResultsMap.get(i));
		        	}
		        }
		        
				return (Choice[])ArrayUtils.addAll(choices.values, results.toArray(new Choice[results.size()]));
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		} else {
			log.warn("external source for authority not configured");
		}	
		return choices.values;
	}

    public List<Choice> buildAggregateByExtra(AuthorityValue value, String field)
    {
        List<Choice> choiceList = new LinkedList<Choice>();
        if(generators!=null) {
            OrcidAuthorityExtraMetadataGenerator defaultGenerator = null;
            boolean generatorFound = false;
            for(OrcidAuthorityExtraMetadataGenerator gg : generators) {
                String parentField = gg.getParentInputFormMetadata();
                if ( null == parentField ) {
                    defaultGenerator = gg;
                }
                
                if ( field.equals(parentField)) {
                    generatorFound = true;
                    choiceList.addAll(gg.buildAggregate(source, value));
                }
            }
            if ( !generatorFound && null != defaultGenerator ) {
                choiceList.addAll(defaultGenerator.buildAggregate(source, value));
            }
        }
        return choiceList;
    }
	
    private String getLink(OrcidAuthorityValue val) {
		return source.getBaseURL() + val.getOrcid_id();
	}

}
