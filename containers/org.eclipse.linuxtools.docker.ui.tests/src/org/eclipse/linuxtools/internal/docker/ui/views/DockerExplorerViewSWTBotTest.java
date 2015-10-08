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

package org.eclipse.linuxtools.internal.docker.ui.views;

import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.linuxtools.docker.core.DockerConnectionManager;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.internal.docker.core.DefaultDockerConnectionStorageManager;
import org.eclipse.linuxtools.internal.docker.core.DockerConnection;
import org.eclipse.linuxtools.internal.docker.ui.BaseSWTBotTest;
import org.eclipse.linuxtools.internal.docker.ui.testutils.MockDockerClientFactory;
import org.eclipse.linuxtools.internal.docker.ui.testutils.MockDockerConnectionFactory;
import org.eclipse.linuxtools.internal.docker.ui.testutils.MockDockerConnectionStorageManagerFactory;
import org.eclipse.linuxtools.internal.docker.ui.testutils.MockDockerContainerFactory;
import org.eclipse.linuxtools.internal.docker.ui.testutils.MockDockerImageFactory;
import org.eclipse.linuxtools.internal.docker.ui.testutils.swt.DockerExplorerViewAssertion;
import org.eclipse.linuxtools.internal.docker.ui.testutils.swt.SWTUtils;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.ui.PlatformUI;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.spotify.docker.client.DockerClient;

/**
 * Testing the {@link DockerExplorerView} {@link Viewer}
 */
public class DockerExplorerViewSWTBotTest extends BaseSWTBotTest {

	private SWTBotView dockerExplorerViewBot;
	private DockerExplorerView dockerExplorerView;
	private SWTBotTree dockerExplorerViewTreeBot;

