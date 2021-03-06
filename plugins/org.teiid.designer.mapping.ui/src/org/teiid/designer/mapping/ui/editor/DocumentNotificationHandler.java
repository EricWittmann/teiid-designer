/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.teiid.designer.mapping.ui.editor;

import java.util.Collection;
import java.util.Iterator;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.mapping.Mapping;
import org.eclipse.emf.mapping.MappingHelper;
import org.teiid.designer.core.ModelerCore;
import org.teiid.designer.core.notification.util.IgnorableNotificationSource;
import org.teiid.designer.core.notification.util.NotificationUtilities;
import org.teiid.designer.core.transaction.SourcedNotification;
import org.teiid.designer.core.workspace.ModelResource;
import org.teiid.designer.diagram.ui.model.DiagramModelNode;
import org.teiid.designer.diagram.ui.util.DiagramUiUtilities;
import org.teiid.designer.mapping.factory.IMappableTree;
import org.teiid.designer.metamodels.diagram.Diagram;
import org.teiid.designer.metamodels.transformation.MappingClass;
import org.teiid.designer.metamodels.transformation.MappingClassColumn;
import org.teiid.designer.metamodels.transformation.MappingClassObject;
import org.teiid.designer.metamodels.transformation.MappingClassSet;
import org.teiid.designer.metamodels.transformation.TransformationMappingRoot;
import org.teiid.designer.ui.viewsupport.ModelUtilities;


/**
 * DocumentNotificationHandler
 *
 * @since 8.0
 */
public class DocumentNotificationHandler {
    private static final String[] notificationTypes = {"CREATE", //$NON-NLS-1$
        "SET", //$NON-NLS-1$
        "UNSET", //$NON-NLS-1$
        "ADD", //$NON-NLS-1$
        "REMOVE", //$NON-NLS-1$
        "ADD_MANY", //$NON-NLS-1$
        "REMOVE_MANY", //$NON-NLS-1$
        "MOVE", //$NON-NLS-1$
        "REMOVING_ADAPTER", //$NON-NLS-1$
        "RESOLVE" //$NON-NLS-1$
    };

    // /////////////////////////////////////////////////////////////////////////////////////////////
    // FIELDS
    // /////////////////////////////////////////////////////////////////////////////////////////////
    private MappingDiagramController mappingDiagramController;
    private IMappableTree mappableTree;

    // private boolean logDebug = false;

    /**
     * Construct an instance of DocumentNotificationHandler.
     */
    public DocumentNotificationHandler( MappingDiagramController controller,
                                        IMappableTree theTree ) {
        super();
        mappingDiagramController = controller;
        mappableTree = theTree;
    }

    public void handleNotification( Notification notification ) {
        // logDebug = false;
        boolean somethingChanged = false;
        boolean ignoreNotification = (notification instanceof SourcedNotification)
                                     && ((SourcedNotification)notification).getSource() instanceof IgnorableNotificationSource;

        if (!ignoreNotification) {
            if (notification instanceof SourcedNotification) {
                if (((SourcedNotification)notification).getSource() != mappingDiagramController) {
                    Collection notifications = ((SourcedNotification)notification).getNotifications();
                    Iterator iter = notifications.iterator();
                    boolean singleChange = false;
                    while (iter.hasNext()) {
                        singleChange = handleSingleNotification((Notification)iter.next());
                        if (!somethingChanged && singleChange) somethingChanged = true;
                    }
                }
            } else {
                somethingChanged = handleSingleNotification(notification);
            }
        }

        if (somethingChanged && shouldRefreshDiagram(notification)) {
            mappingDiagramController.refresh(true);
        }
    }

    private boolean shouldHandleMappingClassSet( MappingClassSet theMappingClassSet ) {
        return theMappingClassSet.getTarget().equals(mappableTree.getTreeRoot());
    }

    private boolean isMappingObject( EObject eObject ) {
        boolean result = false;
        if (eObject instanceof Mapping || eObject instanceof MappingClass || eObject instanceof MappingClassColumn
            || eObject instanceof MappingClassSet || eObject instanceof MappingHelper) result = true;

        return result;
    }

