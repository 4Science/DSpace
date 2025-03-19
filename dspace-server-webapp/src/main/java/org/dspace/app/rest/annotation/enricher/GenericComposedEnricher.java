/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.annotation.enricher;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Enricher that compose a list of functions that returns a value of type T.
 * The value of type T is then reduced by a reducer function that returns a value of type T.
 * The result of the reducer function is then set to the target field of the annotation target.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public abstract class GenericComposedEnricher<R, T> implements GenericItemEnricher<R> {

    final String targetField;
    final List<Function<Item, T>> composer;
    final BinaryOperator<T> reducer;
    final Expression targetFieldExpression;

    public GenericComposedEnricher(
        String targetField,
        List<Function<Item, T>> composer,
        BinaryOperator<T> reducer
    ) {
        this.targetField = targetField;
        this.composer = composer;
        this.reducer = reducer;
        targetFieldExpression = new SpelExpressionParser().parseExpression(targetField);
    }

    @Override
    public BiConsumer<Context, R> apply(Item item) {
        return (context, annotationTarget) ->
            composeString(item).ifPresent(
                composed -> targetFieldExpression.setValue(annotationTarget, composed)
            );
    }

    protected Optional<T> composeString(Item item) {
        return composer.stream()
                       .map(c -> c.apply(item))
                       .reduce(reducer);
    }

}
