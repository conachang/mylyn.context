/*******************************************************************************
 * Copyright (c) 2015 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.context.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.eclipse.mylyn.internal.context.core.AggregateInteractionEvent;
import org.eclipse.mylyn.internal.context.core.AggregateInteractionEvent.Duration;
import org.eclipse.mylyn.monitor.core.InteractionEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AggregateInteractionEventTest {
	TimeZone defaultTimeZone;

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S z", Locale.ENGLISH);

	@Before
	public void setUp() throws Exception {
		defaultTimeZone = TimeZone.getDefault();
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
	}

	@After
	public void tearDown() throws Exception {
		TimeZone.setDefault(defaultTimeZone);
	}

	@Test
	public void testGetAggregatedEvent() {
		//A referred event
		InteractionEvent ie = createMockEdit(1435121946000L, 1435121947000L, false);
		AggregateInteractionEvent aie = AggregateInteractionEvent.getAggregatedEvent(ie, 0);
		assertEquals(1, aie.getDurationList().size());
		assertFalse(aie.getDurationList().get(0).isModified());
		assertEquals(1, aie.getNumCollapsedEvents());

		//A modified event
		InteractionEvent ie2 = createMockEdit(1435121948000L, 1435121949000L, true);
		aie = AggregateInteractionEvent.appendOneEditEvent(aie, ie2, false);
		assertEquals(1, aie.getDurationList().size());
		assertTrue(aie.getDurationList().get(0).isModified());
		assertEquals(2, aie.getNumCollapsedEvents());

		//A referred event again
		InteractionEvent ie3 = createMockEdit(1435121950000L, 1435121951000L, false);
		aie = AggregateInteractionEvent.appendOneEditEvent(aie, ie3, false);
		assertEquals(1, aie.getDurationList().size());
		assertTrue(aie.getDurationList().get(0).isModified());
		assertEquals(3, aie.getNumCollapsedEvents());

		//A referred event to new duration.
		InteractionEvent ie4 = createMockEdit(1435122050000L, 1435122051000L, false);
		aie = AggregateInteractionEvent.appendOneEditEvent(aie, ie4, true);
		assertEquals(2, aie.getDurationList().size());
		List<Duration> durationList = aie.getDurationList();

		Duration d0 = durationList.get(0);
		assertEquals(ie.getDate(), d0.getBegin());
		assertEquals(ie3.getEndDate(), d0.getEnd());
		assertTrue(d0.isModified());

		Duration d1 = durationList.get(1);
		assertEquals(ie4.getDate(), d1.getBegin());
		assertEquals(ie4.getEndDate(), d1.getEnd());
		assertFalse(d1.isModified());

		assertEquals(4, aie.getNumCollapsedEvents());

		assertEquals(
				"[2015-06-24 04:59:06.0 GMT/2015-06-24 04:59:11.0 GMT/modified, 2015-06-24 05:00:50.0 GMT/2015-06-24 05:00:51.0 GMT/referred]",
				aie.getDurationListXMLString());

	}

	@Test
	public void getListOfDurationFromXMLStringTest() throws ParseException {
		String xmlString = "[2015-06-24 04:59:06.0 GMT/2015-06-24 04:59:11.0 GMT/modified, 2015-06-24 05:00:50.0 GMT/2015-06-24 05:00:51.0 GMT/referred]";
		List<Duration> actualDurationList = AggregateInteractionEvent.getListOfDurationFromXMLString(xmlString);

		List<Duration> expectedDurationList = new ArrayList<Duration>();
		expectedDurationList.add(new Duration(dateFormat.parse("2015-06-24 04:59:06.0 GMT"),
				dateFormat.parse("2015-06-24 04:59:11.0 GMT"), true));
		expectedDurationList.add(new Duration(dateFormat.parse("2015-06-24 05:00:50.0 GMT"),
				dateFormat.parse("2015-06-24 05:00:51.0 GMT"), false));

		assertEquals(expectedDurationList, actualDurationList);
	}

	private InteractionEvent createMockEdit(long startTime, long endTime, boolean isModified) {
		return new InteractionEvent(InteractionEvent.Kind.EDIT, "kind", "handle", "source-id", "id", isModified
				? AggregateInteractionEvent.DELTA_MODIFIED
				: AggregateInteractionEvent.DELTA_REFERRED, 1, new Date(startTime), new Date(endTime));

	}

}
