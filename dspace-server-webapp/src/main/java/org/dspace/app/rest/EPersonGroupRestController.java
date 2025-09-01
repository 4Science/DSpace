/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.util.UUID;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.hateoas.EPersonResource;
import org.dspace.app.rest.repository.CollectionRestRepository;
import org.dspace.app.rest.repository.EPersonGroupLinkRepository;
import org.dspace.app.rest.repository.EPersonRestRepository;
import org.dspace.core.Context;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/api/")
public class EPersonGroupRestController implements InitializingBean {
    @Autowired
    private EPersonRestRepository ePersonRestRepository;
    @Autowired
    private EPersonGroupLinkRepository ePersonGroupLinkRepository;
    @Autowired
    private ConverterService converter;
    @Autowired
    private CollectionRestRepository collectionRestRepository;

    /**
     * This request can be used to join a user to a target group by using a registration data token will be replaced
     * by the {@link EPersonRegistrationRestController} features.
     *
     * @param context
     * @param uuid
     * @param token
     * @return
     * @throws Exception
     */
    @Deprecated
    @RequestMapping(method = RequestMethod.POST, value = EPersonRest.CATEGORY + "/"
            + EPersonRest.PLURAL_NAME + "/{uuid}/" + EPersonRest.GROUPS)
    public ResponseEntity<RepresentationModel<?>> joinUserToGroups(Context context,
                        @PathVariable String uuid, @RequestParam String token) throws Exception {
        EPersonRest ePersonRest =  ePersonRestRepository.joinUserToGroups(UUID.fromString(uuid), token);
        EPersonResource ePersonResource = converter.toResource(ePersonRest);
        context.commit();
        return ControllerUtils.toResponseEntity(HttpStatus.CREATED, new HttpHeaders(),
                ePersonResource);
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
