/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@RestController
@RequestMapping("/api/" + AnnotationRest.ANNOTATION)
public class AnnotationRestController {

    @GetMapping(value = "/search", produces = {"application/ld+json"})
    public AnnotationRest[] search(@RequestParam String uri) {
        return null;
    }

    @PostMapping(value = "/create", produces = {"application/ld+json"})
    public AnnotationRest create(@RequestBody AnnotationRest annotation) {
        annotation.setId("customId");
        return annotation;
    }

    @PostMapping(value = "/update", produces = {"application/ld+json"})
    public AnnotationRest update(@RequestBody AnnotationRest annotation) {
        return null;
    }

    @DeleteMapping(value = "/destroy", produces = {"application/ld+json"})
    public AnnotationRest destroy(@RequestBody AnnotationRest annotation) {
        return null;
    }

}
