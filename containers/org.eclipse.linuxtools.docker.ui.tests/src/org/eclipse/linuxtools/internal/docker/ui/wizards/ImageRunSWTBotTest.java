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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.linuxtools.internal.docker.core.DockerConnection;
import org.eclipse.linuxtools.internal.docker.ui.BaseSWTBotTest;
import org.eclipse.linuxtools.internal.docker.ui.launch.IRunDockerImageLaunchConfigurationConstants;
import org.eclipse.linuxtools.internal.docker.ui.launch.LaunchConfigurationUtils;
import org.eclipse.linuxtools.internal.docker.ui.testutils.MockDockerClientFactory;
import org.eclipse.linuxtools.internal.docker.ui.testutils.MockDockerConnectionFactory;
import org.eclipse.linuxtools.internal.docker.ui.testutils.MockDockerContainerFactory;
import org.eclipse.linuxtools.internal.docker.ui.testutils.MockDockerContainerInfoFactory;
import org.eclipse.linuxtools.internal.docker.ui.testutils.MockDockerImageFactory;
import org.eclipse.linuxtools.internal.docker.ui.testutils.MockDockerImageInfoFactory;
import org.eclipse.linuxtools.internal.docker.ui.testutils.swt.SWTUtils;
import org.eclipse.linuxtools.internal.docker.ui.views.DockerExplorerView;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.ImageInfo;

/**
 * Testing the {@link ImageRun} wizard
 */
public class ImageRunSWTBotTest extends BaseSWTBotTest {

	private SWTBotView dockerExplorerViewBot;

	private DockerExplorerView dockerExplorerView;

	@Before
	public void lookupDockerExplorerView() {
		dockerExplorerViewBot = bot.viewById("org.eclipse.linuxtools.docker.ui.dockerExplorerView");
		dockerExplorerView = (DockerExplorerView) (dockerExplorerViewBot.getViewReference().getView(true));
		dockerExplorerViewBot.show();
		dockerExplorerViewBot.setFocus();
	}

	@Test
	public void shouldCreateLaunchConfigurationWhenRunningNamedContainer()
			throws InterruptedException, DockerException, CoreException {
		// image to use
		final Image image = MockDockerImageFactory.id("1a2b3c4d5e6f7g").name("foo/bar:latest").build();
		final ImageInfo imageInfo = MockDockerImageInfoFactory.volume("/foo/bar")
				.command(Arrays.asList("the", "command")).entrypoint(Arrays.asList("the", "entrypoint")).build();
		// container to be created
		final Container createdContainer = MockDockerContainerFactory.id("MockContainer").name("foo")
				.imageId("1a2b3c4d5e6f7g").status("Started 1 second ago").build();
		final ContainerInfo containerInfo = MockDockerContainerInfoFactory.build();
		final DockerClient client = MockDockerClientFactory.image(image, imageInfo)
				.container(createdContainer, containerInfo).build();
		// expected response when creating the container
		final ContainerCreation containerCreation = Mockito.mock(ContainerCreation.class);
		Mockito.when(containerCreation.id()).thenReturn("MockContainer");
		Mockito.when(client.createContainer(Matchers.any(), Matchers.any())).thenReturn(containerCreation);

		final DockerConnection dockerConnection = MockDockerConnectionFactory.from("Test", client).get();
		configureConnectionManager(dockerExplorerView, dockerConnection);
		SWTUtils.asyncExec(() -> dockerExplorerView.getCommonViewer().expandAll());
		SWTUtils.syncExec(() -> dockerExplorerView.getCommonViewer().getTree().getItems());
		final SWTBotTreeItem imagesTreeItem = SWTUtils.getTreeItem(dockerExplorerViewBot, "Test", "Images").expand();
		// when select image and click on run to open the wizard
		SWTUtils.getTreeItem(imagesTreeItem, "foo/bar").select();
		dockerExplorerViewBot.bot().tree().contextMenu("Run Image...").click();
		bot.waitUntil(Conditions.shellIsActive("Run a Docker Image"), TimeUnit.SECONDS.toMillis(1)); //$NON-NLS-1$
		// configure container
		bot.text(0).setText("foo");
		// bot.button("Next >").click();
		bot.button("Finish").click();
		// wait for background job to complete
		SWTUtils.waitForJobsToComplete();

		// then
		// check that the client was called
		Mockito.verify(client).createContainer(Matchers.any(), Matchers.eq("foo"));
		// check that a launch configuration was created
		final ILaunchConfigurationType launchConfigType = LaunchConfigurationUtils
				.getLaunchConfigType(IRunDockerImageLaunchConfigurationConstants.CONFIG_TYPE_ID);
		final ILaunchConfiguration launchConfiguration = LaunchConfigurationUtils
				.getLaunchConfigurationByImageName(launchConfigType, "1a2b3c4d5e6f7g");
		assertThat(launchConfiguration).isNotNull();

	}
}
