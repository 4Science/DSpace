/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.index.adder.IndexAdder;
import org.dspace.discovery.indexobject.IndexableItem;
import org.junit.Before;
import org.junit.Test;

public class SolrServiceFileInfoPluginTest {

    private SolrServiceFileInfoPlugin solrServiceFileInfoPlugin;
    private Context context = mock(Context.class);


    @Before
    public void setUp() {
        solrServiceFileInfoPlugin = new SolrServiceFileInfoPlugin();
    }

    @Test
    public void shouldHandleNPE() {
        IndexableItem indexableItem = mock(IndexableItem.class);

        Item item = mock(Item.class);
        Bitstream bitstream1 = mock(Bitstream.class);
        Bitstream bitstream2 = mock(Bitstream.class);
        Bundle bundle1 = mock(Bundle.class);
        Bundle bundle2 = mock(Bundle.class);

        when(indexableItem.getIndexedObject()).thenReturn(item);
        when(bitstream1.getName()).thenReturn("bitstream1");
        when(bitstream2.getName()).thenReturn("bitstream2");
        when(bundle1.getBitstreams()).thenReturn(List.of(bitstream1));
        when(bundle2.getBitstreams()).thenReturn(List.of(bitstream2));
        when(bundle1.getName()).thenReturn("ORIGINAL");
        when(bundle2.getName()).thenReturn("ORIGINAL");

        when(item.getBundles()).thenReturn(List.of(bundle1, bundle2));

        SolrInputDocument solrInputDocument = new SolrInputDocument();

        SolrInputDocument document = spy(solrInputDocument);

        IndexAdder simpleSolrIndexAdder = mock(IndexAdder.class);
        solrServiceFileInfoPlugin.setSimpleIndexAdder(simpleSolrIndexAdder);

        doThrow(new NullPointerException())
            .when(document)
            .addField("original_bundle_filenames", "bitstream1");

        solrServiceFileInfoPlugin.additionalIndex(context, indexableItem, document);

        verify(simpleSolrIndexAdder, times(2)).add(any(), any(), any());
    }
}