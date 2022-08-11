/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.mapper;

import java.util.Map;

import org.dspace.content.Item;

/*
 * @author Jurgen Mamani
 */
public interface ItemControlledVocabularyMapper {

    public Map<String, String> buildExtraValues(Item item);

}
