/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.dqp.webservice.war.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.teiid.designer.dqp.webservice.war.WebArchiveBuilder;
import org.teiid.designer.dqp.webservice.war.WebArchiveBuilderFactory;
import com.metamatrix.core.util.I18nUtil;
import com.metamatrix.modeler.core.workspace.ModelResource;
import com.metamatrix.modeler.dqp.ui.DqpUiPlugin;
import com.metamatrix.modeler.webservice.WebServicePlugin;
import com.metamatrix.ui.internal.util.WidgetFactory;

/**
 * @since 7.1
 */
public class WarDeploymentInfoDialog extends TitleAreaDialog implements InternalModelerWarUiConstants {

    // /////////////////////////////////////////////////////////////////////////////////////////////
    // CONSTANTS
    // /////////////////////////////////////////////////////////////////////////////////////////////
    private static final String I18N_PREFIX = I18nUtil.getPropertyPrefix(WarDeploymentInfoDialog.class);
    private static final String TITLE = getString("title"); //$NON-NLS-1$
    private static final String MESSAGE_TITLE = getString("messageTitle"); //$NON-NLS-1$
    private static final String INITIAL_MESSAGE = getString("initialMessage"); //$NON-NLS-1$
    private static final String OVERWRITE_TARGET_WAR_TITLE = getString("overwriteTargetWar.title"); //$NON-NLS-1$
    private static final String OVERWRITE_TARGET_WAR_MESSAGE = getString("overwriteTargetWar.message"); //$NON-NLS-1$
    private static final String CREATING_WAR_FILE_MESSAGE_ID = "WarDeploymentInfoDialog.creatingWarFileMessage"; //$NON-NLS-1$

    // /////////////////////////////////////////////////////////////////////////////////////////////
    // FIELDS
    // /////////////////////////////////////////////////////////////////////////////////////////////
    private Button okButton;
    private WarDeploymentInfoDataPage warDeploymentInfoDataPage;

    private IFile theVdb;
    private ArrayList<ModelResource> wsModelResourcearrayList;
    private IStatus deploymentStatus;
    private String warFileName;

    private IStatus initialStatus;

    /**
     * @param parent
     * @param title
     * @since 7.1
     */
    public WarDeploymentInfoDialog( Shell parent,
                                    IFile theVdb,
                                    ArrayList<ModelResource> wsModelResources,
                                    IStatus initialStatus ) {
        super(parent); // );
        // this.setShellStyle(getShellStyle());

        ImageDescriptor id = DqpUiPlugin.getDefault().getImageDescriptor(InternalModelerWarUiConstants.WebServicesImages.WAR_FILE_ICON);
        if (id != null) setDefaultImage(id.createImage());

        this.theVdb = theVdb;
        this.wsModelResourcearrayList = wsModelResources;
        this.initialStatus = initialStatus;
        deploymentStatus = new Status(IStatus.OK, DqpUiPlugin.PLUGIN_ID, IStatus.OK, "WAR file created successfully", null);//$NON-NLS-1$
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////
    // INSTANCE METHODS
    // /////////////////////////////////////////////////////////////////////////////////////////////
    public void setOkButtonEnable( boolean isEnable ) {
        if (okButton != null) {
            okButton.setEnabled(isEnable);
        }
    }

    /**
     * @see org.eclipse.jface.window.Window#setShellStyle(int)
     * @since 7.1
     */
    @Override
    protected void setShellStyle( int theNewShellStyle ) {
        super.setShellStyle(theNewShellStyle | SWT.RESIZE);
    }

    @Override
    protected void configureShell( Shell shell ) {
        super.configureShell(shell);
        shell.setText(TITLE);
    }

    @Override
    protected Control createDialogArea( Composite parent ) {
        Composite contents = WidgetFactory.createPanel(parent, SWT.NONE, GridData.FILL_BOTH, 1);
        warDeploymentInfoDataPage = null;
        warDeploymentInfoDataPage = new WarDeploymentInfoDataPage(contents, this, this.theVdb, this.initialStatus);

        ImageDescriptor id = DqpUiPlugin.getDefault().getImageDescriptor(InternalModelerWarUiConstants.WebServicesImages.WAR_FILE_ICON);
        if (id != null) this.setTitleImage(id.createImage());

        this.setTitle(MESSAGE_TITLE);
        this.setMessage(INITIAL_MESSAGE);

        return contents;
    }

    protected static String getString( final String id ) {
        return DqpUiPlugin.UTIL.getString(I18N_PREFIX + id);
    }

    @Override
    protected void createButtonsForButtonBar( Composite parent ) {
        okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);

        loadDefault();
    }

