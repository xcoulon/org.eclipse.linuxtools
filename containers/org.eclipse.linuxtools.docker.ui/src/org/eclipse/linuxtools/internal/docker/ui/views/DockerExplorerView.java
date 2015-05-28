/*******************************************************************************
 * Copyright (c) 2014, 2015 Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/

package org.eclipse.linuxtools.internal.docker.ui.views;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.linuxtools.docker.core.DockerConnectionManager;
import org.eclipse.linuxtools.docker.core.IDockerConnectionManagerListener;
import org.eclipse.linuxtools.docker.core.IDockerContainer;
import org.eclipse.linuxtools.docker.core.IDockerImage;
import org.eclipse.linuxtools.internal.docker.ui.wizards.NewDockerConnection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * @author xcoulon
 *
 */
public class DockerExplorerView extends CommonNavigator implements
		IDockerConnectionManagerListener, ITabbedPropertySheetPageContributor {

	private static final String NO_CONNECTION_LABEL = "NoConnection.label"; //$NON-NLS-1$

	public static final String VIEW_ID = "org.eclipse.linuxtools.docker.ui.dockerExplorerView";
	
	private Control connectionsPane;
	private Control explanationsPane;
	private PageBook pageBook;

	/** the search text widget. to filter containers and images. */
	private Text search;

	private ViewerFilter containersAndImagesSearchFilter;

	@Override
	protected Object getInitialInput() {
		return DockerConnectionManager.getInstance();
	}

	@Override
	protected CommonViewer createCommonViewer(final Composite parent) {
		final CommonViewer viewer = super.createCommonViewer(parent);
		setLinkingEnabled(false);
		DockerConnectionManager.getInstance()
				.addConnectionManagerListener(this);
		return viewer;
	}

	@Override
	public String getContributorId() {
		return getSite().getId();
	}
	
	@Override
    public Object getAdapter(@SuppressWarnings("rawtypes") final Class adapter) {
        if (adapter == IPropertySheetPage.class) {
            return new TabbedPropertySheetPage(this, true);
        }
        return super.getAdapter(adapter);
    }

	@Override
	public void dispose() {
		DockerConnectionManager.getInstance().removeConnectionManagerListener(
				this);
		super.dispose();
	}

	@Override
	public void createPartControl(final Composite parent) {
		final FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		this.pageBook = new PageBook(parent, SWT.NONE);
		this.connectionsPane = createConnectionsPane(pageBook, toolkit);
		this.explanationsPane = createExplanationPane(connectionsPane,
				pageBook, toolkit);
		showConnectionsOrExplanations();
		this.containersAndImagesSearchFilter = getContainersAndImagesSearchFilter();
		getCommonViewer().addFilter(containersAndImagesSearchFilter);
	}

	/**
	 * @return a {@link ViewerFilter} that will retain {@link IDockerContainer}
	 *         and {@link IDockerImage} that match the content of the
	 *         {@link DockerExplorerView#search} text widget.
	 */
	private ViewerFilter getContainersAndImagesSearchFilter() {
		return new ViewerFilter() {

			@Override
			public boolean select(Viewer viewer, Object parentElement,
					Object element) {
				// filtering Docker containers
				if (element instanceof IDockerContainer
						|| element instanceof IDockerImage) {
					return element.toString().contains(
							DockerExplorerView.this.search.getText());
				}
				// any other element makes it through the filter (i.e., it is
				// displayed)
				return true;
			}
		};
	}

	private Control createConnectionsPane(final PageBook pageBook,
			final FormToolkit toolkit) {
		final Form form = toolkit.createForm(pageBook);
		final Composite container = form.getBody();
		GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5)
				.applyTo(container);
		this.search = new Text(container, SWT.SEARCH | SWT.ICON_SEARCH);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL)
				.grab(true, false).applyTo(search);
		search.addModifyListener(onSearch());
		super.createPartControl(container);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL)
				.grab(true, true).applyTo(getCommonViewer().getControl());
		return form;
	}

	/**
	 * Filters {@link IDockerContainer} and {@link IDockerImage} using the input
	 * text in the search widget of this view.
	 * 
	 * @return
	 */
	private ModifyListener onSearch() {
		return new ModifyListener() {

			@Override
			public void modifyText(final ModifyEvent e) {
				final CommonViewer viewer = DockerExplorerView.this
						.getCommonViewer();
				final TreePath[] treePaths = viewer.getExpandedTreePaths();
				for (TreePath treePath : treePaths) {
					final Object lastSegment = treePath.getLastSegment();
					if (lastSegment instanceof DockerExplorerContentProvider.DockerContainersCategory
							|| lastSegment instanceof DockerExplorerContentProvider.DockerImagesCategory) {
						viewer.refresh(lastSegment);
						viewer.expandToLevel(treePath,
								AbstractTreeViewer.ALL_LEVELS);
					}
				}
			}
		};
	}

	private Control createExplanationPane(final Control connectionsPane,
			final PageBook pageBook, final FormToolkit toolkit) {
		final Form form = toolkit.createForm(pageBook);
		final Composite container = form.getBody();
		GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5)
				.applyTo(container);
		final Link link = new Link(container, SWT.NONE);
		link.setText(DVMessages.getFormattedString(NO_CONNECTION_LABEL,
				new String[0]));
		link.setBackground(pageBook.getDisplay().getSystemColor(
				SWT.COLOR_LIST_BACKGROUND));
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.FILL)
				.grab(true, false).applyTo(link);
		link.addSelectionListener(onExplanationClicked(connectionsPane, link));
		return form;
	}

	private SelectionAdapter onExplanationClicked(
			final Control connectionsPane, final Control explanationPane) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final NewDockerConnection wizard = new NewDockerConnection();
				final WizardDialog dialog = new WizardDialog(PlatformUI
						.getWorkbench().getModalDialogShellProvider()
						.getShell(), wizard);
				if (dialog.open() == WizardDialog.OK) {
					getCommonViewer().refresh();
				}
				// if a (first) connection is added, the
				// DockerExplorerView#changeEvent(int) method
				// will be called and the pageBook will show the connectionsPane
				// instead of the explanationsPane

			}
		};
	}

	/**
	 * Shows the {@link DockerExplorerView#explanationsPane} or the
	 * {@link DockerExplorerView#connectionsPane} depending on the number of
	 * connections in the {@link DockerConnectionManager}.
	 */
	private void showConnectionsOrExplanations() {
		if (DockerConnectionManager.getInstance().getConnections().length < 1) {
			pageBook.showPage(explanationsPane);
		} else {
			pageBook.showPage(connectionsPane);
		}
	}

	@Override
	public void changeEvent(int type) {
		showConnectionsOrExplanations();
	}

}
