package org.dspace.app.webui.cris.controller;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.EditTabDynamicObject;
import org.dspace.app.cris.model.jdyna.EditTabOrganizationUnit;
import org.dspace.app.cris.model.jdyna.EditTabProject;
import org.dspace.app.cris.model.jdyna.EditTabResearcherPage;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.cris.util.CrisAuthorizeManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;
import org.springframework.web.servlet.ModelAndView;

import it.cilea.osd.common.controller.BaseAbstractController;
import it.cilea.osd.jdyna.web.AbstractEditTab;
import it.cilea.osd.jdyna.web.ITabService;

/**
 * This SpringMVC controller is used to fetch private CRIS entities of the
 * current authorized user.
 * 
 * @author Cornelius Matějka <cornelius.matejka@uni-bamberg.de>
 * 
 */
@SuppressWarnings("rawtypes")
public class PrivateEntitiesController extends BaseAbstractController {

	private final ITabService applicationService;
	private final SearchService searchService;
	private final Map<Class<ACrisObject>, Integer> configuration;

	public PrivateEntitiesController() {

		DSpace dspace = new DSpace();
		this.applicationService = dspace.getServiceManager().getServiceByName("applicationService",
				ApplicationService.class);
		this.searchService = dspace.getSingletonService(SearchService.class);
		this.configuration = parseConfiguration();

	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		Map<String, Object> model = new HashMap<String, Object>();

		Context context = UIUtil.obtainContext(request);
		List<ACrisObject> privateEntitiesOfCurrentUser = getPrivateEntitiesForCurrentUser(context);

		model.put("privateEntities", privateEntitiesOfCurrentUser);

		return new ModelAndView("privateEntities", model);
	}

	private Map<Class<ACrisObject>, Integer> parseConfiguration() {
		/*
		 * Parse configuration for private entities: '<entity_class>:<entity_typeID>'
		 * configured in cris.cfg
		 */
		Map<Class<ACrisObject>, Integer> configuration = new HashMap<>();
		for (String st : ConfigurationManager.getProperty("cris", "rp.privateentities").split(",")) {
			String[] sta = st.split(":");
			Class<ACrisObject> clazz;
			int typeID;
			try {
				clazz = (Class<ACrisObject>) Class.forName(sta[0].trim());
				typeID = Integer.parseInt(sta[1].trim());
				configuration.put(clazz, typeID);
			} catch (ClassNotFoundException e) {
				log.error(e);
			}
		}
		return configuration;
	}

	private List<ACrisObject> getPrivateEntitiesForCurrentUser(Context context) {

		List<ACrisObject> allPrivateEntities = getAllPrivateEntities();
		List<ACrisObject> myPrivateEntities = new LinkedList<>();
		try {
			myPrivateEntities = getMyPrivateEntities(allPrivateEntities, context);
		} catch (SQLException e) {
			log.error(e);
		}

		return myPrivateEntities;
	}

	private List<ACrisObject> getAllPrivateEntities() {

		List<ACrisObject> privateEntities = new LinkedList<>();

		for (Class<ACrisObject> key : configuration.keySet()) {
			try {
				SolrQuery query = new SolrQuery("{!field f=search.resourcetype}" + configuration.get(key));
				query.setFilterQueries("discoverable: \"false\"");
				query.setRows(Integer.MAX_VALUE);

				QueryResponse response = this.searchService.search(query);

				SolrDocumentList docList = response.getResults();

				Iterator<SolrDocument> solrDoc = docList.iterator();
				while (solrDoc.hasNext()) {
					SolrDocument doc = solrDoc.next();
					Integer coID = (Integer) doc.getFirstValue("search.resourceid");
					ACrisObject co = this.applicationService.get(key, coID);
					if (co != null)
						privateEntities.add(co);
				}
			} catch (SearchServiceException e) {
				log.error(e);
			}
		}

		return privateEntities;
	}

	private List<ACrisObject> getMyPrivateEntities(List<ACrisObject> privateProjects, Context context)
			throws SQLException {

		List<ACrisObject> myPrivateEntities = new LinkedList<>();

		for (ACrisObject co : privateProjects) {
			boolean canEdit = CrisAuthorizeManager.canEdit(context, this.applicationService, getTabClass(co), co);
			if (canEdit) {
				myPrivateEntities.add(co);
			}
		}

		return myPrivateEntities;

	}

	private Class<? extends AbstractEditTab> getTabClass(ACrisObject co) {
		if (co instanceof Project) {
			return EditTabProject.class;
		} else if (co instanceof OrganizationUnit) {
			return EditTabOrganizationUnit.class;
		} else if (co instanceof ResearcherPage) {
			return EditTabResearcherPage.class;
		} else {
			return EditTabDynamicObject.class;
		}
	}

}
