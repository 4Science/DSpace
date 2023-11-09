/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.model.ItemRest.PLURAL_NAME;
import static org.dspace.app.rest.model.UnpaywallItemVersionsRest.CATEGORY;
import static org.dspace.app.rest.model.UnpaywallItemVersionsRest.VERSIONS;
import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;

import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.UnpaywallItemVersionsRest;
import org.dspace.app.rest.model.hateoas.UnpaywallItemVersionsResource;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.unpaywall.dto.UnpaywallItemVersionDto;
import org.dspace.unpaywall.service.UnpaywallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for work with item from unpaywall api.
 */
@RestController
@RequestMapping("/api/" + CATEGORY + "/" + PLURAL_NAME + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/unpaywall")
public class UnpaywallItemController {

    @Autowired
    private UnpaywallService unpaywallService;

    @Autowired
    private ConverterService converter;

    @GetMapping("/" + VERSIONS)
    @PreAuthorize("hasPermission(#uuid, 'ITEM', 'READ')")
    public UnpaywallItemVersionsResource getVersions(@PathVariable UUID uuid, HttpServletRequest request) {
        Context context = ContextUtil.obtainContext(request);
        List<UnpaywallItemVersionDto> itemVersions = unpaywallService.getItemVersions(context, uuid);
        UnpaywallItemVersionsRest itemVersionsRest = new UnpaywallItemVersionsRest(itemVersions);
        return converter.toResource(itemVersionsRest);
    }
}
