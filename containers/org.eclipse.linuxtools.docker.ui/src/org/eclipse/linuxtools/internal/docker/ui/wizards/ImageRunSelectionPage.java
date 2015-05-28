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

package org.eclipse.linuxtools.internal.docker.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerSupport;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.linuxtools.docker.core.DockerException;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.core.IDockerImage;
import org.eclipse.linuxtools.docker.core.IDockerImageInfo;
import org.eclipse.linuxtools.docker.core.IDockerImageSearchResult;
import org.eclipse.linuxtools.docker.ui.Activator;
import org.eclipse.linuxtools.internal.docker.ui.SWTImagesFactory;
import org.eclipse.linuxtools.internal.docker.ui.commands.CommandUtils;
import org.eclipse.linuxtools.internal.docker.ui.utils.IRunnableWithResult;
import org.eclipse.linuxtools.internal.docker.ui.views.DVMessages;
import org.eclipse.linuxtools.internal.docker.ui.views.ImagePullProgressHandler;
import org.eclipse.linuxtools.internal.docker.ui.wizards.ImageRunSelectionModel.ContainerLinkModel;
import org.eclipse.linuxtools.internal.docker.ui.wizards.ImageRunSelectionModel.ExposedPortModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

/**
 * A {@link WizardPage} to let the user select the {@link IDockerImage} to run
 * and select the most common arguments (container name, port settings, etc.)
 * 
 * @author xcoulon
 *
 */
public class ImageRunSelectionPage extends WizardPage {

	private static final int INDENT = 30;

	private static final String ERROR_PULLING_IMAGE = "ImagePullError.msg"; //$NON-NLS-1$

	private final DataBindingContext dbc = new DataBindingContext();
	private final ImageRunSelectionModel model;
	private CheckboxTableViewer exposedPortsTableViewer;

	private static final int COLUMNS = 3;

	/**
	 * Default constructor.
	 * 
	 * @param selectedImage
	 *            the {@link IDockerImage} to run
	 * 
	 */
	public ImageRunSelectionPage(final IDockerImage selectedImage) {
		super("ImageSelectionPage", "Docker Container settings", //$NON-NLS-1$ //$NON-NLS-2$
				SWTImagesFactory.DESC_BANNER_REPOSITORY);
		setMessage("Run a Docker Image"); //$NON-NLS-1$
		setPageComplete(true);
		this.model = new ImageRunSelectionModel(selectedImage);
	}

	/**
	 * Default constructor.
	 * 
	 * @param selectedConnection
	 *            the {@link IDockerConnection} to run
	 * 
	 */
	public ImageRunSelectionPage(final IDockerConnection selectedConnection) {
		super("ImageSelectionPage", //$NON-NLS-1$
				"Image Selection and Exposed Port Publishing", //$NON-NLS-1$
				SWTImagesFactory.DESC_BANNER_REPOSITORY);
		setMessage("Select the Docker Image to run and the ports to expose"); //$NON-NLS-1$
		setPageComplete(false);
		this.model = new ImageRunSelectionModel(selectedConnection);
	}

	/**
	 * @return the {@link ImageRunSelectionModel} model associated
	 */
	public ImageRunSelectionModel getModel() {
		return model;
	}

	@Override
	public void dispose() {
		dbc.dispose();
		super.dispose();
	}

