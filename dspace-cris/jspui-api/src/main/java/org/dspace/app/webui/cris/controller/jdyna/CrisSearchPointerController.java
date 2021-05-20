/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.jdyna;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.widget.WidgetPointerDO;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;

import it.cilea.osd.jdyna.model.AWidget;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.service.IAutoCreateApplicationService;
import it.cilea.osd.jdyna.utils.SelectableDTO;
import it.cilea.osd.jdyna.web.controller.SearchPointerController;
import it.cilea.osd.jdyna.web.tag.DisplayPointerTagLibrary;
import it.cilea.osd.jdyna.widget.WidgetPointer;

public class CrisSearchPointerController extends
        SearchPointerController<PropertiesDefinition, ACrisObject>
{
    private CrisSearchService searchService;

    @Override
    protected List<SelectableDTO> getResult(AWidget widget, String query, String expression, String... filtro)
    {
        Context context = null;
        WidgetPointer widgetPointer = (WidgetPointer)widget;
        List<SelectableDTO> results = new ArrayList<SelectableDTO>();
        try
        {
            context = new Context();
            boolean searchValueFound = false;
            
            List<DSpaceObject> objects = getSearchService().search(context,
                    query + "*", null, true, 0, Integer.MAX_VALUE, filtro);
            for (DSpaceObject obj : objects)
            {
                ACrisObject real = (ACrisObject) obj;
                String display = (String) DisplayPointerTagLibrary.evaluate(
                        obj, expression);
                SelectableDTO dto = new SelectableDTO(
                        real.getIdentifyingValue(), display);
                results.add(dto);
                
                if (display != null && display.equalsIgnoreCase(query)) {
                	searchValueFound = true;
                }
            }
            
            if (!searchValueFound) {
            	String type = getPointerType(widgetPointer);
            	if (ConfigurationManager.getBooleanProperty("cris", "widgetpointer."+type+".create-new")) {
	            	String tag = I18nUtil
	                        .getMessage("CrisSearchPointerController.create-new", context).trim();
		        	Integer temporaryId = null;
		        	if (getApplicationService() instanceof IAutoCreateApplicationService) {
		        		temporaryId = ((IAutoCreateApplicationService)getApplicationService()).generateTemporaryPointerCandidate(type, query, tag);
		        	}
		        	if (temporaryId != null) {
		        		results.add(new SelectableDTO(
		        			Integer.toString(temporaryId), query + " " + tag));
		        	}
            	}
            }
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }

        return results;
    }

    public void setSearchService(CrisSearchService searchService)
    {
        this.searchService = searchService;
    }

    public CrisSearchService getSearchService()
    {
        return searchService;
    }

    @Override
    protected String getDisplay(AWidget widget)
    {
        WidgetPointer widgetPointer = (WidgetPointer)widget;
        return widgetPointer.getDisplay();
    }
    
    @Override
    protected String[] getFilter(AWidget widget)
    {
        WidgetPointer widgetPointer = (WidgetPointer)widget;
        String filtro = widgetPointer.getFiltro();
        Class target = widgetPointer.getTargetValoreClass();

        String resourcetype = "search.resourcetype: [9 TO 11]";
        if (target.equals(ResearcherPage.class))
        {
            resourcetype = "search.resourcetype:" + CrisConstants.RP_TYPE_ID;
        }
        else if (target.equals(Project.class))
        {
            resourcetype = "search.resourcetype:"
                    + CrisConstants.PROJECT_TYPE_ID;
        }
        else if (target.equals(OrganizationUnit.class))
        {
            resourcetype = "search.resourcetype:" + CrisConstants.OU_TYPE_ID;
        }
        else if (target.equals(ResearchObject.class))
        {
            resourcetype = ((WidgetPointerDO) widgetPointer)
                    .getFilterExtended();
        }
        if(filtro==null || filtro.isEmpty()) {
            return new String[] { resourcetype };    
        }
        return new String[] { resourcetype, filtro };
    }
    
    private String getPointerType(WidgetPointer widgetPointer) {
    	Class target = widgetPointer.getTargetValoreClass();
    	if (target.equals(ResearcherPage.class))
        {
            return CrisConstants.getEntityTypeText(CrisConstants.RP_TYPE_ID);
        }
        else if (target.equals(Project.class))
        {
        	return CrisConstants.getEntityTypeText(CrisConstants.PROJECT_TYPE_ID);
        }
        else if (target.equals(OrganizationUnit.class))
        {
        	return CrisConstants.getEntityTypeText(CrisConstants.OU_TYPE_ID);
        }
        else if (target.equals(ResearchObject.class))
        {
			return CrisConstants.getEntityTypeText(Integer.parseInt(
					((WidgetPointerDO) widgetPointer).getFilterExtended().substring("search.resourcetype:".length())));
        }
    	return null;
    }
}