    /**
     * override the method
     * 
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     * @since 7.1
     */
    @Override
    protected void okPressed() {
        // create VDB resource

        final IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run( final IProgressMonitor monitor ) throws InvocationTargetException {
                try {
                    execute(monitor);
                } catch (final Exception err) {
                    throw new InvocationTargetException(err);
                } finally {
                    monitor.done();
                }
            }
        };
        try {
            new ProgressMonitorDialog(getShell()).run(false, true, op);
        } catch (Throwable err) {
            if (err instanceof InvocationTargetException) {
                err = ((InvocationTargetException)err).getTargetException();
            }
            DqpUiPlugin.UTIL.log(err);
        }

    }

    void execute( IProgressMonitor monitor ) {
        try {

            WebArchiveBuilder webArchiveBuilder = WebArchiveBuilderFactory.create();
            monitor.worked(25);
            boolean createWar = true;
            // Check if file already exists, check whether it already exists...
            boolean targetExists = webArchiveBuilder.targetWarFileExists(WarDataserviceModel.getInstance().getProperties());
            if (targetExists) {
                // Ask user whether to proceed by overwriting the existing file
                createWar = MessageDialog.openQuestion(this.getShell(), OVERWRITE_TARGET_WAR_TITLE, OVERWRITE_TARGET_WAR_MESSAGE);

            }
            if (createWar) {
                monitor.beginTask(DqpUiPlugin.UTIL.getString(CREATING_WAR_FILE_MESSAGE_ID, warFileName), 100);
                deploymentStatus = webArchiveBuilder.createWebArchive(WarDataserviceModel.getInstance().getProperties(), monitor);
                // log status
                DqpUiPlugin.UTIL.log(deploymentStatus.getMessage());

                super.okPressed();
            }
        } catch (RuntimeException err) {
            // BusyCursor.endBusy();
            DqpUiPlugin.UTIL.log(err);
            setMessage("Error while generating the WAR file check log for detail message.", InternalModelerWarUiConstants.ERROR); //$NON-NLS-1$
        }
    }

    /**
     * override the method
     * 
     * @see org.eclipse.jface.dialogs.Dialog#cancelPressed()
     * @since 7.1
     */
    @Override
    protected void cancelPressed() {
        super.cancelPressed();
    }

    /**
     * set this method for connector binding editor view
     * 
     * @param boolean bBool
     * @since 7.1
     */
    protected void setForConnectorBindingEditorViewStatus( IStatus status ) {
        deploymentStatus = status;
    }

    /**
     * calling class will use this method to determine if connector binding editor view will be open
     * 
     * @return boolean
     * @since 7.1
     */
    public IStatus getForConnectorBindingEditorViewStatus() {
        return deploymentStatus;
    }

    /**
     * load default setting values for WAR file location, License file location, context name.
     * 
     * @since 7.1
     */
    private void loadDefault() {
        loadWarFileLocationDefault();
        loadContextNameDefault();
        loadHostNameDefault();
        loadPortDefault();
        loadTnsDefault();
        loadJndiNameDefault();

        WarDataserviceModel.getInstance().setVdbFile(theVdb);
        WarDataserviceModel.getInstance().setWsModelResourcearrayList(wsModelResourcearrayList);

        warDeploymentInfoDataPage.loadData();
        warDeploymentInfoDataPage.setWarFileNameInDialog();
    }

    /**
     * set default war file location
     * 
     * @since 7.1
     */
    private void loadWarFileLocationDefault() {
        try {
            String warDir = WebServicePlugin.getDefaultWarFileSaveLocation();
            if (warDir != null && warDir.indexOf("/") >= 0) { //$NON-NLS-1$
                warDir = warDir.replace('/', '\\');
            }

            WarDataserviceModel.getInstance().setWarFilenameDefault(warDir);
        } catch (Throwable theThrowable) {
            DqpUiPlugin.UTIL.log(theThrowable);
        }
    }

    /**
     * set default context name, by default context name is vdb name.
     * 
     * @since 7.1
     */
    private void loadContextNameDefault() {
        try {
            WarDataserviceModel.getInstance().setContextNameDefault("");//$NON-NLS-1$

            if (theVdb == null) {
                WarDataserviceModel.getInstance().setContextNameDefault("");//$NON-NLS-1$ 
                return;
            }

            String name = theVdb.getName().substring(0, theVdb.getName().lastIndexOf("."));//$NON-NLS-1$        
            WarDataserviceModel.getInstance().setContextNameDefault(name);

        } catch (Throwable theThrowable) {
            DqpUiPlugin.UTIL.log(theThrowable);
        }
    }

    /**
     * set default context name, by default host name is "localhost".
     * 
     * @since 7.1
     */
    private void loadHostNameDefault() {
        WarDataserviceModel.getInstance().setHostNameDefault("localhost");//$NON-NLS-1$
    }

    /**
     * set default port, by default port is "8080".
     * 
     * @since 7.1
     */
    private void loadPortDefault() {
        WarDataserviceModel.getInstance().setPortDefault("8080");//$NON-NLS-1$
    }

    /**
     * set default tns, by default tns is "http://teiid.org".
     * 
     * @since 7.1
     */
    private void loadTnsDefault() {
        WarDataserviceModel.getInstance().setTnsDefault("http://teiid.org");//$NON-NLS-1$
    }

    /**
     * set default jndiName, by default the jndiName is "java:teiidvdb".
     * 
     * @since 7.1
     */
    private void loadJndiNameDefault() {
        WarDataserviceModel.getInstance().setJndiNameDefault("java:teiidvdb");//$NON-NLS-1$
    }

    public void setWarFileName( String name ) {
        warFileName = name;
    }

    public String getWarFileName() {
        return warFileName;
    }

}