	@Override
	public void createControl(final Composite parent) {
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).span(1, 1)
				.grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(COLUMNS).margins(6, 6)
				.applyTo(container);
		createImageSettingsSection(container);
		createSectionSeparator(container, true);
		createPortSettingsSection(container);
		// addSectionSeparator(container, false);
		createLinkSettingsSection(container);
		// addSectionSeparator(container, false);
		createRunOptionsSection(container);
		// Observe model changes to propagate to the UI via listeners.
		final IObservableValue imageSelectionObservable = BeanProperties
				.value(ImageRunSelectionModel.class,
						ImageRunSelectionModel.SELECTED_IMAGE_NAME)
				.observe(model);
		imageSelectionObservable
				.addValueChangeListener(onImageSelectionChange());
		setDefaultValues();
		// setup validation support
		WizardPageSupport.create(this, dbc);
		// set validation
		final ImageSelectionValidator imageSelectionValidator = new ImageSelectionValidator(
				imageSelectionObservable);
		dbc.addValidationStatusProvider(imageSelectionValidator);
		//
		setControl(container);
	}

	private void createSectionSeparator(final Composite container,
			final boolean separator) {
		final int SECTION_INDENT = 10;
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.span(COLUMNS, 1).grab(true, false)
				.indent(SWT.DEFAULT, SECTION_INDENT)
				.applyTo(new Label(container, separator
						? (SWT.SEPARATOR | SWT.HORIZONTAL) : SWT.NONE));
	}

	/**
	 * Creates the {@link Composite} container that will display widgets to
	 * select an {@link IDockerImage}, name it and specify the command to run.
	 * 
	 * @param container
	 *            the parent {@link Composite}
	 */
	private void createImageSettingsSection(final Composite container) {
		// Image selection name
		final Label imageSelectionLabel = new Label(container, SWT.NONE);
		imageSelectionLabel.setText("Image:"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).applyTo(imageSelectionLabel);
		final Combo imageSelectionCombo = new Combo(container, SWT.BORDER);
		final ComboViewer imageSelectionComboViewer = new ComboViewer(
				imageSelectionCombo);
		imageSelectionCombo.setToolTipText("Select the Docker Image to run"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).span(1, 1).applyTo(imageSelectionCombo);
		new ControlDecoration(imageSelectionCombo, SWT.TOP | SWT.LEFT);
		new ContentProposalAdapter(imageSelectionCombo,
				new ComboContentAdapter() {
					@Override
					public void insertControlContents(Control control,
							String text, int cursorPosition) {
						final Combo combo = (Combo) control;
						final Point selection = combo.getSelection();
						combo.setText(text);
						selection.x = text.length();
						selection.y = selection.x;
						combo.setSelection(selection);
					}
				}, getImageNameContentProposalProvider(imageSelectionCombo),
				null, null);
		// image search
		final Button searchImageButton = new Button(container, SWT.NONE);
		searchImageButton.setText("Search..."); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).span(1, 1).applyTo(searchImageButton);
		searchImageButton.addSelectionListener(onSearchImage());
		// link to pull image
		final Label fillerLabel = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).span(1, 1).applyTo(fillerLabel);
		final Link pullImageLink = new Link(container, SWT.NONE);
		pullImageLink.setText("<a>Pull this image...</a>");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).span(COLUMNS - 1, 1).applyTo(pullImageLink);
		pullImageLink.addSelectionListener(onPullImage());
		// bind combo with model (for values and selection)
		imageSelectionComboViewer
				.setContentProvider(new ObservableListContentProvider());
		dbc.bindList(WidgetProperties.items().observe(imageSelectionCombo),
				BeanProperties
						.list(ImageRunSelectionModel.class,
								ImageRunSelectionModel.IMAGE_NAMES)
						.observe(model));
		dbc.bindValue(WidgetProperties.selection().observe(imageSelectionCombo),
				BeanProperties
						.value(ImageRunSelectionModel.class,
								ImageRunSelectionModel.SELECTED_IMAGE_NAME)
						.observe(model));
		// Container name (optional)
		final Label containerNameLabel = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).applyTo(imageSelectionLabel);
		containerNameLabel.setText("Name:"); //$NON-NLS-1$
		final Text containerNameText = new Text(container, SWT.BORDER);
		containerNameText.setToolTipText(
				"a UUID long identifier, a UUID short identifier or a String"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).span(1, 1).applyTo(containerNameText);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).span(1, 1)
				.applyTo(new Label(container, SWT.NONE));
		dbc.bindValue(
				WidgetProperties.text(SWT.Modify).observe(containerNameText),
				BeanProperties
						.value(ImageRunSelectionModel.class,
								ImageRunSelectionModel.CONTAINER_NAME)
						.observe(model));

		// EntryPoint (optional)
		final Label entrypointLabel = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).applyTo(imageSelectionLabel);
		entrypointLabel.setText("Entrypoint:"); //$NON-NLS-1$
		// TODO: include SWT.SEARCH | SWT.ICON_SEARCH to support value reset
		final Text entrypointText = new Text(container, SWT.BORDER);

		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).span(1, 1).applyTo(entrypointText);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).span(1, 1)
				.applyTo(new Label(container, SWT.NONE));
		dbc.bindValue(WidgetProperties.text(SWT.Modify).observe(entrypointText),
				BeanProperties
						.value(ImageRunSelectionModel.class,
								ImageRunSelectionModel.ENTRYPOINT)
						.observe(model));

		// Command (optional)
		final Label commandLabel = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).applyTo(imageSelectionLabel);
		commandLabel.setText("Command:"); //$NON-NLS-1$
		final Text commandText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).span(1, 1).applyTo(commandText);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).span(1, 1)
				.applyTo(new Label(container, SWT.NONE));
		dbc.bindValue(WidgetProperties.text(SWT.Modify).observe(commandText),
				BeanProperties.value(ImageRunSelectionModel.class,
						ImageRunSelectionModel.COMMAND).observe(model));
	}

	private void createPortSettingsSection(final Composite container) {
		final Button publishAllPortsButton = new Button(container, SWT.CHECK);
		publishAllPortsButton.setText(
				"Publish all exposed ports to random ports on the host interfaces"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.span(COLUMNS, 1).grab(true, false)
				.applyTo(publishAllPortsButton);
		dbc.bindValue(
				WidgetProperties.selection().observe(publishAllPortsButton),
				BeanProperties
						.value(ImageRunSelectionModel.class,
								ImageRunSelectionModel.PUBLISH_ALL_PORTS)
						.observe(model));
		// specify ports
		final Label portSettingsLabel = new Label(container, SWT.NONE);
		portSettingsLabel.setText(
				"Only publish the selected container ports below to the host:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).span(COLUMNS, 1).indent(INDENT, 0)
				.applyTo(portSettingsLabel);
		exposedPortsTableViewer = createPortSettingsTable(container);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP)
				.grab(true, false).span(COLUMNS - 1, 1).indent(INDENT, 0)
				.hint(200, 70).applyTo(exposedPortsTableViewer.getTable());
		// buttons
		final Composite buttonsContainers = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP)
				.grab(false, false).applyTo(buttonsContainers);
		GridLayoutFactory.fillDefaults().numColumns(1).margins(0, 0)
				.spacing(SWT.DEFAULT, 0).applyTo(buttonsContainers);

		final Button addButton = new Button(buttonsContainers, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP)
				.grab(true, false).applyTo(addButton);
		addButton.setText("Add"); //$NON-NLS-1$
		addButton.addSelectionListener(onAddPort());
		final Button editButton = new Button(buttonsContainers, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP)
				.grab(true, false).applyTo(editButton);
		editButton.setText("Edit..."); //$NON-NLS-1$
		editButton.setEnabled(false);
		editButton.addSelectionListener(onEditPort(exposedPortsTableViewer));
		final Button removeButton = new Button(buttonsContainers, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP)
				.grab(true, false).applyTo(removeButton);
		removeButton.setText("Remove"); //$NON-NLS-1$
		removeButton
				.addSelectionListener(onRemovePorts(exposedPortsTableViewer));
		BeanProperties
				.value(ImageRunSelectionModel.class,
						ImageRunSelectionModel.PUBLISH_ALL_PORTS)
				.observe(model)
				.addValueChangeListener(onPublishAllPortsChange(
						exposedPortsTableViewer.getTable(), addButton,
						removeButton));
		ViewerSupport.bind(exposedPortsTableViewer, model.getExposedPorts(),
				BeanProperties.values(ExposedPortModel.class,
						new String[] { ExposedPortModel.CONTAINER_PORT,
								ExposedPortModel.PORT_TYPE,
								ExposedPortModel.HOST_ADDRESS,
								ExposedPortModel.HOST_PORT }));
		dbc.bindSet(
				ViewersObservables.observeCheckedElements(
						exposedPortsTableViewer, ExposedPortModel.class),
				BeanProperties.set(ImageRunSelectionModel.SELECTED_PORTS)
						.observe(model));

		togglePortMappingControls(exposedPortsTableViewer.getTable(), addButton,
				removeButton);
	}

	private CheckboxTableViewer createPortSettingsTable(
			final Composite container) {
		final Table table = new Table(container, SWT.BORDER | SWT.FULL_SELECTION
				| SWT.V_SCROLL | SWT.H_SCROLL | SWT.CHECK);
		final CheckboxTableViewer tableViewer = new CheckboxTableViewer(table);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		createTableViewerColum(tableViewer, "Container Port", //$NON-NLS-1$
				100);
		createTableViewerColum(tableViewer, "Type", //$NON-NLS-1$
				50);
		createTableViewerColum(tableViewer, "Host Address", //$NON-NLS-1$
				100);
		createTableViewerColum(tableViewer, "Host Port", //$NON-NLS-1$
				100);
		tableViewer.setContentProvider(new ObservableListContentProvider());
		return tableViewer;
	}

	private TableViewerColumn createTableViewerColum(
			final TableViewer tableViewer, final String title,
			final int width) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(
				tableViewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		if (title != null) {
			column.setText(title);
		}
		column.setWidth(width);
		return viewerColumn;
	}

	private void createLinkSettingsSection(final Composite container) {
		final Label linksLabel = new Label(container, SWT.NONE);
		linksLabel.setText("Links to other containers:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).span(COLUMNS, 1).applyTo(linksLabel);
		final TableViewer linksTableViewer = createLinksTable(container);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).grab(true, true)
				.span(COLUMNS - 1, 1).hint(200, 50).indent(INDENT, SWT.DEFAULT)
				.applyTo(linksTableViewer.getTable());
		// buttons
		final Composite buttonsContainers = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP)
				.grab(false, false).applyTo(buttonsContainers);
		GridLayoutFactory.fillDefaults().numColumns(1).margins(0, 0)
				.spacing(SWT.DEFAULT, 0).applyTo(buttonsContainers);

		final Button addButton = new Button(buttonsContainers, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP)
				.grab(true, false).applyTo(addButton);
		addButton.setText("Add..."); //$NON-NLS-1$
		addButton.addSelectionListener(onAddLink());
		final Button editButton = new Button(buttonsContainers, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP)
				.grab(true, false).applyTo(editButton);
		editButton.setText("Edit..."); //$NON-NLS-1$
		editButton.setEnabled(false);
		editButton.addSelectionListener(onEditLink(linksTableViewer));
		final Button removeButton = new Button(buttonsContainers, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP)
				.grab(true, false).applyTo(removeButton);
		removeButton.setText("Remove"); //$NON-NLS-1$
		removeButton.addSelectionListener(onRemoveLinks(linksTableViewer));
		removeButton.setEnabled(false);
		ViewerSupport
				.bind(linksTableViewer, model.getLinks(),
						BeanProperties.values(ContainerLinkModel.class,
								new String[] {
										ContainerLinkModel.CONTAINER_NAME,
										ContainerLinkModel.CONTAINER_ALIAS }));
		linksTableViewer.addSelectionChangedListener(
				onLinkSelected(editButton, removeButton));
		// disable the edit and removeButton if the table is empty
		model.getLinks()
				.addChangeListener(onRemoveLinks(editButton, removeButton));
	}

	private ISelectionChangedListener onLinkSelected(
			final Control... controls) {
		return new ISelectionChangedListener() {

			@Override
			public void selectionChanged(final SelectionChangedEvent e) {
				if (e.getSelection().isEmpty()) {
					setControlsEnabled(controls, false);
				} else {
					setControlsEnabled(controls, true);
				}
			}

		};
	}

	private TableViewer createLinksTable(final Composite container) {
		final Table table = new Table(container,
				SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		final TableViewer tableViewer = new TableViewer(table);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		createTableViewerColum(tableViewer, "Container Name", //$NON-NLS-1$
				200);
		createTableViewerColum(tableViewer, "Alias", //$NON-NLS-1$
				150);
		return tableViewer;
	}

	private SelectionListener onAddLink() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				final ContainerLinkDialog dialog = new ContainerLinkDialog(
						getShell(), model.getSelectedConnection());
				dialog.create();
				if (dialog.open() == IDialogConstants.OK_ID) {
					model.addLink(dialog.getContainerName(),
							dialog.getContainerAlias());
				}
			}
		};
	}

	private SelectionListener onEditLink(final TableViewer linksTableViewer) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				final IStructuredSelection selection = linksTableViewer
						.getStructuredSelection();

				final ContainerLinkModel selectedContainerLink = (ContainerLinkModel) selection
						.getFirstElement();
				final ContainerLinkDialog dialog = new ContainerLinkDialog(
						getShell(), model.getSelectedConnection(),
						selectedContainerLink);
				dialog.create();
				if (dialog.open() == IDialogConstants.OK_ID) {
					selectedContainerLink
							.setContainerName(dialog.getContainerName());
					selectedContainerLink
							.setContainerAlias(dialog.getContainerAlias());
					linksTableViewer.refresh();
				}
			}
		};
	}

	private IChangeListener onRemoveLinks(final Button... buttons) {
		return new IChangeListener() {

			@Override
			public void handleChange(final ChangeEvent event) {
				@SuppressWarnings("unchecked")
				final List<ContainerLinkModel> links = (List<ContainerLinkModel>) event
						.getSource();
				if (links.isEmpty()) {
					setControlsEnabled(buttons, false);
				}
			}
		};
	}

	private SelectionListener onRemoveLinks(
			final TableViewer linksTableViewer) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				final IStructuredSelection selection = linksTableViewer
						.getStructuredSelection();
				for (@SuppressWarnings("unchecked")
				Iterator<ContainerLinkModel> iterator = selection
						.iterator(); iterator.hasNext();) {
					model.removeLink(iterator.next());
				}
			}
		};
	}

	private void createRunOptionsSection(final Composite container) {
		// interactive/show in console mode
		final Button interactiveButton = new Button(container, SWT.CHECK);
		interactiveButton.setText(
				"Keep STDIN open to Console even if not attached (-i)"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.span(COLUMNS, 1).grab(true, false).applyTo(interactiveButton);
		dbc.bindValue(WidgetProperties.selection().observe(interactiveButton),
				BeanProperties
						.value(ImageRunSelectionModel.class,
								ImageRunSelectionModel.INTERACTIVE_MODE)
						.observe(model));
		// allocate pseudo-TTY
		final Button allocatePseudoTTY = new Button(container, SWT.CHECK);
		allocatePseudoTTY.setText("Allocate pseudo-TTY from Console (-t)"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.span(COLUMNS, 1).grab(true, false).applyTo(allocatePseudoTTY);
		dbc.bindValue(WidgetProperties.selection().observe(allocatePseudoTTY),
				BeanProperties
						.value(ImageRunSelectionModel.class,
								ImageRunSelectionModel.ALLOCATE_PSEUDO_TTY)
						.observe(model));

		// remove when exits
		final Button removeWhenExitsButton = new Button(container, SWT.CHECK);
		removeWhenExitsButton.setText(
				"Automatically remove the container when it exits (--rm)"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.span(COLUMNS, 1).grab(true, false)
				.applyTo(removeWhenExitsButton);
		dbc.bindValue(
				WidgetProperties.selection().observe(removeWhenExitsButton),
				BeanProperties
						.value(ImageRunSelectionModel.class,
								ImageRunSelectionModel.REMOVE_WHEN_EXITS)
						.observe(model));
	}

	/**
	 * Creates an {@link IContentProposalProvider} to propose
	 * {@link IDockerImage} names based on the current text.
	 * 
	 * @param items
	 * @return
	 */
	private IContentProposalProvider getImageNameContentProposalProvider(
			final Combo imageSelectionCombo) {
		return new IContentProposalProvider() {

			@Override
			public IContentProposal[] getProposals(final String contents,
					final int position) {
				final List<IContentProposal> proposals = new ArrayList<IContentProposal>();
				for (String imageName : imageSelectionCombo.getItems()) {
					if (imageName.contains(contents)) {
						proposals.add(new ContentProposal(imageName, imageName,
								imageName, position));
					}
				}
				return proposals.toArray(new IContentProposal[0]);
			}
		};
	}

	private IValueChangeListener onImageSelectionChange() {
		return new IValueChangeListener() {

			@Override
			public void handleValueChange(final ValueChangeEvent event) {
				final IDockerImage selectedImage = model.getSelectedImage();
				// skip if the selected image does not exist in the local Docker
				// host
				if (selectedImage == null) {
					model.setExposedPorts(new WritableList());
					return;
				}
				findImageInfo(selectedImage);
			}
		};
	}

	private SelectionListener onPullImage() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				pullSelectedImage();
			}
		};
	}

	private IValueChangeListener onPublishAllPortsChange(
			final Control... controls) {
		return new IValueChangeListener() {

			@Override
			public void handleValueChange(final ValueChangeEvent event) {
				togglePortMappingControls(controls);
			}
		};
	}

	private SelectionListener onSearchImage() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				final ImageSearch imageSearchWizard = new ImageSearch(
						ImageRunSelectionPage.this.model
								.getSelectedConnection());
				final boolean completed = CommandUtils
						.openWizard(imageSearchWizard, getShell());
				if (completed) {
					final IDockerImageSearchResult selectedSearchImage = imageSearchWizard
							.getSelectedImage();
					if (selectedSearchImage.getName().contains(":")) {
						model.setSelectedImageName(
								selectedSearchImage.getName());
					} else {
						// assume tag is 'latest'
						model.setSelectedImageName(
								selectedSearchImage.getName() + ":latest"); //$NON-NLS-1$
					}
				}
			}
		};
	}

	private SelectionListener onAddPort() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final ContainerPortDialog dialog = new ContainerPortDialog(
						getShell());
				dialog.create();
				if (dialog.open() == IDialogConstants.OK_ID) {
					model.addAvailablePort(dialog.getPort());
				}
			}
		};
	}

	private SelectionListener onEditPort(final TableViewer portsTableViewer) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final IStructuredSelection selection = portsTableViewer
						.getStructuredSelection();
				final ExposedPortModel selectedContainerPort = (ExposedPortModel) selection
						.getFirstElement();
				final ContainerPortDialog dialog = new ContainerPortDialog(
						getShell(), selectedContainerPort);
				dialog.create();
				if (dialog.open() == IDialogConstants.OK_ID) {
					final ExposedPortModel configuredPort = dialog.getPort();
					selectedContainerPort.setContainerPort(
							configuredPort.getContainerPort());
					selectedContainerPort
							.setHostAddress(configuredPort.getHostAddress());
					selectedContainerPort
							.setHostPort(configuredPort.getHostPort());
					portsTableViewer.refresh();
				}
			}
		};
	}

	private SelectionListener onRemovePorts(
			final TableViewer portsTableViewer) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				final IStructuredSelection selection = portsTableViewer
						.getStructuredSelection();
				for (@SuppressWarnings("unchecked")
				Iterator<ExposedPortModel> iterator = selection
						.iterator(); iterator.hasNext();) {
					model.removeAvailablePort(iterator.next());
				}
			}
		};
	}

	private void setDefaultValues() {
		final IDockerImage selectedImage = model.getSelectedImage();
		if (selectedImage == null) {
			return;
		}
		findImageInfo(selectedImage);
	}

	private void findImageInfo(final IDockerImage selectedImage) {
		try {
			final FindImageInfoRunnable findImageInfoRunnable = new FindImageInfoRunnable(
					selectedImage);
			getContainer().run(true, true, findImageInfoRunnable);
			final IDockerImageInfo selectedImageInfo = findImageInfoRunnable
					.getResult();
			final Set<String> exposedPorts = selectedImageInfo.config()
					.exposedPorts();
			final WritableList availablePorts = new WritableList();
			if (exposedPorts != null) {
				for (String exposedPort : exposedPorts) {
					final String privatePort = exposedPort.substring(0,
							exposedPort.indexOf('/'));
					final String type = exposedPort
							.substring(exposedPort.indexOf('/'));
					availablePorts.add(
							new ExposedPortModel(privatePort, type, "", ""));
				}
			}
			model.setExposedPorts(availablePorts);
			model.setCommand(selectedImageInfo.config().cmd());
			model.setEntrypoint(selectedImageInfo.config().entrypoint());

		} catch (InvocationTargetException | InterruptedException e) {
			Activator.log(e);
		}
	}

	private void togglePortMappingControls(final Control... controls) {
		for (Control control : controls) {
			if (model.isPublishAllPorts()) {
				control.setEnabled(false);
			} else {
				control.setEnabled(true);
			}
		}
	}

	private void pullSelectedImage() {
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(final IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					final IDockerConnection connection = model
							.getSelectedConnection();
					final String imageName = model.getSelectedImageName();
					monitor.beginTask("Pulling image '" + imageName + "'", 1);
					try {
						connection.pullImage(imageName,
								new ImagePullProgressHandler(connection,
										imageName));
					} catch (final DockerException e) {
						Display.getDefault().syncExec(new Runnable() {
							@Override
							public void run() {
								MessageDialog.openError(
										Display.getCurrent().getActiveShell(),
										DVMessages.getFormattedString(
												ERROR_PULLING_IMAGE, imageName),
										e.getMessage());
							}
						});
					} finally {
						monitor.done();
						// refresh the widgets
						model.refreshImageNames();
						if (model.getImageNames().contains(imageName)) {
							model.setSelectedImageName(imageName);
						}
					}
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			Activator.log(e);
		}
	}

	private static void setControlsEnabled(final Control[] controls,
			final boolean enabled) {
		for (Control control : controls) {
			control.setEnabled(enabled);
		}
	}

	private static final class FindImageInfoRunnable
			implements IRunnableWithResult<IDockerImageInfo> {
		private final IDockerImage selectedImage;
		private IDockerImageInfo selectedImageInfo;

		private FindImageInfoRunnable(IDockerImage selectedImage) {
			this.selectedImage = selectedImage;
		}

		@Override
		public void run(final IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			selectedImageInfo = selectedImage.getConnection()
					.getImageInfo(selectedImage.id());
		}

		@Override
		public IDockerImageInfo getResult() {
			return selectedImageInfo;
		}
	}

	private class ImageSelectionValidator extends MultiValidator {

		private final IObservableValue imageSelectionObservable;

		ImageSelectionValidator(
				final IObservableValue imageSelectionObservable) {
			this.imageSelectionObservable = imageSelectionObservable;
		}

		@Override
		protected IStatus validate() {
			final String selectedImageName = (String) imageSelectionObservable
					.getValue();
			if (selectedImageName.isEmpty()) {
				return ValidationStatus
						.error("Please specify the image to run."); //$NON-NLS-1$
			}
			if (model.getSelectedImage() != null) {
				return ValidationStatus.ok();
			}
			return ValidationStatus.warning("Image named '" + selectedImageName
					+ "' does not exist locally. Click on the link below the 'Image' combo to to pull it."); //$NON-NLS-1$
		}

		@Override
		public IObservableList getTargets() {
			WritableList targets = new WritableList();
			targets.add(imageSelectionObservable);
			return targets;
		}

	}

}
