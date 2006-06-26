/*******************************************************************************
 * Copyright (c) 2004 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.internal.ide.team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.mylar.internal.ide.MylarIdePlugin;
import org.eclipse.mylar.provisional.core.InteractionEvent;
import org.eclipse.mylar.provisional.tasklist.ITask;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.provider.ThreeWayDiff;
import org.eclipse.team.core.mapping.provider.ResourceDiff;
import org.eclipse.team.internal.ccvs.core.mapping.CVSActiveChangeSet;
import org.eclipse.team.internal.ccvs.core.mapping.ChangeSetResourceMapping;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
import org.osgi.service.prefs.Preferences;

/**
 * @author Mik Kersten
 */
public class MylarActiveChangeSet extends CVSActiveChangeSet implements IAdaptable {

	private static final String PREFIX_HTTP = "http://";

	private static final String PREFIX_HTTPS = "https://";

	private static final String PREFIX_DELIM = ": ";

	// HACK: copied from super
	private static final String CTX_TITLE = "title";

	public static final String SOURCE_ID = "org.eclipse.mylar.java.context.changeset.add";

	private boolean suppressInterestContribution = false;

	private ITask task;

	public MylarActiveChangeSet(ITask task, ActiveChangeSetManager manager) {
		super(manager, task.getDescription());
		this.task = task;
		initTitle();
	}

	@Override
	public boolean isUserCreated() {
		return true;
	}
	
	public void initTitle() {
		super.setName(task.getDescription());
		super.setTitle(task.getDescription());
	}

	/**
	 * Encodes the handle in the title, since init won't get called on this
	 * class.
	 */
	@Override
	public void save(Preferences prefs) {
		super.save(prefs);
		prefs.put(CTX_TITLE, getTitleForPersistance());
	}

	private String getTitleForPersistance() {
		return getTitle() + " (" + task.getHandleIdentifier() + ")";
	}

	public static String getHandleFromPersistedTitle(String title) {
		int delimStart = title.lastIndexOf('(');
		int delimEnd = title.lastIndexOf(')');
		if (delimStart != -1 && delimEnd != -1) {
			return title.substring(delimStart + 1, delimEnd);
		} else {
			return null;
		}
	}

	@Override
	public String getComment() {
		String completedPrefix = MylarIdePlugin.getDefault().getPreferenceStore().getString(
				MylarIdePlugin.COMMIT_PREFIX_COMPLETED);
		String progressPrefix = MylarIdePlugin.getDefault().getPreferenceStore().getString(
				MylarIdePlugin.COMMIT_PREFIX_PROGRESS);
		String comment = "";
		comment = generateComment(task, completedPrefix, progressPrefix);
		return comment;
	}

	@Override
	public void remove(IResource resource) {
		super.remove(resource);
	}

	@Override
	public void remove(IResource[] newResources) {
		super.remove(newResources);
	}

	@Override
	public void add(IDiff diff) {
		super.add(diff);
		IResource resource = getResourceFromDiff(diff);
		if (!suppressInterestContribution && resource != null) {
			Set<IResource> resources = new HashSet<IResource>();
			resources.add(resource); 
			if (MylarIdePlugin.getDefault() != null) {
				MylarIdePlugin.getDefault().getInterestUpdater().addResourceToContext(resources, InteractionEvent.Kind.SELECTION);
			}
		}
	}

	private IResource getResourceFromDiff(IDiff diff) {
		if (diff instanceof ResourceDiff) {
			return ((ResourceDiff)diff).getResource();
		} else if (diff instanceof ThreeWayDiff) {
			ThreeWayDiff threeWayDiff = (ThreeWayDiff)diff;
			return ResourcesPlugin.getWorkspace().getRoot().findMember(threeWayDiff.getPath());
		} else {
			return null;
		}
	} 

	@Override
	public void add(IDiff[] diffs) {
		super.add(diffs);
	}

	@Override
	public void add(IResource[] newResources) throws CoreException {
		super.add(newResources);
	}

