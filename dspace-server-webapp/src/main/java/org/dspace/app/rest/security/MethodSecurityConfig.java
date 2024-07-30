/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.util.Optional;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

/**
 * Extension of {@link GlobalMethodSecurityConfiguration} that allow to override
 * the {@link DefaultMethodSecurityExpressionHandler} standard configuration to
 * set a specific ParameterNameDiscoverer. This customization is done to avoid
 * Spring to use the {@link AnnotationParameterNameDiscoverer} when resolving
 * parameters of under security methods for performance reasons. For this reason
 * it is not possible to use the {@link P} and {@link Param} annotations to
 * indicate the name of the parameters referenced in the methods.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {

    @Autowired
    private PermissionEvaluator dSpacePermissionEvaluator;

    @Autowired
    private ApplicationContext applicationContext;

    private DefaultMethodSecurityExpressionHandler expressionHandler;

    @Override
    protected MethodSecurityExpressionHandler createExpressionHandler() {
        this.expressionHandler = new DefaultMethodSecurityExpressionHandler();
        this.expressionHandler.setApplicationContext(applicationContext);
        this.expressionHandler.setParameterNameDiscoverer(new LocalVariableTableParameterNameDiscoverer());
        this.expressionHandler.setPermissionEvaluator(dSpacePermissionEvaluator);
        return expressionHandler;
    }

    @Override
    public void afterSingletonsInstantiated() {
        getSingleBean(PermissionEvaluator.class)
            .ifPresent(((DefaultMethodSecurityExpressionHandler)this.expressionHandler)::setPermissionEvaluator);
    }

    private <T> Optional<T> getSingleBean(Class<T> type) {
        try {
            return Optional.of(applicationContext.getBean(type));
        } catch (NoSuchBeanDefinitionException e) {
            return Optional.empty();
        }
    }
}
