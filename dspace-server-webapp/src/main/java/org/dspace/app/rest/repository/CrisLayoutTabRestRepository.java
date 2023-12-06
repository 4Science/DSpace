/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.CrisLayoutTabConverter;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.CrisLayoutBoxRest;
import org.dspace.app.rest.model.CrisLayoutMetricsConfigurationRest;
import org.dspace.app.rest.model.CrisLayoutTabRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.ResourcePatch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBoxTypes;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.service.CrisLayoutTabService;
import org.dspace.metrics.CrisItemMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
/**
 * This is the repository responsible to manage CrisLayoutTab Rest object
 *
 * @author Danilo Di Nuzzo (danilo dot dinuzzo at 4science dot it)
 *
 */
@Component(CrisLayoutTabRest.CATEGORY + "." + CrisLayoutTabRest.NAME)
public class CrisLayoutTabRestRepository extends DSpaceRestRepository<CrisLayoutTabRest, Integer>
    implements ReloadableEntityObjectRepository<CrisLayoutTab, Integer> {

    public static final String SCOPE_ITEM_ATTRIBUTE = "cris-layout-tab.scope-item";

    @Autowired
    private CrisLayoutTabService service;

    @Autowired
    private CrisLayoutTabConverter tabConverter;

    @Autowired
    private ResourcePatch<CrisLayoutTab> resourcePatch;

    @Autowired
    private CrisItemMetricsService metricsService;

    @Override
    @PreAuthorize("permitAll")
    public CrisLayoutTabRest findOne(Context context, Integer id) {
        CrisLayoutTab tab = null;
        try {
            tab = service.findAndEagerlyFetch(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if ( tab == null ) {
            return null;
        }
        return converter.toRest(tab, utils.obtainProjection());
    }

    @Override
    public Page<CrisLayoutTabRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @SearchRestMethod(name = "findByItem")
    public Page<CrisLayoutTabRest> findByItem(
        @Parameter(value = "uuid", required = true) String itemUuid, Pageable pageable) throws SQLException {
        List<CrisLayoutTab> tabList = service.findByItem(obtainContext(), itemUuid);
        getRequestService().getCurrentRequest().setAttribute(SCOPE_ITEM_ATTRIBUTE, itemUuid);
        Page<CrisLayoutTabRest> restTabs = converter.toRestPage(tabList, pageable, utils.obtainProjection());
        restTabs = filterTabWithoutRows(pageable, restTabs);
        return filterBoxesWithMetricsType(restTabs, itemUuid);
    }

    @SearchRestMethod(name = "findByEntityType")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<CrisLayoutTabRest> findByEntityType(
            @Parameter(value = "type", required = true) String type, Pageable pageable) {
        Context context = obtainContext();
        List<CrisLayoutTab> tabList = null;
        Long totalRow = null;
        try {
            totalRow = service.countByEntityType(context, type);
            tabList = service.findByEntityType(
                context,
                type,
                null,
                pageable.getPageSize(),
                (pageable.getPageNumber() * pageable.getPageSize()) );
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (tabList == null) {
            return null;
        }
        return converter.toRestPage(tabList, pageable, totalRow, utils.obtainProjection());
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.DSpaceRestRepository#getDomainClass()
     */
    @Override
    public Class<CrisLayoutTabRest> getDomainClass() {
        return CrisLayoutTabRest.class;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.ReloadableEntityObjectRepository#findDomainObjectByPk
     * (org.dspace.core.Context, java.io.Serializable)
     */
    @Override
    public CrisLayoutTab findDomainObjectByPk(Context context, Integer id) throws SQLException {
        return service.find(context, id);
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.repository.ReloadableEntityObjectRepository#getPKClass()
     */
    @Override
    public Class<Integer> getPKClass() {
        return Integer.class;
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected CrisLayoutTabRest createAndReturn(Context context) {
        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        CrisLayoutTabRest tabRest = null;
        try {
            tabRest = mapper.readValue(req.getInputStream(), CrisLayoutTabRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("error parsing the body", e1);
        }

        CrisLayoutTab tab = tabConverter.toModel(context, tabRest);
        try {
            service.create(context, tab);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return converter.toRest(tab, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public void patch(
            Context context, HttpServletRequest request, String apiCategory, String model, Integer id,
            Patch patch)
            throws UnprocessableEntityException, DSpaceBadRequestException {
        CrisLayoutTab tab = null;
        try {
            tab = service.find(context, id);
            if (tab == null) {
                throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + id + " not found");
            }
            resourcePatch.patch(obtainContext(), tab, patch.getOperations());
            service.update(context, tab);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected void delete(Context context, Integer id) throws AuthorizeException {
        try {
            CrisLayoutTab tab = service.find(context, id);
            if (tab == null) {
                throw new ResourceNotFoundException("CrisLayoutTab with id: " + id + " not found");
            }
            service.delete(context, tab);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Page<CrisLayoutTabRest> filterTabWithoutRows(Pageable pageable, Page<CrisLayoutTabRest> restTabs) {
        List<CrisLayoutTabRest> listOfTabs = restTabs.filter(tab -> CollectionUtils.isNotEmpty(tab.getRows())).toList();
        return utils.getPage(listOfTabs, pageable);
    }

    private Page<CrisLayoutTabRest> filterBoxesWithMetricsType(Page<CrisLayoutTabRest> restTabs, String itemUuid) {
        List<CrisLayoutTabRest> listOfTabs = restTabs.toList();

        // Get CrisLayoutBoxRest with boxType=METRICS
        List<CrisLayoutBoxRest> boxes = listOfTabs
            .stream()
            .flatMap(t -> t.getRows()
                .stream()
                .flatMap(r -> r.getCells()
                    .stream()
                    .flatMap(c -> c.getBoxes()
                        .stream()
                        .filter(b -> b.getBoxType()
                            .equals(CrisLayoutBoxTypes.METRICS.name())))))
            .collect(Collectors.toList());

        // Set new metrics for each box
        boxes.forEach(box -> {
            CrisLayoutMetricsConfigurationRest boxConfiguration =
                ((CrisLayoutMetricsConfigurationRest) box.getConfiguration());

            List<String> boxMetrics = boxConfiguration.getMetrics();
            List<String> itemMetrics = metricsService.getMetrics(obtainContext(), UUID.fromString(itemUuid))
                .stream()
                .map(CrisMetrics::getMetricType)
                .collect(Collectors.toList());

            // Inner join metrics of box and item and distinct the results
            boxConfiguration.setMetrics(boxMetrics
                                            .stream()
                                            .filter(b -> itemMetrics
                                                .stream()
                                                .anyMatch(i -> i.equals(b)))
                                            .distinct()
                                            .collect(Collectors.toList()));
        });

        return utils.getPage(listOfTabs, restTabs.getPageable());
    }
}
