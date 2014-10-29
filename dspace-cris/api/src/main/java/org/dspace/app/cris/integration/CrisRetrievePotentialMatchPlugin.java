/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowseItem;
import org.dspace.browse.BrowserScope;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

public class CrisRetrievePotentialMatchPlugin implements
        IRetrievePotentialMatchPlugin
{

    /** the logger */
    private static Logger log = Logger
            .getLogger(CrisRetrievePotentialMatchPlugin.class);

    /**
     * the name of the browse index where lookup for potential matches.
     * Configured in the dspace.cfg with the property
     * <code>researcherpage.browseindex</code>
     */
    private static final String researcherPotentialMatchLookupBrowserIndex = ConfigurationManager
            .getProperty(CrisConstants.CFG_MODULE, "researcherpage.browseindex");
    
    @Override
    public Set<Integer> retrieve(Context context, Set<Integer> invalidIds,
            ResearcherPage researcher)
    {


        String authority = researcher.getCrisID();
        Integer id = researcher.getId();

        List<NameResearcherPage> names = ResearcherPageUtils.getAllVariantsName(invalidIds,
                researcher);

        Set<Integer> result = new HashSet<Integer>();
        try
        {
            BrowseIndex bi = BrowseIndex
                    .getBrowseIndex(researcherPotentialMatchLookupBrowserIndex);
            // now start up a browse engine and get it to do the work for us
            BrowseEngine be = new BrowseEngine(context);
            int count = 1;

            for (NameResearcherPage tempName : names)
            {
                log.debug("work on " + tempName.getName() + " with identifier "
                        + tempName.getPersistentIdentifier() + " (" + count
                        + " of " + names.size() + ")");
                // set up a BrowseScope and start loading the values into it
                BrowserScope scope = new BrowserScope(context);
                scope.setBrowseIndex(bi);
                // scope.setOrder(order);
                scope.setFilterValue(tempName.getName());
                // scope.setFilterValueLang(valueLang);
                // scope.setJumpToItem(focus);
                // scope.setJumpToValue(valueFocus);
                // scope.setJumpToValueLang(valueFocusLang);
                // scope.setStartsWith(startsWith);
                // scope.setOffset(offset);
                scope.setResultsPerPage(Integer.MAX_VALUE);
                // scope.setSortBy(sortBy);
                scope.setBrowseLevel(1);
                // scope.setEtAl(etAl);

                BrowseInfo binfo = be.browse(scope);
                log.debug("Find " + binfo.getResultCount()
                        + "item(s) in browsing...");
                for (BrowseItem bitem : binfo.getBrowseItemResults())
                {
                    if (!invalidIds.contains(bitem.getID()))
                    {
                        result.add(bitem.getID());
                    }
                }
            }
        }
        catch (BrowseException e)
        {
            log.error(LogManager.getHeader(context, "getPotentialMatch",
                    "researcher=" + authority), e);
        }

        return result;
    }

 

    @Override
    public Map<NameResearcherPage, Item[]> retrieveGroupByName(Context context,
            Map<String, Set<Integer>> mapInvalids, List<ResearcherPage> rps)
    {
        
      
        Map<NameResearcherPage, Item[]> result = new HashMap<NameResearcherPage, Item[]>();

        for (ResearcherPage researcher : rps)
        {
            String authority = researcher.getCrisID();
            Integer id = researcher.getId();
            BrowseIndex bi;
            try
            {
                bi = BrowseIndex
                        .getBrowseIndex(researcherPotentialMatchLookupBrowserIndex);

                // now start up a browse engine and get it to do the work for us
                BrowseEngine be = new BrowseEngine(context);
                int count = 1;
                List<NameResearcherPage> names = ResearcherPageUtils.getAllVariantsName(mapInvalids.get(authority),
                        researcher);
                for (NameResearcherPage tempName : names)
                {
                    log.info("work on " + tempName.getName()
                            + " with identifier "
                            + tempName.getPersistentIdentifier() + " (" + count
                            + " of " + names.size() + ")");
                    // set up a BrowseScope and start loading the values into it
                    BrowserScope scope = new BrowserScope(context);
                    scope.setBrowseIndex(bi);
                    // scope.setOrder(order);
                    scope.setFilterValue(tempName.getName());
                    // scope.setFilterValueLang(valueLang);
                    // scope.setJumpToItem(focus);
                    // scope.setJumpToValue(valueFocus);
                    // scope.setJumpToValueLang(valueFocusLang);
                    // scope.setStartsWith(startsWith);
                    // scope.setOffset(offset);
                    scope.setResultsPerPage(Integer.MAX_VALUE);
                    // scope.setSortBy(sortBy);
                    scope.setBrowseLevel(1);
                    // scope.setEtAl(etAl);

                    BrowseInfo binfo = be.browse(scope);
                    log.info("Find " + binfo.getResultCount()
                            + "item(s) in browsing...");
                    result.put(tempName, binfo.getItemResults(context));
                    count++;
                }
            }
            catch (BrowseException e)
            {
                log.error(LogManager.getHeader(context, "getPotentialMatch",
                        "researcher=" + authority), e);
            }
        }
        return result;
    }

}
