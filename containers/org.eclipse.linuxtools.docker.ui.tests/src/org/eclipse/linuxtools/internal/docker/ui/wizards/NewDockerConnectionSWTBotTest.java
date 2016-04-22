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
import org.eclipse.linuxtools.internal.docker.ui.testutils.MockDockerConnectionSettingsFinder;
import org.eclipse.linuxtools.internal.docker.ui.testutils.swt.ButtonAssertion;
import org.eclipse.linuxtools.internal.docker.ui.testutils.swt.CloseWelcomePageRule;
import org.eclipse.linuxtools.internal.docker.ui.testutils.swt.RadioAssertion;
import org.eclipse.linuxtools.internal.docker.ui.testutils.swt.SWTUtils;
import org.eclipse.linuxtools.internal.docker.ui.testutils.swt.TableAssertion;
import org.eclipse.linuxtools.internal.docker.ui.testutils.swt.TextAssertion;
import org.eclipse.linuxtools.internal.docker.ui.views.DockerContainersView;
import org.eclipse.linuxtools.internal.docker.ui.views.DockerExplorerView;
import org.eclipse.linuxtools.internal.docker.ui.views.DockerImagesView;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.eclipse.ui.PlatformUI;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing the {@link NewDockerConnection} {@link Wizard}
 */
@RunWith(SWTBotJunit4ClassRunner.class) 
public class NewDockerConnectionSWTBotTest {

	private SWTWorkbenchBot bot = new SWTWorkbenchBot();
	private SWTBotToolbarButton addConnectionButton;
	private SWTBotView dockerExplorerViewBot;

	@ClassRule
	public static CloseWelcomePageRule closeWelcomePage = new CloseWelcomePageRule(); 
	
