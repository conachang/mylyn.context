/*******************************************************************************
 * Copyright (c) 2004, 2008 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.context.core;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.eclipse.mylyn.monitor.core.InteractionEvent;

/**
 * @author Shawn Minto
 */
public class AggregateInteractionEvent extends InteractionEvent {

	// these are needed for collapsed events so that we can restore the context properly
	private final int numCollapsedEvents;

	private final int eventCountOnCreation;

	private final List<Duration> durationList;

	private static final String DELTA_UPDATED = "updated"; //$NON-NLS-1$

	private static final String XMLSTRING_LIST_DELIMITER = ","; //$NON-NLS-1$

	private static final String XMLSTRING_DURATION_DELIMITER = "/"; //$NON-NLS-1$

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
			InteractionContextExternalizer.DATE_FORMAT_STRING, Locale.ENGLISH);

	public static AggregateInteractionEvent getAggregatedEvent(InteractionEvent event, int eventCountOnCreation) {
		Duration duration = new Duration(event.getDate(), event.getDate(), event.getDelta().equals(DELTA_UPDATED));
		List<Duration> durationList = new ArrayList<Duration>();
		durationList.add(duration);
		return new AggregateInteractionEvent(event.getKind(), event.getStructureKind(), event.getStructureHandle(),
				event.getOriginId(), event.getNavigation(), "null", event.getInterestContribution(), //$NON-NLS-1$
				event.getDate(), event.getEndDate(), 1, eventCountOnCreation, durationList);
	}

	private static List<Duration> getUpdatedDurationList(List<Duration> _durationList, InteractionEvent e,
			boolean toNewDuration) {
		if (_durationList == null) {
			List<Duration> durationList = new ArrayList<Duration>();
			Duration newDuration = new Duration(e.getDate(), e.getEndDate(), e.getDelta().equals(DELTA_UPDATED));
			durationList.add(newDuration);
			return durationList;
		}

		List<Duration> durationList = new ArrayList<Duration>(_durationList);
		if (toNewDuration) {
			Duration newDuration = new Duration(e.getDate(), e.getEndDate(), e.getDelta().equals(DELTA_UPDATED));
			durationList.add(newDuration);
		} else {
			Duration lastDuration = durationList.get(durationList.size() - 1);
			Duration updatedDuration = new Duration(lastDuration.begin, e.getEndDate(), lastDuration.isUpdated
					|| e.getDelta().equals(DELTA_UPDATED));
			durationList.set(durationList.size() - 1, updatedDuration);
		}
		return durationList;
	}

	public static AggregateInteractionEvent appendOneEditEvent(AggregateInteractionEvent last, InteractionEvent event,
			int eventCountOnCreation, boolean toNewDuration) {
		List<Duration> newDuration = getUpdatedDurationList(last.getDurationList(), event, toNewDuration);
		return new AggregateInteractionEvent(event.getKind(), event.getStructureKind(), event.getStructureHandle(),
				event.getOriginId(), event.getNavigation(), "null", last.getInterestContribution() //$NON-NLS-1$
				+ event.getInterestContribution(), last.getDate(), event.getEndDate(),
				last.getNumCollapsedEvents() + 1, eventCountOnCreation, newDuration);
	}

	/**
	 * For parameter description see this class's getters.
	 */
	public AggregateInteractionEvent(Kind kind, String structureKind, String handle, String originId,
			String navigatedRelation, String delta, float interestContribution, int numCollapsedEvents,
			int eventCountOnCreation) {
		super(kind, structureKind, handle, originId, navigatedRelation, delta, interestContribution);
		this.numCollapsedEvents = numCollapsedEvents;
		this.eventCountOnCreation = eventCountOnCreation;
		this.durationList = null;
	}

	/**
	 * For parameter description see this class's getters.
	 */
	public AggregateInteractionEvent(Kind kind, String structureKind, String handle, String originId,
			String navigatedRelation, String delta, float interestContribution, Date startDate, Date endDate,
			int numCollapsedEvents, int eventCountOnCreation) {
		this(kind, structureKind, handle, originId, navigatedRelation, delta, interestContribution, startDate, endDate,
				numCollapsedEvents, eventCountOnCreation, null);
	}

	public AggregateInteractionEvent(Kind kind, String structureKind, String handle, String originId,
			String navigatedRelation, String delta, float interestContribution, Date startDate, Date endDate,
			int numCollapsedEvents, int eventCountOnCreation, List<Duration> durationList) {

		super(kind, structureKind, handle, originId, navigatedRelation, delta, interestContribution, startDate, endDate);
		this.numCollapsedEvents = numCollapsedEvents;
		this.eventCountOnCreation = eventCountOnCreation;
		this.durationList = durationList;
	}

	/**
	 * Returns the number of events this event represents
	 */
	public int getNumCollapsedEvents() {
		return numCollapsedEvents;
	}

	/**
	 * Returns the number of user events that had occurred when this was created or -1 to use the context's count
	 */
	public int getEventCountOnCreation() {
		return eventCountOnCreation;
	}

	public List<Duration> getDurationList() {
		return durationList != null ? new ArrayList<Duration>(durationList) : null;
	}

	public String getDurationListXMLString() {
		if (durationList == null) {
			return null;
		}
		return durationList.toString();
	}

	public static List<Duration> getListOfDurationFromXMLString(String xmlString) {
		if (xmlString == null || xmlString.charAt(0) != '[' || xmlString.charAt(xmlString.length() - 1) != ']') {
			return null;
		}
		StringTokenizer tokenizer = new StringTokenizer(xmlString.substring(1, xmlString.length() - 2),
				XMLSTRING_LIST_DELIMITER);
		List<Duration> durationList = new ArrayList<Duration>();
		try {
			while (tokenizer.hasMoreTokens()) {
				durationList.add(new Duration(tokenizer.nextToken()));
			}
		} catch (NoSuchElementException e) {
			e.printStackTrace();
			return null;
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
		return durationList;

	}

	static class Duration {
		final Date begin;

		final Date end;

		final boolean isUpdated;

		Duration(Date begin, Date end, boolean isUpdated) {
			this.begin = begin;
			this.end = end;
			this.isUpdated = isUpdated;
		}

		Duration(String xmlString) throws ParseException, NoSuchElementException {
			StringTokenizer tokenizer = new StringTokenizer(xmlString, XMLSTRING_DURATION_DELIMITER);
			this.begin = dateFormat.parse(tokenizer.nextToken());
			this.end = dateFormat.parse(tokenizer.nextToken());
			this.isUpdated = Boolean.valueOf(tokenizer.nextToken());
		}

		@Override
		public String toString() {
			return dateFormat.format(begin) + XMLSTRING_DURATION_DELIMITER + dateFormat.format(end)
					+ XMLSTRING_DURATION_DELIMITER + isUpdated;
		}
	}

}
