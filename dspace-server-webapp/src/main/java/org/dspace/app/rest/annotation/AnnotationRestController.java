/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import java.sql.SQLException;
import java.util.List;
import javax.ws.rs.NotAuthorizedException;

import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@RestController
@RequestMapping(
    value = "/annotation",
    produces = "application/json;charset=UTF-8"
)
public class AnnotationRestController {

    private static final Logger log = LoggerFactory.getLogger(AnnotationRestController.class);
    @Autowired
    AnnotationService annotationService;

    protected Context obtainContext() {
        return ContextUtil.obtainCurrentRequestContext();
    }

    @RequestMapping(method = RequestMethod.GET, value = "/search")
    public ResponseEntity<List<AnnotationRest>> search(@RequestParam String uri) {
        return new ResponseEntity<>(
            annotationService.search(obtainContext(), uri),
            HttpStatus.OK
        );
    }

    @RequestMapping(method = { RequestMethod.POST, RequestMethod.OPTIONS }, value = "/create")
    public ResponseEntity<AnnotationRest> create(@RequestBody AnnotationRest annotation) {
        Context context = obtainContext();
        WorkspaceItem workspaceItem = null;
        try {
            workspaceItem = annotationService.create(context, annotation);
            context.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Error creating annotation", e);
        } catch (IllegalArgumentException e) {
            log.error("Cannot create the annotation: {}", annotation, e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (NotAuthorizedException e) {
            log.error("Current user is not authorized to create the annotation: {}", annotation, e);
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(
            annotationService.convert(
                context, annotationService.findByItemId(context, workspaceItem.getItem().getID())
            ),
            HttpStatus.OK
        );
    }

    @RequestMapping(method = RequestMethod.POST, value = "/update")
    public ResponseEntity<AnnotationRest> update(@RequestBody AnnotationRest annotation) {
        Context context = obtainContext();
        try {
            // parse the annotation to remove the created field
            // since it's an update operation, the created field should be null
            if (annotation.created != null) {
                annotation.created = null;
            }
            annotationService.update(context, annotation);
            context.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating annotation", e);
        } catch (IllegalArgumentException e) {
            log.error("Cannot find annotation with id: {}", annotation.id, e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (NotAuthorizedException e) {
            log.error("Current user is not authorized to update annotation with id: {}", annotation.id, e);
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(
            annotationService.convert(context, annotationService.findById(context, annotation.getId())),
            HttpStatus.OK
        );
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/destroy")
    public ResponseEntity<?> destroy(@RequestParam String uri) {
        Context context = obtainContext();
        try {
            annotationService.delete(context, annotationService.findById(context, uri));
            context.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting annotation", e);
        } catch (IllegalArgumentException e) {
            log.error("Cannot find annotation with id: {}", uri, e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (NotAuthorizedException e) {
            log.error("Current user is not authorized to delete annotation with id: {}", uri, e);
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
