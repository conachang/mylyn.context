/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.context.tests;

import org.eclipse.mylyn.internal.context.core.InteractionContext;
import org.eclipse.mylyn.internal.context.core.InteractionContextScaling;
import org.eclipse.mylyn.monitor.core.InteractionEvent;

/**
 * @author Mik Kersten
 */
public class InteractionContextTest extends AbstractContextTest {

	public void testReset() {
		InteractionEvent event = mockSelection("aaaaa");
		InteractionContext context = new InteractionContext("test", new InteractionContextScaling());
		context.parseEvent(event);
		assertEquals(1, context.getUserEventCount());
		assertEquals(1, context.getInteractionHistory().size());

		context.reset();
		assertEquals(0, context.getUserEventCount());
		assertEquals(0, context.getInteractionHistory().size());
	}

	public void testParseEventWithNullHandle() {
		InteractionEvent event = mockSelection(null);
		InteractionContext context = new InteractionContext("test", new InteractionContextScaling());
		assertNull(context.parseEvent(event));
	}

	public void testSetScalingFactors() {
		InteractionContextScaling oldScalingFactors = new InteractionContextScaling();
//		InteractionContextScaling newScalingFactors = new InteractionContextScaling();
//		newScalingFactors.get(InteractionEvent.Kind.EDIT).setValue(10f);
		InteractionContext globalContext = new InteractionContext("global", oldScalingFactors);
		assertEquals(oldScalingFactors, globalContext.getScaling());
//		globalContext.setScaling(newScalingFactors);
		((InteractionContextScaling) globalContext.getScaling()).set(InteractionEvent.Kind.EDIT, 10f);
//		assertEquals(newScalingFactors, globalContext.getScaling());
		assertEquals(10f, globalContext.getScaling().get(InteractionEvent.Kind.EDIT));
	}

	public void testScalingFactorSet() {
		InteractionContextScaling scalingFactors = new InteractionContextScaling();
		scalingFactors.setDecay(0f);
		InteractionContext context = new InteractionContext("test", scalingFactors);
		assertEquals(0f, context.getScaling().getDecay());
	}

	public void testIsInteresting() {
		InteractionContext context = new InteractionContext("test", new InteractionContextScaling());

		assertFalse(context.isInteresting("1"));

		context.parseEvent(mockSelection("1"));

		assertTrue(context.isInteresting("1"));

		context.parseEvent(mockInterestContribution("1", -10));

		assertFalse(context.isInteresting("1"));
		assertNotNull(context.get("1"));
	}

}
