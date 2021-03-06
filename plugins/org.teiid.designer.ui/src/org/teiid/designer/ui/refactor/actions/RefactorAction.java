/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.ui.refactor.actions;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.actions.ActionDelegate;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.teiid.designer.core.ModelerCore;
import org.teiid.designer.core.refactor.RefactorCommand;
import org.teiid.designer.core.refactor.ResourceRefactorCommand;
import org.teiid.designer.core.workspace.ModelUtil;
import org.teiid.designer.ui.UiConstants;
import org.teiid.designer.ui.UiPlugin;
import org.teiid.designer.ui.actions.ModelerActionService;
import org.teiid.designer.ui.common.eventsupport.SelectionUtilities;
import org.teiid.designer.ui.common.util.UiUtil;
import org.teiid.designer.ui.common.widget.ListMessageDialog;
import org.teiid.designer.ui.refactor.RefactorUndoManager;
import org.teiid.designer.ui.viewsupport.ModelLabelProvider;
import org.teiid.designer.ui.viewsupport.ModelUtilities;


/**
 * MoveRefactorAction
 *
 * @since 8.0
 */
public abstract class RefactorAction extends ActionDelegate implements IWorkbenchWindowActionDelegate, IViewActionDelegate {

    private static final String READ_ONLY_TITLE = UiConstants.Util.getString("RefactorAction.readonlyTitle"); //$NON-NLS-1$

    protected ISelection selection;
    protected IResource resSelectedResource;
    protected IAction action;
    protected IWorkbenchWindow window;
    private IStatus status;

    /**
     * @return the Label to use for UNDO
     */
    protected abstract String getUndoLabel();
    
    /**
     * @return <code>true</code> if no unsaved editors in the workspace
     */
    protected boolean doResourceCleanup() {
        return UiUtil.saveDirtyEditors(null, null, true);
    }

    public RefactorUndoManager getRefactorUndoManager() {

        ModelerActionService mas = (ModelerActionService)UiPlugin.getDefault().getActionService(window.getActivePage());
        return mas.getRefactorUndoManager();
    }

    protected void setResult( IStatus status ) {
        this.status = status;
    }

    protected IStatus getStatus() {
        return status;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.eclipse.ui.IViewActionDelegate#init(org.eclipse.ui.IViewPart)
     */
    @Override
    public void init( IViewPart view ) {
        window = view.getSite().getWorkbenchWindow();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
     */
    @Override
    public void init( IWorkbenchWindow iww ) {
        window = iww;
    }

    @Override
    public void selectionChanged( IAction action,
                                  ISelection selection ) {
        super.selectionChanged(action, selection);
        this.action = action;
        this.selection = selection;

        setEnabledState();
    }

    protected void setEnabledState() {
        boolean enable = false;

        if (selection != null && SelectionUtilities.isSingleSelection(selection)
            && SelectionUtilities.isAllIResourceObjects(selection)) {
            IResource resTemp = (IResource)SelectionUtilities.getSelectedIResourceObjects(selection).get(0);

            if (!ModelUtil.isIResourceReadOnly(resTemp) && !(resTemp instanceof IProject)
                && ModelUtilities.isModelProjectResource(resTemp)) {
            	if( ModelUtil.isModelFile(resTemp) || ModelUtil.isXsdFile(resTemp)) {
	                enable = true;
	                resSelectedResource = resTemp;
            	}
            }
        }
        
        if (action.isEnabled() != enable) {
            action.setEnabled(enable);
        }
    }

    protected IProgressMonitor executeCommand( final RefactorCommand command ) {

        WorkspaceModifyOperation operation = new WorkspaceModifyOperation() {
            @Override
            public void execute( final IProgressMonitor monitor ) {
                IStatus status = command.execute(monitor);
                setResult(status);
            }
        };

        IProgressMonitor monitor = new NullProgressMonitor();

        // start the txn
        final boolean started = ModelerCore.startTxn(true, false, getUndoLabel(), this);
        boolean succeeded = false;
        
        try {
        	// run the operation
        	operation.run(monitor);
        	succeeded = true;
        } catch (Exception e) {
        	ModelerCore.Util.log(IStatus.ERROR, e, e.getMessage());
        } finally {
        	if (started) {
        		if (succeeded) {
        			// commit the txn
        			ModelerCore.commitTxn();
        		}
        		else
        			ModelerCore.rollbackTxn();
			 }
        }

        return monitor;
    }

    protected Shell getShell() {
        return UiPlugin.getDefault().getCurrentWorkbenchWindow().getShell();
    }

    /**
     * @param rrCommand
     * @return true if there are no read-only dependents or user has authorized refactor
     */
    protected boolean checkDependentStatus( ResourceRefactorCommand rrCommand,
                                            IResource res ) {
        // make sure we can actually move the file:
        List readOnlys = rrCommand.getReadOnlyDependentResources();
        if (!readOnlys.isEmpty()) {
            String msg = UiConstants.Util.getString("RefactorAction.readonlyResources", res.getName()); //$NON-NLS-1$

            // prompt user to continue. If they do not want to, return, else
            // continue on.
            if (!ListMessageDialog.openWarningQuestion(getShell(),
                                                       READ_ONLY_TITLE,
                                                       null,
                                                       msg,
                                                       readOnlys,
                                                       new ResourceLabelProvider())) {
                return false;
            } // endif -- cancel or not
        } // endif -- anything read only

        return true;
    }

    static class ResourceLabelProvider extends ModelLabelProvider {
        @Override
        public String getText( Object element ) {
            // since we are in a list, not a tree, show full path:
            if (element instanceof IResource) {
                return ((IResource)element).getFullPath().toString();
            } // endif
            return super.getText(element);
        }
    } // endclass ResourceLabelProvider
}
