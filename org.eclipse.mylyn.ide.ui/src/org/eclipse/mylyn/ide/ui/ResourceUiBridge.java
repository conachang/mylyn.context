/*******************************************************************************
 * Copyright (c) 2004 - 2005 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/
/*
 * Created on Apr 20, 2005
  */
package org.eclipse.mylar.ide.ui;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.mylar.core.IMylarElement;
import org.eclipse.mylar.core.IMylarStructureBridge;
import org.eclipse.mylar.core.MylarPlugin;
import org.eclipse.mylar.core.util.MylarStatusHandler;
import org.eclipse.mylar.ide.MylarIdePlugin;
import org.eclipse.mylar.ui.IMylarUiBridge;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.texteditor.AbstractTextEditor;

/**
 * @author Mik Kersten
 */
public class ResourceUiBridge implements IMylarUiBridge {
 
    public void open(IMylarElement node) {
        IMylarStructureBridge adapter = MylarPlugin.getDefault().getStructureBridge(node.getContentType());
        if (adapter == null) return;
        IResource resource = (IResource)adapter.getObjectForHandle(node.getHandleIdentifier());
        if (resource instanceof IFile && resource.exists()) {
	        internalOpenEditor((IFile)resource, true);
        }
    }

	public void setContextCapturePaused(boolean paused) {
		// TODO Auto-generated method stub
		
	}
    
	public void restoreEditor(IMylarElement document) {
		IResource resource = MylarIdePlugin.getDefault().getResourceForElement(document);
		if (resource instanceof IFile) {
			internalOpenEditor((IFile)resource, false);
		}
	}
	
	private void internalOpenEditor(IFile file, boolean activate) {
		try {
			IWorkbenchPage activePage = Workbench.getInstance().getActiveWorkbenchWindow().getActivePage();
			IEditorDescriptor editorDescriptor = IDE.getDefaultEditor(file);
			if (editorDescriptor != null && editorDescriptor.isInternal()) {
				IDE.openEditor(activePage, (IFile)file, activate);
			} 
		} catch (PartInitException e) {
			MylarStatusHandler.fail(e, "failed to open editor for: " + file, false);
		}
	}
	
    public void close(IMylarElement node) {
        IWorkbenchPage page = Workbench.getInstance().getActiveWorkbenchWindow().getActivePage();
        if (page != null) {
            IEditorReference[] references = page.getEditorReferences();
            for (int i = 0; i < references.length; i++) {
                IEditorPart part = references[i].getEditor(false);
                if (part != null) {
                    if (part instanceof AbstractTextEditor) {
                       ((AbstractTextEditor)part).close(false);
                    } else if (part instanceof FormEditor) {
                        ((FormEditor)part).close(false);
                    }    
                }
            }
        }
    }

    public boolean acceptsEditor(IEditorPart editorPart) {
        return false;
    }

    public List<TreeViewer> getContentOutlineViewers(IEditorPart editor) {
        return Collections.emptyList();
    }

    public void refreshOutline(Object element, boolean updateLabels, boolean setSelection) {
    	// no outline to refresh
        
    }

	public Object getObjectForTextSelection(TextSelection selection, IEditorPart editor) {
		return null;
	}
}
