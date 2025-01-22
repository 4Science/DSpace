/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.action;

import static org.junit.Assert.assertThrows;

import org.dspace.AbstractUnitTest;
import org.dspace.identifier.DOI;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.doi.DOIIdentifierException;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

public class DeleteDOIActionTest extends AbstractUnitTest {

    @Mock
    DOI doi;

    @Mock
    DOIIdentifierProvider provider;

    @InjectMocks
    private DeleteDOIAction deleteDOIAction;

    @Test
    public void testNullDSpaceObject() {
        assertThrows(IllegalArgumentException.class, () -> deleteDOIAction.apply(context, null));
    }


    @Test
    public void testValidDOIObject() throws Exception {
        Mockito.doNothing()
               .when(provider)
               .deleteOnline(Mockito.any(), Mockito.any());

        Mockito.when(doi.getDoi()).thenReturn("DOI TO DELETE");

        deleteDOIAction.apply(context, doi);

        Mockito.verify(provider, Mockito.times(1))
               .deleteOnline(Mockito.eq(context), Mockito.eq("DOI TO DELETE"));
    }

    @Test
    public void testDOIIdentifierException() throws Exception {
        DOIIdentifierException ex =
            new DOIIdentifierException(DOIIdentifierException.DOI_IS_DELETED);

        Mockito.doThrow(ex)
               .when(provider)
               .deleteOnline(Mockito.any(), Mockito.any());

        Mockito.when(doi.getDoi()).thenReturn("DOI TO DELETE");

        assertThrows(DOIIdentifierException.class, () -> deleteDOIAction.apply(context, doi));

        Mockito.verify(provider, Mockito.times(1))
               .deleteOnline(Mockito.eq(context), Mockito.eq("DOI TO DELETE"));
    }


    @Test
    public void testIllegalArgumentException() throws Exception {

        Mockito.doThrow(IllegalArgumentException.class)
               .when(provider)
               .deleteOnline(Mockito.any(), Mockito.any());

        Mockito.when(doi.getDoi()).thenReturn("DOI TO DELETE");

        assertThrows(IllegalArgumentException.class, () -> deleteDOIAction.apply(context, doi));

        Mockito.verify(provider, Mockito.times(1))
               .deleteOnline(Mockito.eq(context), Mockito.eq("DOI TO DELETE"));
    }

}