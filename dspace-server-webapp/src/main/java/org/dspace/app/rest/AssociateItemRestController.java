/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.exception.ResourceAlreadyExistsException;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.AssociateItemModeRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.edit.service.AssociateItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This is a rest controller to change associateitem modes.
 * It provided basic functionality and just returns the response code.
 * Rest Endpoints needs authentication.
 * @author Florian Gantner (florian.gantner@uni-bamberg.de)
 */
@RequestMapping("/api/" + AssociateItemModeRest.CATEGORY + "/" + AssociateItemModeRest.NAMESHORT)
@RestController
public class AssociateItemRestController implements InitializingBean {

    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AssociateItemService associateItemService;

    @Autowired
    ConverterService converter;

    @Autowired
    Utils utils;

    @Override
    public void afterPropertiesSet() {
        discoverableEndpointsService
            .register(this, Arrays.asList(
                Link.of("/api/" + AssociateItemModeRest.CATEGORY + "/" + AssociateItemModeRest.NAMESHORT,
                    AssociateItemModeRest.NAMESHORT)));
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @RequestMapping(method = RequestMethod.PUT, value = "/create")
    public boolean create(HttpServletRequest request,
                       HttpServletResponse response, @RequestParam(name = "sourceuuid") UUID sourceuuid,
                          @RequestParam(name = "targetuuid") UUID targetuuid, String modename) {
        Context context = null;
        boolean created = false;
        try {
            if (configurationService.getBooleanProperty("associateitem.enabled", true) == false) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return false;
            }
            context = ContextUtil.obtainContext(request);
            created = this.associateItemService.create(context, sourceuuid, targetuuid, modename);
            response.setStatus(HttpServletResponse.SC_CREATED);
            context.commit();
        } catch (ResourceAlreadyExistsException e) {
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (AuthorizeException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            //TODO: check other cases
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (Objects.nonNull(context) && context.isValid()) {
                context.close();
            }
        }
        return created;
    }

    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    @RequestMapping(method = RequestMethod.DELETE, value = "/delete")
    public boolean delete(HttpServletRequest request,
                          HttpServletResponse response, @RequestParam(name = "sourceuuid") UUID sourceuuid,
                          @RequestParam(name = "targetuuid") UUID targetuuid, String modename) {
        Context context = null;
        boolean delete = false;
        try {
            if (configurationService.getBooleanProperty("associateitem.enabled", true) == false) {
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return false;
            }
            context = ContextUtil.obtainContext(request);
            delete = this.associateItemService.delete(context, sourceuuid, targetuuid, modename);
            context.commit();
        } catch (AuthorizeException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (Objects.nonNull(context) && context.isValid()) {
                context.close();
            }
        }
        return delete;
    }
}