	public void restoreResources(IResource[] newResources) throws CoreException {
		suppressInterestContribution = true;
		try {
			super.add(newResources);
			setComment(getComment());
		} catch (TeamException e) {
			throw e;
		} finally {
			suppressInterestContribution = false;
		}
	}

	@Override
	public IResource[] getResources() {
		List<IResource> allResources = getAllResourcesInChangeContext();
		return allResources.toArray(new IResource[allResources.size()]);
	}

	public List<IResource> getAllResourcesInChangeContext() {
		Set<IResource> allResources = new HashSet<IResource>();
		allResources.addAll(Arrays.asList(super.getResources()));
		if (MylarIdePlugin.getDefault() != null && task.isActive()) {
			// TODO: if super is always managed correctly should remove following line
			allResources.addAll(MylarIdePlugin.getDefault().getInterestingResources());
		}
		return new ArrayList<IResource>(allResources);
	}

	/**
	 * TODO: unnessary check context?
	 */
	public boolean contains(IResource local) {
		return getAllResourcesInChangeContext().contains(local);
	}

	public static String generateComment(ITask task, String completedPrefix, String progressPrefix) {
		String comment;
		completedPrefix = fixUpDelimIfPresent(completedPrefix);
		progressPrefix = fixUpDelimIfPresent(progressPrefix);
		if (task.isCompleted()) {
			comment = completedPrefix + PREFIX_DELIM;
		} else {
			comment = progressPrefix + PREFIX_DELIM;
		}
		comment += task.getDescription();
		String url = task.getUrl();
		if (url != null && !url.equals("") && !url.endsWith("//")) {
			comment += " \n" + url;
		}
		return comment;
	}

	private static String fixUpDelimIfPresent(String prefix) {
		if (prefix.endsWith(":") || prefix.endsWith(PREFIX_DELIM)) {
			prefix = prefix.substring(0, prefix.lastIndexOf(':'));
		}
		return prefix;
	}

	public static String getTaskIdFromCommentOrLabel(String comment) {
		int firstDelimIndex = comment.indexOf(PREFIX_DELIM);
		if (firstDelimIndex != -1) {
			int idStart = firstDelimIndex + PREFIX_DELIM.length();
			int idEnd = comment.indexOf(PREFIX_DELIM, firstDelimIndex + PREFIX_DELIM.length());// comment.indexOf(PREFIX_DELIM);
			if (idEnd != -1 && idStart < idEnd) {
				String id = comment.substring(idStart, idEnd);
				if (id != null)
					return id.trim();
			} else {
				// change set label
				return comment.substring(0, firstDelimIndex);
			}
		}
		return null;
	}

	public static String getUrlFromComment(String comment) {
		int httpIndex = comment.indexOf(PREFIX_HTTP);
		int httpsIndex = comment.indexOf(PREFIX_HTTPS);
		int idStart = -1;
		if (httpIndex != -1) {
			idStart = httpIndex;
		} else if (httpsIndex != -1) {
			idStart = httpsIndex;
		}
		if (idStart != -1) {
			int idEnd = comment.indexOf(' ', idStart);
			if (idEnd == -1) {
				return comment.substring(idStart);
			} else if (idEnd != -1 && idStart < idEnd) {
				return comment.substring(idStart, idEnd);
			}
		}
		return null;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof MylarActiveChangeSet && task != null) {
			MylarActiveChangeSet changeSet = (MylarActiveChangeSet) object;
			return task.equals(changeSet.getTask());
		} else {
			return super.equals(object);
		}
	}

	@Override
	public int hashCode() {
		if (task != null) {
			return task.hashCode();
		} else {
			return super.hashCode();
		}
	}

	public ITask getTask() {
		return task;
	}

	public Object getAdapter(Class adapter) {
		if (adapter == ResourceMapping.class) {
			return new ChangeSetResourceMapping(this);
		} else {
			return null;
		}
	}
}