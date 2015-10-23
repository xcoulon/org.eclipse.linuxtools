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

package org.eclipse.linuxtools.internal.docker.ui.wizards;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.linuxtools.docker.core.DockerConnectionManager;
import org.eclipse.linuxtools.internal.docker.core.DefaultDockerConnectionSettingsFinder;
import org.eclipse.linuxtools.internal.docker.ui.BaseSWTBotTest;
import org.eclipse.linuxtools.internal.docker.ui.testutils.MockDockerConnectionSettingsFinder;
import org.eclipse.linuxtools.internal.docker.ui.testutils.swt.CheckBoxAssertion;
import org.eclipse.linuxtools.internal.docker.ui.testutils.swt.RadioAssertion;
import org.eclipse.linuxtools.internal.docker.ui.testutils.swt.TextAssertion;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Testing the {@link NewDockerConnection} {@link Wizard}
 */
public class NewDockerConnectionSWTBotTest extends BaseSWTBotTest {

	private SWTBotToolbarButton addConnectionButton;
	private SWTBotView dockerExplorerViewBot;

	@Before
	public void lookupDockerExplorerView() throws Exception {
		dockerExplorerViewBot = bot.viewById("org.eclipse.linuxtools.docker.ui.dockerExplorerView");
		dockerExplorerViewBot.show();
		bot.views().stream()
				.filter(v -> v.getReference().getId().equals("org.eclipse.linuxtools.docker.ui.dockerContainersView")
						|| v.getReference().getId().equals("org.eclipse.linuxtools.docker.ui.dockerImagesView"))
				.forEach(v -> v.close());
		dockerExplorerViewBot.setFocus();
		this.addConnectionButton = dockerExplorerViewBot.toolbarButton("&Add Connection");
	}

	@After
	public void closeWizard() {
		if (bot.button("Cancel") != null) {
			bot.button("Cancel").click();
		}
		DockerConnectionManager.getInstance().setConnectionSettingsFinder(new DefaultDockerConnectionSettingsFinder());
	}

	@Test
	public void shouldShowCustomUnixSocketSettingsWhenNoConnectionAvailable() {
		// given
		DockerConnectionManager.getInstance()
				.setConnectionSettingsFinder(MockDockerConnectionSettingsFinder.noDockerConnectionAvailable());
		// when
		// TODO: should wait until dialog appears after call to click()
		addConnectionButton.click();
		// then
		// Empty Connection name
		TextAssertion.assertThat(bot.text(0)).isEnabled().isEmpty();
		// "Use custom connection settings" should be enabled and checked
		CheckBoxAssertion.assertThat(bot.checkBox(0)).isEnabled().isChecked();
		// "Unix socket" radio should be enabled and selected
		RadioAssertion.assertThat(bot.radio(0)).isEnabled().isSelected();
		// "Unix socket path" text should be enabled and empty
		TextAssertion.assertThat(bot.text(1)).isEnabled().isEmpty();
		// "TCP Connection" radio should be enabled but unselected
		RadioAssertion.assertThat(bot.radio(1)).isEnabled().isNotSelected();
		// "URI" should be disabled but empty
		TextAssertion.assertThat(bot.text(2)).isNotEnabled().isEmpty();
		// "Enable Auth" checkbox should be unselected and disabled
		CheckBoxAssertion.assertThat(bot.checkBox(1)).isNotEnabled().isNotChecked();
		// "Path" for certs should be disabled and empty
		TextAssertion.assertThat(bot.text(3)).isNotEnabled().isEmpty();
	}

	@Test
	public void shouldShowDefaultUnixSocketConnectionSettingsWithValidConnectionAvailable() {
		// given
		MockDockerConnectionSettingsFinder.validUnixSocketConnectionAvailable();
		// when
		addConnectionButton.click();
		// TODO: should wait until dialog appears.
		// then
		// Connection name
		TextAssertion.assertThat(bot.text(0)).isEnabled().textEquals("mock");
		// "Use custom connection settings" should be enabled but unchecked
		CheckBoxAssertion.assertThat(bot.checkBox(0)).isEnabled().isNotChecked();
		// "Unix socket" radio should be disabled and selected
		RadioAssertion.assertThat(bot.radio(0)).isNotEnabled().isSelected();
		// "Unix socket path" text should be disabled and not empty
		TextAssertion.assertThat(bot.text(1)).isNotEnabled().textEquals("unix://var/run/docker.sock");
		// "TCP Connection" radio should be unselected and disabled
		RadioAssertion.assertThat(bot.radio(1)).isNotEnabled().isNotSelected();
		// "URI" should be disabled and empty
		TextAssertion.assertThat(bot.text(2)).isNotEnabled().isEmpty();
		// "Enable Auth" checkbox should be unselected and disabled
		CheckBoxAssertion.assertThat(bot.checkBox(1)).isNotEnabled().isNotChecked();
		// "Path" for certs should be disabled but not empty
		TextAssertion.assertThat(bot.text(3)).isNotEnabled().isEmpty();
	}

	@Test
	public void shouldShowDefaultTCPSettingsWithValidConnectionAvailable() {
		// given
		MockDockerConnectionSettingsFinder.validTCPConnectionAvailable();
		// when
		addConnectionButton.click();
		// TODO: should wait until dialog appears.
		// then
		// Connection name
		TextAssertion.assertThat(bot.text(0)).isEnabled().textEquals("mock");
		// "Use custom connection settings" should be enabled but unchecked
		CheckBoxAssertion.assertThat(bot.checkBox(0)).isEnabled().isNotChecked();
		// "Unix socket" radio should be disabled and unselected
		RadioAssertion.assertThat(bot.radio(0)).isNotEnabled().isNotSelected();
		// "Unix socket path" text should be disabled and not empty
		TextAssertion.assertThat(bot.text(1)).isNotEnabled().isEmpty();
		// "TCP Connection" radio should be selected but diabled
		RadioAssertion.assertThat(bot.radio(1)).isNotEnabled().isSelected();
		// "URI" should be disabled but not empty
		TextAssertion.assertThat(bot.text(2)).isNotEnabled().textEquals("tcp://1.2.3.4:1234");
		// "Enable Auth" checkbox should be selected but disabled
		CheckBoxAssertion.assertThat(bot.checkBox(1)).isNotEnabled().isChecked();
		// "Path" for certs should be disabled but not empty
		TextAssertion.assertThat(bot.text(3)).isNotEnabled().textEquals("/path/to/certs");
	}

}