	@Before
	public void lookupDockerExplorerView() throws InterruptedException {
		SWTUtils.asyncExec(() -> {try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.showView("org.eclipse.linuxtools.docker.ui.dockerExplorerView");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Failed to open Docker Explorer view: " + e.getMessage());
		}});
		dockerExplorerViewBot = bot.viewById("org.eclipse.linuxtools.docker.ui.dockerExplorerView");
		dockerExplorerView = (DockerExplorerView) (dockerExplorerViewBot.getViewReference().getView(true));
		bot.views().stream()
				.filter(v -> v.getReference().getId().equals("org.eclipse.linuxtools.docker.ui.dockerContainersView")
						|| v.getReference().getId().equals("org.eclipse.linuxtools.docker.ui.dockerImagesView"))
				.forEach(v -> v.close());
	}

	@After
	public void clearConnectionManager() throws InterruptedException {
		SWTUtils.syncExec(() -> { Stream.of(DockerConnectionManager.getInstance().getConnections())
				.forEach(c -> DockerConnectionManager.getInstance().removeConnection(c));
				dockerExplorerView.getCommonViewer().refresh(true);});
	}
	
	@AfterClass
	public static void restoreDefaultConfig() {
		DockerConnectionManager.getInstance().setConnectionStorageManager(new DefaultDockerConnectionStorageManager());
	}

	private void configureConnectionManager(final IDockerConnection... connections) throws InterruptedException {
		DockerConnectionManager.getInstance()
				.setConnectionStorageManager(MockDockerConnectionStorageManagerFactory.providing(connections));
		SWTUtils.asyncExec(() -> {
			DockerConnectionManager.getInstance().reloadConnections();
			dockerExplorerView.getCommonViewer().refresh();
		});

	}

	@Test
	public void shouldDisplayExplanationPane() throws InterruptedException {
		// given
		configureConnectionManager();
		// then
		DockerExplorerViewAssertion.assertThat(dockerExplorerView).isEmpty();
	}

	@Test
	public void shouldDisplayConnectionsPane() throws InterruptedException {
		// given
		final DockerClient client = MockDockerClientFactory.noImages().noContainers();
		final DockerConnection dockerConnection = MockDockerConnectionFactory.from("Test", client).get();
		configureConnectionManager(dockerConnection);
		// then
		DockerExplorerViewAssertion.assertThat(dockerExplorerView).isNotEmpty();
	}

	@Test
	public void shouldRefreshImagesAndShowChanges() throws Exception {
		// given
		final DockerClient client = MockDockerClientFactory.noImages().noContainers();
		final DockerConnection dockerConnection = MockDockerConnectionFactory.from("Test", client).get();
		configureConnectionManager(dockerConnection);
		SWTUtils.asyncExec(() -> dockerExplorerView.getCommonViewer().expandAll());
		final TreeItem[] items = SWTUtils.syncExec(() -> dockerExplorerView.getCommonViewer().getTree().getItems());
		SWTUtils.syncAssert(() -> {
			// only 1 connection element at the root.
			Assertions.assertThat(items).hasSize(1);
			Assertions.assertThat(items[0].getItemCount()).isEqualTo(2);
		});
		final SWTBotTreeItem imagesTreeItem = SWTUtils.getTreeItem(dockerExplorerViewBot, "Test (null)", "Images");
		SWTUtils.syncAssert(() -> {
			Assertions.assertThat(imagesTreeItem.getItems().length).isEqualTo(0);
		});
		
		// when locating the 'Images' node and hit refresh
		SWTUtils.asyncExec(() -> {
			dockerExplorerViewTreeBot = dockerExplorerViewBot.bot().tree();
			dockerExplorerViewTreeBot.select(imagesTreeItem);
			// update the client 
			final DockerClient updatedClient = MockDockerClientFactory.images(MockDockerImageFactory.name("foo/bar").build()).noContainers();
			dockerConnection.setClient(updatedClient);
			dockerExplorerViewTreeBot.contextMenu("Refresh").click();
		});
		SWTUtils.syncExec(() -> imagesTreeItem.expand());
		
		// then check that there are images now
		SWTUtils.syncAssert(() -> {
			Assertions.assertThat(imagesTreeItem.isExpanded()).isTrue();
			Assertions.assertThat(imagesTreeItem.getItems().length).isEqualTo(1);
		});
	}
	
	@Test
	public void shouldRefreshContainersAndShowChanges() throws Exception {
		// given
		final DockerClient client = MockDockerClientFactory.noImages().noContainers();
		final DockerConnection dockerConnection = MockDockerConnectionFactory.from("Test", client).get();
		configureConnectionManager(dockerConnection);
		SWTUtils.asyncExec(() -> dockerExplorerView.getCommonViewer().expandAll());
		final TreeItem[] items = SWTUtils.syncExec(() -> dockerExplorerView.getCommonViewer().getTree().getItems());
		SWTUtils.syncAssert(() -> {
			// only 1 connection element at the root.
			Assertions.assertThat(items).hasSize(1);
			Assertions.assertThat(items[0].getItemCount()).isEqualTo(2);
		});
		final SWTBotTreeItem containersTreeItem = SWTUtils.getTreeItem(dockerExplorerViewBot, "Test (null)", "Containers");
		SWTUtils.syncAssert(() -> {
			Assertions.assertThat(containersTreeItem.getItems().length).isEqualTo(0);
		});
		
		// when locating the 'Containers' node and hit refresh
		SWTUtils.asyncExec(() -> {
			dockerExplorerViewTreeBot = dockerExplorerViewBot.bot().tree();
			dockerExplorerViewTreeBot.select(containersTreeItem);
			// update the client 
			final DockerClient updatedClient = MockDockerClientFactory.noImages().containers(MockDockerContainerFactory.name("foo_bar").build());
			dockerConnection.setClient(updatedClient);
			dockerExplorerViewTreeBot.contextMenu("Refresh").click();
		});
		SWTUtils.syncExec(() -> containersTreeItem.expand());
		// then check that there are images now
		SWTUtils.syncAssert(() -> {
			Assertions.assertThat(containersTreeItem.isExpanded()).isTrue();
			Assertions.assertThat(containersTreeItem.getItems().length).isEqualTo(1);
		});
	}

}
