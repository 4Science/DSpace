/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.Collection;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.factory.WorkflowServiceFactory;

/**
 * Class representing an item going through the workflow process in DSpace
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
@Entity
@Table(name="cwf_workflowitem")
public class XmlWorkflowItem implements WorkflowItem, ReloadableEntity<Integer>, BrowsableDSpaceObject<Integer> {

    @Transient
    public transient Map<String, Object> extraInfo = new HashMap<String, Object>();
    
    @Id
    @Column(name="workflowitem_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE ,generator="cwf_workflowitem_seq")
    @SequenceGenerator(name="cwf_workflowitem_seq", sequenceName="cwf_workflowitem_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private Collection collection;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", unique = true)
    private Item item;

    @Column(name = "multiple_titles")
    private boolean multipleTitles = false;

    @Column(name = "published_before")
    private boolean publishedBefore = false;

    @Column(name = "multiple_files")
    private boolean multipleFiles = false;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService#create(Context, Item, Collection)}
     *
     */
    protected XmlWorkflowItem()
    {

    }

    /**
     * Get the internal ID of this workflow item
     *
     * @return the internal identifier
     */
    @Override
    public Integer getID()
    {
        return id;
    }


    @Override
    public Collection getCollection(){
        return this.collection;
    }

    public void setCollection(Collection collection){
        this.collection = collection;
    }

    @Override
    public Item getItem()
    {
        return item;
    }

    public void setItem(Item item){
        this.item = item;
    }

    @Override
    public EPerson getSubmitter() throws SQLException
    {
        return item.getSubmitter();
    }

    @Override
    public boolean hasMultipleFiles()
    {
        return multipleFiles;
    }

    @Override
    public void setMultipleFiles(boolean b)
    {
        this.multipleFiles = b;
    }

    @Override
    public boolean hasMultipleTitles()
    {
        return this.multipleTitles;
    }

    @Override
    public void setMultipleTitles(boolean b)
    {
        this.multipleTitles = b;
    }

    @Override
    public boolean isPublishedBefore()
    {
        return this.publishedBefore;
    }

    @Override
    public void setPublishedBefore(boolean b)
    {
        this.publishedBefore = b;
    }
    
	@Override
	public void update() throws SQLException, AuthorizeException {
		
		Context context = null; 
		try {
			context = new Context();
			WorkflowServiceFactory.getInstance().getWorkflowItemService().update(context, this);
		}
		finally {
			if(context!=null && context.isValid()) {
				context.abort();
			}
		}
	}

	@Override
	public int getState() {
		// FIXME
		return 0;
	}
    @Override
    public String getHandle() {
        return null;
    }

    @Override
    public List<String> getMetadataValue(String mdString) {
        return item.getMetadataValue(mdString);
    }

    @Override
    public List<IMetadataValue> getMetadataValueInDCFormat(String mdString) {
        return item.getMetadataValueInDCFormat(mdString);
    }

    @Override
    public String getTypeText() {
        return "workflowitem";
    }

    @Override
    public int getType() {
        return Constants.WORKFLOWITEM;
    }

    @Override
    public boolean isWithdrawn() {
        return false;
    }

    @Override
    public Map<String, Object> getExtraInfo() {
        return extraInfo;
    }

    @Override
    public boolean isArchived() {
        return false;
    }

    @Override
    public List<IMetadataValue> getMetadata(String schema, String element, String qualifier, String lang) {
        return item.getMetadata(schema, element, qualifier, lang);
    }

    @Override
    public List<IMetadataValue> getMetadata() {
        return item.getMetadata();
    }

    @Override
    public String getMetadata(String field) {
        return item.getMetadata(field);
    }

    @Override
    public boolean isDiscoverable() {
        return false;
    }

    @Override
    public String getName() {
        return item.getName();
    }

    @Override
    public String findHandle(Context context) throws SQLException {
        return null;
    }

    @Override
    public boolean haveHierarchy() {
        return false;
    }

    @Override
    public BrowsableDSpaceObject getParentObject() {
        return getItem();
    }

    @Override
    public String getMetadataFirstValue(String schema, String element, String qualifier, String language) {
        return item.getMetadataFirstValue(schema, element, qualifier, language);
    }

    @Override
    public Date getLastModified() {
        return item.getLastModified();
    }

}
