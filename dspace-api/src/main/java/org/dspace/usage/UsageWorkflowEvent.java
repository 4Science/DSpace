/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.usage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.workflow.WorkflowItem;

/**
 * Extends the standard usage event to contain workflow information
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class UsageWorkflowEvent extends UsageEvent {

    private static final long serialVersionUID = 8325691533696305470L;

    private String workflow;

    private String currentWorkflowStep;

    private String previousWorkflowStep;

    private String currentWorkflowAction;

    private String previousWorkflowAction;

    private List<DSpaceObject> owners = new ArrayList<>();

    private Collection scope;

    private EPerson actor;

    private WorkflowItem workflowItem;

    private boolean previousActionRequiresUI;

    public UsageWorkflowEvent(Context context, WorkflowItem workflowItem) {
        super(Action.WORKFLOW, null, context, workflowItem.getItem());
        this.workflowItem = workflowItem;
        this.scope = workflowItem.getCollection();
    }

    public String getCurrentWorkflowStep() {
        return currentWorkflowStep;
    }

    public void setCurrentWorkflowStep(String currentWorkflowStep) {
        this.currentWorkflowStep = currentWorkflowStep;
    }

    public String getWorkflow() {
        return workflow;
    }

    public void setWorkflow(String workflow) {
        this.workflow = workflow;
    }

    public String getPreviousWorkflowStep() {
        return previousWorkflowStep;
    }

    public void setPreviousWorkflowStep(String previousWorkflowStep) {
        this.previousWorkflowStep = previousWorkflowStep;
    }

    public String getCurrentWorkflowAction() {
        return currentWorkflowAction;
    }

    public void setCurrentWorkflowAction(String currentWorkflowAction) {
        this.currentWorkflowAction = currentWorkflowAction;
    }

    public String getPreviousWorkflowAction() {
        return previousWorkflowAction;
    }

    public void setPreviousWorkflowAction(String previousWorkflowAction) {
        this.previousWorkflowAction = previousWorkflowAction;
    }

    public EPerson getActor() {
        return actor;
    }

    public void setActor(EPerson actor) {
        this.actor = actor;
    }

    public boolean isPreviousActionRequiresUI() {
        return previousActionRequiresUI;
    }

    public void setPreviousActionRequiresUI(boolean previousActionRequiresUI) {
        this.previousActionRequiresUI = previousActionRequiresUI;
    }

    public Collection getScope() {
        return scope;
    }

    public void setScope(Collection scope) {
        this.scope = scope;
    }

    public WorkflowItem getWorkflowItem() {
        return workflowItem;
    }

    public void setWorkflowItem(WorkflowItem workflowItem) {
        this.workflowItem = workflowItem;
    }

    public List<DSpaceObject> getOwners() {
        return owners;
    }

    public List<EPerson> getEPersonOwners() {
        return owners.stream()
            .filter(owner -> owner.getType() == Constants.EPERSON)
            .map(owner -> (EPerson) owner)
            .collect(Collectors.toList());
    }

    public List<Group> getGroupOwners() {
        return owners.stream()
            .filter(owner -> owner.getType() == Constants.GROUP)
            .map(owner -> (Group) owner)
            .collect(Collectors.toList());
    }

    public void setOwners(List<DSpaceObject> owners) {
        this.owners = owners;
    }

}
