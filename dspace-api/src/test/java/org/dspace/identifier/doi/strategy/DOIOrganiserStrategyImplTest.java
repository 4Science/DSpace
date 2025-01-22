/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.strategy;

import org.dspace.AbstractUnitTest;
import org.dspace.identifier.DOI;
import org.dspace.identifier.doi.action.DOIAction;
import org.dspace.identifier.doi.iterator.DOIIterator;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

public class DOIOrganiserStrategyImplTest extends AbstractUnitTest {

    @Mock
    DOIAction doiAction;

    @Mock
    DOIIterator iterator;

    @InjectMocks
    DOIOrganiserStrategyImpl doiStrategy;

    @Test
    public void testEmptyIterator() throws Exception {
        Mockito.when(iterator.hasNext()).thenReturn(false);

        doiStrategy.apply(context);

        Mockito.verify(iterator, Mockito.never()).next();
        Mockito.verify(iterator, Mockito.never()).failed();
        Mockito.verify(doiAction, Mockito.never()).apply(Mockito.any(), Mockito.any());
        Mockito.verify(iterator, Mockito.times(1)).hasNext();
    }

    @Test
    public void testSingleDOIIterator() throws Exception {
        DOI doi = Mockito.mock(DOI.class);

        Mockito.when(iterator.hasNext())
               .thenReturn(true)
               .thenReturn(false);

        Mockito.when(iterator.next())
                   .thenReturn(doi)
                   .thenThrow(new RuntimeException());

        doiStrategy.apply(context);

        Mockito.verify(iterator, Mockito.times(1)).next();
        Mockito.verify(iterator, Mockito.times(2)).hasNext();
        Mockito.verify(doiAction, Mockito.times(1)).apply(Mockito.eq(context), Mockito.eq(doi));
        Mockito.verify(iterator, Mockito.never()).failed();
    }


    @Test
    public void testMultipleDOIIterator() throws Exception {
        DOI first = Mockito.mock(DOI.class);
        DOI second = Mockito.mock(DOI.class);

        Mockito.when(iterator.hasNext())
               .thenReturn(true)
               .thenReturn(true)
               .thenReturn(false);

        Mockito.when(iterator.next())
               .thenReturn(first)
               .thenReturn(second)
               .thenThrow(new RuntimeException());

        doiStrategy.apply(context);

        Mockito.verify(iterator, Mockito.times(2)).next();
        Mockito.verify(iterator, Mockito.times(3)).hasNext();
        Mockito.verify(doiAction, Mockito.times(1)).apply(Mockito.eq(context), Mockito.eq(first));
        Mockito.verify(doiAction, Mockito.times(1)).apply(Mockito.eq(context), Mockito.eq(second));
        Mockito.verify(iterator, Mockito.never()).failed();
    }

    @Test
    public void testFailedItemInIterator() throws Exception {
        DOI first = Mockito.mock(DOI.class);
        DOI second = Mockito.mock(DOI.class);

        Mockito.when(iterator.hasNext())
               .thenReturn(true)
               .thenReturn(true)
               .thenReturn(false);

        Mockito.when(iterator.next())
               .thenReturn(first)
               .thenReturn(second)
               .thenThrow(new RuntimeException());

        Mockito.doThrow(new RuntimeException())
               .when(doiAction)
               .apply(Mockito.eq(context), Mockito.eq(second));

        doiStrategy.apply(context);

        Mockito.verify(iterator, Mockito.times(2)).next();
        Mockito.verify(iterator, Mockito.times(3)).hasNext();

        Mockito.verify(doiAction, Mockito.times(1))
               .apply(Mockito.eq(context), Mockito.eq(first));
        Mockito.verify(doiAction, Mockito.times(1))
               .apply(Mockito.eq(context), Mockito.eq(second));

        Mockito.verify(iterator, Mockito.times(1)).failed();
    }


    @Test
    public void testProcessDOIAfterFailure() throws Exception {
        DOI first = Mockito.mock(DOI.class);
        DOI second = Mockito.mock(DOI.class);
        DOI third = Mockito.mock(DOI.class);

        Mockito.when(iterator.hasNext())
               .thenReturn(true)
               .thenReturn(true)
               .thenReturn(true)
               .thenReturn(false);

        Mockito.when(iterator.next())
               .thenReturn(first)
               .thenReturn(second)
               .thenReturn(third)
               .thenThrow(new RuntimeException());

        Mockito.doThrow(new RuntimeException())
               .when(doiAction)
               .apply(Mockito.eq(context), Mockito.eq(first));

        doiStrategy.apply(context);

        Mockito.verify(iterator, Mockito.times(3)).next();
        Mockito.verify(iterator, Mockito.times(4)).hasNext();

        Mockito.verify(doiAction, Mockito.times(1))
               .apply(Mockito.eq(context), Mockito.eq(first));
        Mockito.verify(doiAction, Mockito.times(1))
               .apply(Mockito.eq(context), Mockito.eq(second));
        Mockito.verify(doiAction, Mockito.times(1))
               .apply(Mockito.eq(context), Mockito.eq(third));

        Mockito.verify(iterator, Mockito.times(1)).failed();
    }

}