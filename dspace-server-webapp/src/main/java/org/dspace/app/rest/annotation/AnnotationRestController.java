/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import java.sql.SQLException;

import org.dspace.app.rest.repository.AbstractDSpaceRestRepository;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping(
    value = "/api/" + AnnotationRest.ANNOTATION,
    consumes = {"application/ld+json"},
    produces = {"application/ld+json"}
)
public class AnnotationRestController extends AbstractDSpaceRestRepository {

    @Autowired
    AnnotationService annotationService;

    @GetMapping("/search")
    public AnnotationRest[] search(@RequestParam String uri) {
        return null;
    }

    @PostMapping("/create")
    public AnnotationRest create(@RequestBody AnnotationRest annotation) {
        Context context = obtainContext();
        try {
            annotationService.create(context, annotation);
            context.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Error creating annotation", e);
        }
        return annotationService.convert(context, annotationService.findById(context, annotation.getId()));
    }

    @PostMapping("/update")
    public AnnotationRest update(@RequestBody AnnotationRest annotation) {
        Context context = obtainContext();
        try {
            annotationService.update(context, annotation);
            context.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating annotation", e);
        }
        return annotationService.convert(context, annotationService.findById(context, annotation.getId()));
    }

    @DeleteMapping("/destroy")
    public void destroy(@RequestParam String uri) {
        Context context = obtainContext();
        annotationService.delete(context, annotationService.findById(context, uri));
    }

}