	@Before
	public void lookupDockerExplorerView() throws Exception {
		SWTUtils.syncExec(() -> {try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.showView(DockerExplorerView.VIEW_ID);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Failed to open Docker Explorer view: " + e.getMessage());
		}});
		dockerExplorerViewBot = bot.viewById(DockerExplorerView.VIEW_ID);
		dockerExplorerViewBot.show();
		bot.views().stream()
				.filter(v -> v.getReference().getId().equals(DockerContainersView.VIEW_ID)
						|| v.getReference().getId().equals(DockerImagesView.VIEW_ID))
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

//	@Test
//	@Ignore
//	public void shouldShowCustomUnixSocketSettingsWhenNoConnectionAvailable() {
//		// given
//		DockerConnectionManager.getInstance()
//				.setConnectionSettingsFinder(MockDockerConnectionSettingsFinder.noDockerConnectionAvailable());
//		// when
//		// TODO: should wait until dialog appears after call to click()
//		addConnectionButton.click();
//		// then
//		// Empty Connection name
//		TextAssertion.assertThat(bot.text(0)).isEnabled().isEmpty();
//		// "Use custom connection settings" should be enabled and checked
//		CheckBoxAssertion.assertThat(bot.checkBox(0)).isEnabled().isChecked();
//		// "Unix socket" radio should be enabled and selected
//		RadioAssertion.assertThat(bot.radio(0)).isEnabled().isSelected();
//		// "Unix socket path" text should be enabled and empty
//		TextAssertion.assertThat(bot.text(1)).isEnabled().isEmpty();
//		// "TCP Connection" radio should be enabled but unselected
//		RadioAssertion.assertThat(bot.radio(1)).isEnabled().isNotSelected();
//		// "URI" should be disabled but empty
//		TextAssertion.assertThat(bot.text(2)).isNotEnabled().isEmpty();
//		// "Enable Auth" checkbox should be unselected and disabled
//		CheckBoxAssertion.assertThat(bot.checkBox(1)).isNotEnabled().isNotChecked();
//		// "Path" for certs should be disabled and empty
//		TextAssertion.assertThat(bot.text(3)).isNotEnabled().isEmpty();
//	}
//
//	@Test
//	@Ignore
//	public void shouldShowDefaultUnixSocketConnectionSettingsWithValidConnectionAvailable() {
//		// given
//		MockDockerConnectionSettingsFinder.validUnixSocketConnectionAvailable();
//		// when
//		addConnectionButton.click();
//		// TODO: should wait until dialog appears.
//		// then
//		// Connection name
//		TextAssertion.assertThat(bot.text(0)).isEnabled().textEquals("mock");
//		// "Use custom connection settings" should be enabled but unchecked
//		CheckBoxAssertion.assertThat(bot.checkBox(0)).isEnabled().isNotChecked();
//		// "Unix socket" radio should be disabled and selected
//		RadioAssertion.assertThat(bot.radio(0)).isNotEnabled().isSelected();
//		// "Unix socket path" text should be disabled and not empty
//		TextAssertion.assertThat(bot.text(1)).isNotEnabled().textEquals("unix://var/run/docker.sock");
//		// "TCP Connection" radio should be unselected and disabled
//		RadioAssertion.assertThat(bot.radio(1)).isNotEnabled().isNotSelected();
//		// "URI" should be disabled and empty
//		TextAssertion.assertThat(bot.text(2)).isNotEnabled().isEmpty();
//		// "Enable Auth" checkbox should be unselected and disabled
//		CheckBoxAssertion.assertThat(bot.checkBox(1)).isNotEnabled().isNotChecked();
//		// "Path" for certs should be disabled but not empty
//		TextAssertion.assertThat(bot.text(3)).isNotEnabled().isEmpty();
//	}
//
//	@Test
//	@Ignore
//	public void shouldShowDefaultTCPSettingsWithValidConnectionAvailable() {
//		// given
//		MockDockerConnectionSettingsFinder.validTCPConnectionAvailable();
//		// when
//		addConnectionButton.click();
//		bot.waitUntil(Conditions.shellIsActive(WizardMessages.getString("NewDockerConnection.title"))); //$NON-NLS-1$
//		// TODO: should wait until dialog appears.
//		// then
//		// Connection name
//		TextAssertion.assertThat(bot.text(0)).isEnabled().textEquals("mock");
//		// "Use custom connection settings" should be enabled but unchecked
//		CheckBoxAssertion.assertThat(bot.checkBox(0)).isEnabled().isNotChecked();
//		// "Unix socket" radio should be disabled and unselected
//		RadioAssertion.assertThat(bot.radio(0)).isNotEnabled().isNotSelected();
//		// "Unix socket path" text should be disabled and not empty
//		TextAssertion.assertThat(bot.text(1)).isNotEnabled().isEmpty();
//		// "TCP Connection" radio should be selected but diabled
//		RadioAssertion.assertThat(bot.radio(1)).isNotEnabled().isSelected();
//		// "URI" should be disabled but not empty
//		TextAssertion.assertThat(bot.text(2)).isNotEnabled().textEquals("tcp://1.2.3.4:1234");
//		// "Enable Auth" checkbox should be selected but disabled
//		CheckBoxAssertion.assertThat(bot.checkBox(1)).isNotEnabled().isChecked();
//		// "Path" for certs should be disabled but not empty
//		TextAssertion.assertThat(bot.text(3)).isNotEnabled().textEquals("/path/to/certs");
//	}
//	
//	@Test
//	@Ignore
//	public void shouldEnableExistingInstancesTableWhenInstancesAreAvailable() {
//		// given
//		MockDockerConnectionSettingsFinder.validTCPConnectionAvailable();
//		// when
//		
//		// then
//	}
//	
	@Test
	public void shouldEnableCustonSettingsByDefaultWhenNoInstanceAvailable() {
		// given
		// when
		MockDockerConnectionSettingsFinder.noExistingDockerInstanceAvailable();
		addConnectionButton.click();
		//bot.wait();
		// then widgets for existing runtimes should be unselected/disabled
		RadioAssertion.assertThat(bot.radio(0)).isEnabled().isNotSelected();
		TableAssertion.assertThat(bot.table(0)).isNotEnabled();
		// and widgets for custom settings should be selected/enabled
		RadioAssertion.assertThat(bot.radio(1)).isEnabled().isSelected();
		TextAssertion.assertThat(bot.text(0)).isEnabled(); // connection_name
		TextAssertion.assertThat(bot.text(1)).isEnabled(); // connection_url
		RadioAssertion.assertThat(bot.radio(2)).isEnabled().isNotSelected(); // tls_verify
		TextAssertion.assertThat(bot.text(2)).isEnabled(); // cert_path
		ButtonAssertion.assertThat(bot.button(0)).isEnabled(); // test_connection
		
	}
	

}
