/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rdf;

public class RDFCris {
/*This class contains methods for processing CRIS Items*/
	/* experimental use:
	 * This class converts CRIS Entity-Objects into RDF
	 * problem: RDF-plugin is based on DSpaceObjects -> can't modify 
	 * Links to CRIS-Entities are saved in metadata.value.authority (eg rp00002) of Items
	 * Possiblities:
	 * 1. Modify methods 
	 * 2. Convert Entity into DSpace-Object-Type
	 * -> No Access to cris-Files ;(
	 *  import org.dspace.app.cris.*;
	 * @author Florian Gantner
	 * */
	
	//ACRISObject has Metadatum as well.
	//Method to Output all CRIS-Entities by Type
	//Test rpo00002 rp00003 rp00001 
	
	//Method to find CRIS-Entity-Type by Id (e.g. rp00002)
	
	
	//Method to create full URI of CRIS-Entity given by cris-ID
	//Use Case: Value is saved in AuthorityValue of Metadata
	public static String getURIByCRISID(String cris_id) {
		//Stub Method, replace later with CRIS-Funcionality to handle this Code
		//f.e. ACrisObject.getPublicPath() -> returns rp
		String uri = "";
		if(cris_id.startsWith("rp")) {
			uri = RDFConfiguration.getDSpaceRDFModuleURI() + "/researcher/" + cris_id;
			return uri;
		}
		else if(cris_id.startsWith("ou")) {
			uri = RDFConfiguration.getDSpaceRDFModuleURI() + "/orgunit/" + cris_id;
			return uri;
		}
		else if(cris_id.startsWith("pr")) {
			uri = RDFConfiguration.getDSpaceRDFModuleURI() + "/project/" + cris_id;
		return uri;
		}else {
			return null;	
		}
			
	}
}
