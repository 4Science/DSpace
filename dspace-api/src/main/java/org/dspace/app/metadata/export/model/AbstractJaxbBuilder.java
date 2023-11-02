/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.metadata.export.model;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;
import javax.xml.bind.JAXBElement;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public abstract class AbstractJaxbBuilder<T, C> {

    T object;
    Class<T> clazz;

    protected final ObjectFactory objectFactory = new ObjectFactory();

    protected AbstractJaxbBuilder(Class<T> clazz) {
        this.clazz = clazz;
    }

    protected T getObejct() {
        if (object == null) {
            try {
                object = clazz.getDeclaredConstructor().newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        return object;
    }

    public T build() {
        return object;
    }

    protected void addChildElement(C value, Function<C, JAXBElement<C>> mapper) {
        if (value == null) {
            return;
        }
        addChildElement(mapper.apply(value));
    }

    protected abstract void addChildElement(JAXBElement<C> v);
}
