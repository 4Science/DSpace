/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation;

import org.dspace.content.MetadataFieldName;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public abstract class AbstractMetadataSpelMapper {

    String spel;
    MetadataFieldName metadata;
    Class<?> clazz;

    Expression fieldExpression;
    SpelExpressionParser expressionParser;

    public AbstractMetadataSpelMapper(String spel, MetadataFieldName metadata, Class<?> clazz) {
        this.spel = spel;
        this.metadata = metadata;
        this.clazz = clazz;
        expressionParser = new SpelExpressionParser();
        fieldExpression = expressionParser.parseExpression(spel);
    }
}
