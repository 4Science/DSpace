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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.orcid.OrcidAuthorityValue;
import org.dspace.authority.orcid.OrcidService;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

public class ORCIDAuthority extends RPAuthority {

	private static final int DEFAULT_MAX_ROWS = 10;

	private static Logger log = Logger.getLogger(ORCIDAuthority.class);

	private OrcidService source = new DSpace().getServiceManager().getServiceByName("OrcidSource", OrcidService.class);

	private List<OrcidAuthorityExtraMetadataGenerator> generators = new DSpace().getServiceManager().getServicesByType(OrcidAuthorityExtraMetadataGenerator.class);
	
	@Override
	public Choices getMatches(Context context, String field, String query, int collection, int start, int limit, String locale) {
		Choices choices = super.getMatches(context, field, query, collection, start, limit, locale);		
		return new Choices(addExternalResults(field, query, choices, start, limit<=0?DEFAULT_MAX_ROWS:limit), choices.start, choices.total, choices.confidence, choices.more);
	}
	
	@Override
	public Choices getMatches(Context context, String field, String query, int collection, int start, int limit, String locale, boolean extra) {
		if(extra)
		{
			return getMatches(context, field, query, collection, start, limit, locale);
		} else {
			return super.getMatches(context, field, query, collection, start, limit, locale);
		}
	} 
	
	protected Choice[] addExternalResults(String field, String text, Choices choices, int start, int max) {
		if (source != null) {
			try {
				List<Choice> results = new ArrayList<Choice>();
				List<AuthorityValue> values = source.queryOrcidBioByFamilyNameAndGivenName(text, start, max);
				
				int maxThreads = ConfigurationManager.getIntProperty("orcid.addexternalresults.thread.max", 5);
	        	
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
						
						int num = threadNumber;
						List<AuthorityValue> values = valuesToWork;
						
						@Override
						public void run()
						{
							threadResultsMap.put(num, new ArrayList<Choice>());
							for (AuthorityValue value : values) {
								
									Map<String, String> extras = ((OrcidAuthorityValue)value).choiceSelectMap();
									extras.put("insolr", "false");
									extras.put("link", getLink((OrcidAuthorityValue)value));
									String serviceId = ((OrcidAuthorityValue)value).getServiceId();
									String inst = ((OrcidAuthorityValue)value).getInstitution();
			                        StringBuffer sb = new StringBuffer(value.getValue());
			                        if (StringUtils.isNotBlank(inst))
			                        {
			                            sb.append(" (").append(inst).append(")");
			                        }
			                        sb.append(" - ").append(serviceId);
									extras.putAll(buildExtra(serviceId));
									threadResultsMap.get(num).add(new Choice(value.generateString(), sb.toString(), value.getValue(), extras));
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
						for (Choice choiceValue : threadResultsMap.get(i)) {
							results.add(choiceValue);
						}
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

	private Map<String, String> buildExtra(String value)
    {
        Map<String, String> extras = new HashMap<String,String>();
        
        if(generators!=null) {
            for(OrcidAuthorityExtraMetadataGenerator gg : generators) {
                Map<String, String> extrasTmp = gg.build(source, value);
                extras.putAll(extrasTmp);
            }
        }
        return extras;
    }

    private String getLink(OrcidAuthorityValue val) {
		return source.getBaseURL() + val.getOrcid_id();
	}

}