    // should not be called with SourcedNotification
    public boolean shouldHandleNotification( Notification notification ) {
        boolean result = false;
        boolean isDiagramObject = false;

        if (NotificationUtilities.isEObjectNotifier(notification)) {

            EObject notifier = (EObject)ModelerCore.getModelEditor().getChangedObject(notification);

            isDiagramObject = DiagramUiUtilities.isDiagramObject(notifier);

            if (!isDiagramObject) {
                if (mappableTree.isAncestorOf(mappableTree.getTreeRoot(), notifier)) {
                    // node is ancestor of tree root
                    result = true;
                } else {
                    // if node or ancestor is MappingClassSet whose target is tree root
                    EObject parent = notifier.eContainer();

                    if (notifier instanceof MappingClassSet) {
                        result = shouldHandleMappingClassSet((MappingClassSet)notifier);
                    } else if (notifier instanceof MappingClassObject && parent != null && parent instanceof MappingClassSet) {
                        result = shouldHandleMappingClassSet((MappingClassSet)notifier.eContainer());
                    } else if (notifier instanceof MappingClassColumn) {
                        result = true;
                    } else if (parent instanceof MappingClassObject && parent.eContainer() != null) {
                        result = shouldHandleMappingClassSet((MappingClassSet)parent.eContainer());
                    } else if (parent instanceof Mapping) {
                        result = true;
                    }
                }
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // private helper methods.
    // ------------------------------------------------------------------

    // should not be called with SourcedNotification
    private boolean handleSingleNotification( Notification notification ) {
        boolean result = false;
        if (shouldHandleNotification(notification)) {
            if (NotificationUtilities.isAdded(notification)) {
                result = performAdd(notification);
            } else if (NotificationUtilities.isRemoved(notification)) {
                result = performRemove(notification);
            } else if (NotificationUtilities.isChanged(notification)) {
                result = performChange(notification);
            }
        }
        return result;
    }

    private boolean performAdd( Notification notification ) {
        boolean somethingChanged = false;
        Object targetObject = ModelerCore.getModelEditor().getChangedObject(notification);

        if (targetObject != null && targetObject instanceof EObject) {
            // we know that the object is not a child of a model resource !!!!!

            if ((targetObject instanceof MappingClassSet) && shouldHandleMappingClassSet((MappingClassSet)targetObject)) {
                somethingChanged = true;
            } else {
                mappingDiagramController.getDocumentTreeController().getViewer().refresh(targetObject, true);
                somethingChanged = true;
            }

        }
        return somethingChanged;
    }

    private boolean performRemove( Notification notification ) {
        boolean somethingChanged = false;
        Object targetObject = ModelerCore.getModelEditor().getChangedObject(notification);

        if (targetObject != null && targetObject instanceof EObject) {
            // we know that the object is not a child of a model resource !!!!!
            if ((targetObject instanceof MappingClassSet) && shouldHandleMappingClassSet((MappingClassSet)targetObject)) {
                somethingChanged = true;
            } else if (targetObject instanceof Mapping) {
                somethingChanged = true;
            } else if ((targetObject instanceof MappingClass) || (targetObject instanceof MappingClassColumn)) {
                somethingChanged = true;
            } else {
                mappingDiagramController.getDocumentTreeController().getViewer().refresh(targetObject, true);
                somethingChanged = true;
            }
        }
        return somethingChanged;
    }

    private boolean performChange( Notification notification ) {
        boolean somethingChanged = false;
        Object targetObject = ModelerCore.getModelEditor().getChangedObject(notification);

        if (targetObject != null && targetObject instanceof EObject) {
            if ((targetObject instanceof MappingClassSet) && shouldHandleMappingClassSet((MappingClassSet)targetObject)) {
                somethingChanged = true;
            } else if (isMappingObject((EObject)targetObject)) {
                somethingChanged = true;
            } else {
                mappingDiagramController.getDocumentTreeController().getViewer().refresh(targetObject, true);
                somethingChanged = true;
            }
        }
        return somethingChanged;
    }

    private boolean shouldRefreshDiagram( Notification notification ) {
        boolean shouldRefresh = false;

        ModelResource documentMR = mappingDiagramController.getCurrentModelResource();
        if (documentMR != null) {
            if (notification instanceof SourcedNotification) {
                Collection notifications = ((SourcedNotification)notification).getNotifications();
                Iterator iter = notifications.iterator();
                Notification nextNotification = null;

                while (iter.hasNext() && !shouldRefresh) {
                    nextNotification = (Notification)iter.next();
                    Object targetObject = ModelerCore.getModelEditor().getChangedObject(nextNotification);

                    if (targetObject != null && targetObject instanceof EObject
                        && !DiagramUiUtilities.isDiagramObject((EObject)targetObject)) {
                        // Check here if the targetObject and document have the same resource, then set to TRUE;
                        ModelResource mr = ModelUtilities.getModelResourceForModelObject((EObject)targetObject);
                        if (mr != null && mr.equals(documentMR)) {
                            // make sure this didn't come from the current editor:
                            SourcedNotification sn = (SourcedNotification)notification;
                            Object src = sn.getSource();

                            // check source:
                            if (src instanceof DiagramModelNode) {
                                DiagramModelNode dmn = (DiagramModelNode)src;
                                Diagram diag = findDiagram(dmn);

                                if (mappingDiagramController.getCurrentDiagram().equals(diag)) {
                                    shouldRefresh = false;
                                } // endif -- same diagram

                            } else if (targetObject instanceof TransformationMappingRoot) {
                                // src is not a DiagramModelNode, make sure we want to process this mappingClassSet:
                                MappingClassSet mapSet = findMappingClassSet(((TransformationMappingRoot)targetObject).getTarget());
                                shouldRefresh = shouldHandleMappingClassSet(mapSet);

                            } else if (targetObject instanceof MappingClassObject) {
                                // src is not a DiagramModelNode, make sure we want to process this mappingClassSet:
                                MappingClassSet mapSet = findMappingClassSet(targetObject);
                                shouldRefresh = shouldHandleMappingClassSet(mapSet);

                            } else {
                                shouldRefresh = true;

                            } // endif -- src is DiagramModelNode
                        } // endif -- modelResource the same
                    } // endif -- target is eobject and not diagram object
                } // endwhile -- notifications
            } else { // SINGLE NOTIFICATION
                Object targetObject = ModelerCore.getModelEditor().getChangedObject(notification);
                if (targetObject != null && targetObject instanceof EObject
                    && !DiagramUiUtilities.isDiagramObject((EObject)targetObject)) {
                    // Check here if the targetObject and document have the same resource, then set to TRUE;
                    ModelResource mr = ModelUtilities.getModelResourceForModelObject((EObject)targetObject);
                    if (mr != null && mr.equals(documentMR)) {
                        shouldRefresh = true;
                    }
                }
            }
        }

        return shouldRefresh;
    }

    private MappingClassSet findMappingClassSet( Object targetObject ) {
        MappingClassSet result = null;

        if (targetObject instanceof EObject) {
            EObject parent = ((EObject)targetObject).eContainer();

            if (targetObject instanceof MappingClassSet) {
                result = (MappingClassSet)targetObject;
            } else if (parent instanceof MappingClassSet) {
                result = (MappingClassSet)parent;
            } else {
                result = findMappingClassSet(parent);
            } // endif -- instance check
        } // endif

        return result;
    }

    public static Diagram findDiagram( DiagramModelNode dmn ) {
        DiagramModelNode nextNode = dmn;
        Diagram diag = nextNode.getDiagram();

        while (diag == null) {
            nextNode = nextNode.getParent();
            diag = nextNode.getDiagram();
        } // endwhile

        return diag;
    }

    /**
     * Gets a string representation of the properties of the given <code>Notification</code>.
     * 
     * @param theNotification the notification being processed
     * @return the string representation
     */
    public static String paramString2( Notification theNotification ) {

        if (theNotification.getEventType() == Notification.ADD) {
            return new StringBuffer().append("\n Notification:  TYPE =").append(notificationTypes[theNotification.getEventType()]) //$NON-NLS-1$
            .append("\n    NOTIFIER =").append(NotificationUtilities.getEObject(theNotification)) //$NON-NLS-1$
            .append("\n    getAddedChildren=").append(getAddedChildrenPrintString(theNotification)) //$NON-NLS-1$
            .toString();
        } else if (theNotification.getEventType() == Notification.REMOVE) {
            return new StringBuffer().append("\n Notification:  TYPE =").append(notificationTypes[theNotification.getEventType()]) //$NON-NLS-1$
            .append("\n    NOTIFIER =").append(NotificationUtilities.getEObject(theNotification)) //$NON-NLS-1$
            .append("\n    getRemovedChildren=").append(getRemovedChildrenPrintString(theNotification)) //$NON-NLS-1$
            .toString();
        } else if (theNotification.getEventType() == Notification.SET) {
            return new StringBuffer().append("\n Notification:  TYPE =").append(notificationTypes[theNotification.getEventType()]) //$NON-NLS-1$
            .append("\n    NOTIFIER =").append(NotificationUtilities.getEObject(theNotification)) //$NON-NLS-1$
            .toString();
        } else {
            return NotificationUtilities.paramString(theNotification);
        }
    }

    private static String getAddedChildrenPrintString( Notification notification ) {
        if (NotificationUtilities.getAddedChildren(notification) == null
            || NotificationUtilities.getAddedChildren(notification).length == 0) return "EMPTY"; //$NON-NLS-1$

        StringBuffer returnString = new StringBuffer().append(" "); //$NON-NLS-1$

        for (int i = 0; i < NotificationUtilities.getAddedChildren(notification).length; i++) {
            returnString.append("\n     child =").append(NotificationUtilities.getAddedChildren(notification)[i]); //$NON-NLS-1$
        }

        return returnString.toString();
    }

    private static String getRemovedChildrenPrintString( Notification notification ) {
        if (NotificationUtilities.getRemovedChildren(notification) == null
            || NotificationUtilities.getRemovedChildren(notification).length == 0) return "EMPTY"; //$NON-NLS-1$

        StringBuffer returnString = new StringBuffer().append(" "); //$NON-NLS-1$

        for (int i = 0; i < NotificationUtilities.getRemovedChildren(notification).length; i++) {
            returnString.append("\n     child =").append(NotificationUtilities.getRemovedChildren(notification)[i]); //$NON-NLS-1$
        }

        return returnString.toString();
    }
}
