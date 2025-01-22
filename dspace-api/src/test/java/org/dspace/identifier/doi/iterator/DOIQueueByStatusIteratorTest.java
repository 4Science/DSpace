/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.iterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.dspace.AbstractUnitTest;
import org.dspace.identifier.DOI;
import org.dspace.identifier.service.DOIService;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class DOIQueueByStatusIteratorTest extends AbstractUnitTest {


    public static final List<Integer> DEFAULT_STATUS = List.of();
    private static final int DEFAULT_LIMIT = 10;

    @Mock()
    DOIService doiService;

    DOIQueueByStatusIterator iterator;

    @Before
    public void setUp() throws Exception {
        this.iterator = new DOIQueueByStatusIterator(context, doiService, DEFAULT_STATUS, -1, DEFAULT_LIMIT);
    }


    @Test
    public void testEmptyDOIQueue() throws Exception {
        Mockito.when(
            doiService.getDOIsByStatus(Mockito.eq(context), Mockito.anyList(), Mockito.anyInt(),
                Mockito.eq(DEFAULT_LIMIT)
            )
        ).thenReturn(List.of());
        DOIQueueByStatusIterator spy = Mockito.spy(this.iterator);

        assertFalse("The iterator shouldn't have any element!", spy.hasNext());

        Mockito.verify(spy, Mockito.times(2)).refreshIterator();

        assertThrows(NoSuchElementException.class, spy::next);
    }

    @Test
    public void testSingleDOIQueue() throws Exception {
        DOI doiMock = Mockito.mock(DOI.class);
        Mockito.when(doiService.getDOIsByStatus(Mockito.eq(context), Mockito.eq(DEFAULT_STATUS), Mockito.anyInt(),
                   Mockito.eq(DEFAULT_LIMIT)
               ))
               .thenReturn(List.of(doiMock));
        DOIQueueByStatusIterator spy = Mockito.spy(this.iterator);

        assertTrue("The iterator should have one element!", spy.hasNext());

        Mockito.verify(spy, Mockito.times(1)).refreshIterator();

        assertThat(spy.next(), CoreMatchers.is(doiMock));

        // Return an empty list since we are simulating a queue of 1 element
        Mockito.when(doiService.getDOIsByStatus(Mockito.eq(context), Mockito.eq(DEFAULT_STATUS), Mockito.anyInt(),
                   Mockito.eq(DEFAULT_LIMIT)
               ))
               .thenReturn(List.of());

        assertFalse("The iterator shouldn't have any element!", spy.hasNext());
        assertThrows(NoSuchElementException.class, spy::next);

        Mockito.verify(spy, Mockito.never()).failed();
    }


    @Test
    public void testMultipleDOIInQueue() throws Exception {
        DOI doiMock1 = Mockito.mock(DOI.class);
        DOI doiMock2 = Mockito.mock(DOI.class);
        DOI doiMock3 = Mockito.mock(DOI.class);
        Mockito.when(doiService.getDOIsByStatus(Mockito.eq(context), Mockito.eq(DEFAULT_STATUS), Mockito.anyInt(),
                   Mockito.eq(DEFAULT_LIMIT)
               ))
               .thenReturn(List.of(doiMock1, doiMock2, doiMock3));
        DOIQueueByStatusIterator spy = Mockito.spy(this.iterator);

        assertTrue("The iterator should have elements!", spy.hasNext());
        Mockito.verify(spy, Mockito.times(1)).refreshIterator();
        assertThat(spy.next(), CoreMatchers.is(doiMock1));

        assertTrue("The iterator should have three elements!", spy.hasNext());
        assertThat(spy.next(), CoreMatchers.is(doiMock2));

        assertTrue("The iterator should have three elements!", spy.hasNext());
        assertThat(spy.next(), CoreMatchers.is(doiMock3));

        // fetching other items from the queue
        Mockito.when(
                   doiService.getDOIsByStatus(Mockito.eq(context), Mockito.any(), Mockito.anyInt(),
                       Mockito.eq(DEFAULT_LIMIT)
                   ))
               .thenReturn(List.of());

        // empty queue
        assertFalse("The iterator should have three elements!", spy.hasNext());
        Mockito.verify(spy, Mockito.times(2)).refreshIterator();
        Mockito.verify(spy, Mockito.never()).failed();
    }

    @Test
    public void testFailingDOIs() throws Exception {
        DOI doiMock1 = Mockito.mock(DOI.class);
        DOI doiMock2 = Mockito.mock(DOI.class);
        DOI doiMock3 = Mockito.mock(DOI.class);
        List<DOI> doiList = Mockito.spy(List.of(doiMock1, doiMock2, doiMock3));
        Mockito.when(doiService.getDOIsByStatus(Mockito.eq(context), Mockito.eq(DEFAULT_STATUS), Mockito.anyInt(),
                   Mockito.eq(DEFAULT_LIMIT)
               ))
               .thenReturn(doiList);
        DOIQueueByStatusIterator spy = Mockito.spy(this.iterator);

        assertTrue("The iterator should have elements!", spy.hasNext());

        Mockito.verify(spy, Mockito.times(1)).refreshIterator();

        assertThat(spy.next(), CoreMatchers.equalTo(doiMock1));
        assertThat(spy.next(), CoreMatchers.equalTo(doiMock2));
        assertThat(spy.next(), CoreMatchers.equalTo(doiMock3));

        Mockito.when(doiService.getDOIsByStatus(Mockito.eq(context), Mockito.eq(DEFAULT_STATUS), Mockito.anyInt(),
                   Mockito.eq(DEFAULT_LIMIT)
               ))
               .thenThrow(new SQLException());

        assertFalse("Shouldn't have any element!", spy.hasNext());
        assertThrows(NoSuchElementException.class, spy::next);
    }

    @Test
    public void skipsFailedDOIs() throws Exception {
        Iterator iterator = Mockito.mock(Iterator.class);
        List doiList = Mockito.mock(List.class);
        DOI doiMock1 = Mockito.mock(DOI.class);
        DOI doiMock3 = Mockito.mock(DOI.class);

        Mockito.when(
                   doiService.getDOIsByStatus(Mockito.eq(context), Mockito.eq(DEFAULT_STATUS), Mockito.anyInt(),
                       Mockito.eq(DEFAULT_LIMIT)
                   ))
               .thenReturn(doiList);

        Mockito.when(doiList.iterator()).thenReturn(iterator);

        Mockito.when(iterator.hasNext()).thenReturn(true);
        Mockito.when(iterator.next())
               .thenReturn(doiMock1)
               .thenThrow(new NoSuchElementException())
               .thenReturn(doiMock3);

        DOIQueueByStatusIterator spy = Mockito.spy(this.iterator);

        assertTrue("The iterator should have elements!", spy.hasNext());
        Mockito.verify(spy, Mockito.times(1)).refreshIterator();

        assertThat(spy.next(), CoreMatchers.equalTo(doiMock1));

        Mockito.verify(spy, Mockito.never()).failed();
        assertThat(spy.next(), CoreMatchers.equalTo(doiMock3));
        Mockito.verify(spy, Mockito.times(1)).failed();

        // when iterators ends
        Mockito.when(iterator.hasNext()).thenReturn(false);
        // fetch more items skipping failed ones (limit = 1)
        Mockito.verify(spy, Mockito.times(1)).refreshIterator();

        List<DOI> emptyDOIs = Mockito.mock(List.class);
        Iterator emptyIterator = Mockito.mock(Iterator.class);

        Mockito.when(emptyDOIs.iterator()).thenReturn(emptyIterator);
        Mockito.when(iterator.hasNext()).thenReturn(false);
        Mockito.when(
            doiService
                .getDOIsByStatus(
                    Mockito.eq(context), Mockito.eq(DEFAULT_STATUS), Mockito.eq(1), Mockito.eq(DEFAULT_LIMIT))
        ).thenReturn(emptyDOIs);

        // check that it's empty
        assertThrows(NoSuchElementException.class, spy::next);
        assertThat(spy.iterator, CoreMatchers.equalTo(emptyIterator));
        Mockito.verify(spy, Mockito.times(2)).refreshIterator();
    }

    @Test
    public void skipsNullDOI() throws Exception {
        Iterator iterator = Mockito.mock(Iterator.class);
        List doiList = Mockito.mock(List.class);
        DOI doiMock1 = Mockito.mock(DOI.class);
        DOI doiMock3 = Mockito.mock(DOI.class);

        Mockito.when(
                   doiService.getDOIsByStatus(
                       Mockito.eq(context), Mockito.eq(DEFAULT_STATUS), Mockito.eq(-1), Mockito.eq(DEFAULT_LIMIT)
                   ))
               .thenReturn(doiList);

        Mockito.when(doiList.iterator()).thenReturn(iterator);

        Mockito.when(iterator.hasNext()).thenReturn(true);
        Mockito.when(iterator.next())
               .thenReturn(doiMock1)
               .thenReturn(null)
               .thenReturn(doiMock3);

        DOIQueueByStatusIterator spy = Mockito.spy(this.iterator);

        assertTrue("The iterator should have elements!", spy.hasNext());
        Mockito.verify(spy, Mockito.times(1)).refreshIterator();

        assertThat(spy.next(), CoreMatchers.equalTo(doiMock1));

        Mockito.verify(spy, Mockito.never()).failed();
        assertThat(spy.next(), CoreMatchers.equalTo(doiMock3));
        Mockito.verify(spy, Mockito.times(1)).failed();
        assertThat(spy.next(), CoreMatchers.equalTo(doiMock3));

        // when iterators ends
        Mockito.when(iterator.hasNext()).thenReturn(false);
        // fetch more items skipping failed ones (limit = 1)
        Mockito.verify(spy, Mockito.times(1)).refreshIterator();

        List<DOI> emptyDOIs = Mockito.mock(List.class);
        Iterator emptyIterator = Mockito.mock(Iterator.class);

        Mockito.when(emptyDOIs.iterator()).thenReturn(emptyIterator);
        Mockito.when(iterator.hasNext()).thenReturn(false);
        Mockito.when(
            doiService
                .getDOIsByStatus(
                    Mockito.eq(context), Mockito.eq(DEFAULT_STATUS), Mockito.eq(1), Mockito.eq(DEFAULT_LIMIT))
        ).thenReturn(emptyDOIs);

        // check that it's empty
        assertThrows(NoSuchElementException.class, spy::next);
        assertThat(spy.iterator, CoreMatchers.equalTo(emptyIterator));
        Mockito.verify(spy, Mockito.times(2)).refreshIterator();
    }


}