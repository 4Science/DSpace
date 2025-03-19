/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation.enricher;

import java.util.function.BiConsumer;

import org.dspace.app.rest.annotation.AnnotationRest;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Enricher that will apply the value of a field of the annotation to a field of the item.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class AnnotationFieldEnricher implements GenericItemEnricher<AnnotationRest> {

    String itemFieldSpel;
    String annotationFieldSpel;
    Expression annotationField;
    Expression itemField;

    public AnnotationFieldEnricher(String itemFieldSpel, String annotationFieldSpel) {
        this.itemFieldSpel = itemFieldSpel;
        this.annotationFieldSpel = annotationFieldSpel;
        itemField = new SpelExpressionParser().parseExpression(itemFieldSpel);
        annotationField = new SpelExpressionParser().parseExpression(annotationFieldSpel);
    }

    @Override
    public BiConsumer<Context, AnnotationRest> apply(Item item) {
        return (context, annotation) -> annotationField.setValue(annotation, itemField.getValue(item));
    }
}
