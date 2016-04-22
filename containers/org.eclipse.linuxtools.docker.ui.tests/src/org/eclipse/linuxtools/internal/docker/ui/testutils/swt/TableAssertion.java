/*******************************************************************************
 * Copyright (c) 2015 Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/
package org.eclipse.linuxtools.internal.docker.ui.testutils.swt;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;

/**
 * Custom assertions on a given {@link SWTBotTable}.
 */
public class TableAssertion extends AbstractSWTBotAssertion<TableAssertion, SWTBotTable> {
	
	protected TableAssertion(final SWTBotTable actual) {
		super(actual, TableAssertion.class);
	}

	public static TableAssertion assertThat(final SWTBotTable actual) {
		return new TableAssertion(actual);
	}

}
