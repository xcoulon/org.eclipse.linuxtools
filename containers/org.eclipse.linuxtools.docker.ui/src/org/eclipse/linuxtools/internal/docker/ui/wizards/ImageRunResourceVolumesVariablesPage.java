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

import java.io.File;
import java.util.Iterator;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.beans.IBeanValueProperty;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.property.Properties;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ObservableMapLabelProvider;
import org.eclipse.jface.databinding.viewers.ViewerSupport;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.linuxtools.docker.core.DockerException;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.internal.docker.ui.SWTImagesFactory;
import org.eclipse.linuxtools.internal.docker.ui.wizards.ImageRunResourceVolumesVariablesModel.MountType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

/**
 * A {@link WizardPage} to let the user select the CPU and memory allocation for
 * the container, as well as the volumes to mount.
 * 
 * @author xcoulon
 *
 */
public class ImageRunResourceVolumesVariablesPage extends WizardPage {

	private final int COLUMNS = 2;

	private final DataBindingContext dbc = new DataBindingContext();
	private final ImageRunResourceVolumesVariablesModel model;

	public ImageRunResourceVolumesVariablesPage(
			final IDockerConnection connection) throws DockerException {
		super("ImageSelectionPage", //$NON-NLS-1$
				"Volumes, Environment Variables and Resource Limitations", //$NON-NLS-1$
				SWTImagesFactory.DESC_BANNER_REPOSITORY);
		setPageComplete(true);
		this.model = new ImageRunResourceVolumesVariablesModel(connection);
	}

	public ImageRunResourceVolumesVariablesModel getModel() {
		return model;
	}

	@Override
	public void createControl(Composite parent) {
		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(COLUMNS).margins(6, 6)
				.applyTo(container);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL)
				.applyTo(container);
		createVolumeSettingsContainer(container);
		// createSectionSeparator(container, true);
		createEnvironmentVariablesContainer(container);
		createSectionSeparator(container, true);
		createResourceSettingsContainer(container);
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

	private void createResourceSettingsContainer(final Composite parent) {
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL)
				.span(COLUMNS, 1).grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().spacing(10, 2).applyTo(container);
		final Button enableResourceLimitationButton = new Button(container,
				SWT.CHECK);
		enableResourceLimitationButton.setText("Enable resource limitations"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.applyTo(enableResourceLimitationButton);
		dbc.bindValue(
				WidgetProperties.selection()
						.observe(enableResourceLimitationButton),
				BeanProperties
						.value(ImageRunResourceVolumesVariablesModel.class,
								ImageRunResourceVolumesVariablesModel.ENABLE_RESOURCE_LIMITATIONS)
						.observe(model));
		final int COLUMNS = 5;
		final int INDENT = 20;
		final Composite subContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL)
				.indent(INDENT, 0).span(COLUMNS, 1).grab(true, false)
				.applyTo(subContainer);
		GridLayoutFactory.fillDefaults().numColumns(COLUMNS).margins(6, 6)
				.spacing(10, 2).applyTo(subContainer);

		// specify CPU limitation
		final Label cpuPriorityLabel = new Label(subContainer, SWT.NONE);
		cpuPriorityLabel.setText("CPU priority:"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).applyTo(cpuPriorityLabel);
		final Button lowCPULimitationButton = new Button(subContainer,
				SWT.RADIO);
		lowCPULimitationButton.setText("Low");
		lowCPULimitationButton.addSelectionListener(
				onCpuShareWeighting(ImageRunResourceVolumesVariablesModel.LOW));
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.applyTo(lowCPULimitationButton);
		final Button mediumCPULimitationButton = new Button(subContainer,
				SWT.RADIO);
		mediumCPULimitationButton.setText("Medium");
		mediumCPULimitationButton.addSelectionListener(onCpuShareWeighting(
				ImageRunResourceVolumesVariablesModel.MEDIUM));
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.applyTo(mediumCPULimitationButton);
		final Button highCPULimitationButton = new Button(subContainer,
				SWT.RADIO);
		mediumCPULimitationButton.setSelection(true);
		highCPULimitationButton.setText("High");
		highCPULimitationButton.addSelectionListener(onCpuShareWeighting(
				ImageRunResourceVolumesVariablesModel.HIGH));
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(2, 1)
				.applyTo(highCPULimitationButton);
		dbc.bindValue(
				WidgetProperties.selection()
						.observe(enableResourceLimitationButton),
				BeanProperties
						.value(ImageRunResourceVolumesVariablesModel.class,
								ImageRunResourceVolumesVariablesModel.ENABLE_RESOURCE_LIMITATIONS)
						.observe(model));

		// Memory limitation
		final Label memoryLimitLabel = new Label(subContainer, SWT.NONE);
		memoryLimitLabel.setText("Memory limit:"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).applyTo(memoryLimitLabel);
		final Scale memoryLimitSpinner = new Scale(subContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).span(2, 1).applyTo(memoryLimitSpinner);
		memoryLimitSpinner.setBackground(
				Display.getDefault().getSystemColor(SWT.COLOR_TRANSPARENT));
		memoryLimitSpinner.setMinimum(0);
		memoryLimitSpinner.setMaximum(this.model.getTotalMemory());
		memoryLimitSpinner.setSelection(512);
		memoryLimitSpinner.setPageIncrement(64);
		dbc.bindValue(WidgetProperties.selection().observe(memoryLimitSpinner),
				BeanProperties
						.value(ImageRunResourceVolumesVariablesModel.class,
								ImageRunResourceVolumesVariablesModel.MEMORY_LIMIT)
						.observe(model));

		final Text memoryLimitValueText = new Text(subContainer, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).hint(50, SWT.DEFAULT)
				.applyTo(memoryLimitValueText);
		dbc.bindValue(
				WidgetProperties.text(SWT.Modify).observe(memoryLimitValueText),
				BeanProperties
						.value(ImageRunResourceVolumesVariablesModel.class,
								ImageRunResourceVolumesVariablesModel.MEMORY_LIMIT)
						.observe(model));
		final Label memoryLimitValueLabel = new Label(subContainer, SWT.NONE);
		memoryLimitValueLabel.setText("MB"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).applyTo(memoryLimitValueLabel);

		// enable/disable controls
		enableResourceLimitationButton
				.addSelectionListener(onEnableResourceLimitation(subContainer));
		toggleResourceLimitationControls(subContainer);

	}

	private SelectionListener onCpuShareWeighting(final int cpuShareWeigth) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				model.setCpuShareWeight(cpuShareWeigth);
			}
		};
	}

	private SelectionListener onEnableResourceLimitation(
			final Composite container) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				toggleResourceLimitationControls(container);
			}

		};
	}

	private void toggleResourceLimitationControls(Composite container) {
		for (Control childControl : container.getChildren()) {
			if (model.isEnableResourceLimitations()) {
				childControl.setEnabled(true);
			} else {
				childControl.setEnabled(false);
			}
		}
	}

	private void createVolumeSettingsContainer(final Composite container) {
		final Label volumesLabel = new Label(container, SWT.NONE);
		volumesLabel.setText("Data Volumes:"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).span(COLUMNS, 1).applyTo(volumesLabel);
		final CheckboxTableViewer dataVolumesTableViewer = createVolumesTable(
				container);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP)
				.grab(true, false).hint(200, 100)
				.applyTo(dataVolumesTableViewer.getTable());
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
		addButton.addSelectionListener(onAddDataVolume(dataVolumesTableViewer));
		final Button editButton = new Button(buttonsContainers, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP)
				.grab(true, false).applyTo(editButton);
		editButton.setText("Edit..."); //$NON-NLS-1$
		editButton
				.addSelectionListener(onEditDataVolume(dataVolumesTableViewer));
		editButton.setEnabled(false);
		final Button removeButton = new Button(buttonsContainers, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP)
				.grab(true, false).applyTo(removeButton);
		removeButton.setText("Remove"); //$NON-NLS-1$
		removeButton.addSelectionListener(
				onRemoveDataVolumes(dataVolumesTableViewer));
		removeButton.setEnabled(false);
		// update table content when selected image changes
		bind(dataVolumesTableViewer, model.getDataVolumes(),
				BeanProperties.values(DataVolumeModel.class,
						new String[] { DataVolumeModel.CONTAINER_PATH,
								DataVolumeModel.MOUNT,
								DataVolumeModel.READ_ONLY_VOLUME }));
		dbc.bindSet(
				ViewersObservables.observeCheckedElements(
						dataVolumesTableViewer, DataVolumeModel.class),
				BeanProperties
						.set(ImageRunResourceVolumesVariablesModel.SELECTED_DATA_VOLUMES)
						.observe(model));
		// disable the edit and removeButton if the table is empty
		dataVolumesTableViewer.addSelectionChangedListener(
				onSelectionChanged(editButton, removeButton));

	}

	/**
	 * Same as
	 * {@link ViewerSupport#bind(StructuredViewer, IObservableList, org.eclipse.core.databinding.property.value.IValueProperty[])
	 * but with a custom LabelProvider, DataVolumesLabelProvider
	 *
	 * @param viewer
	 * @param input
	 * @param labelProperties
	 */
	private void bind(final StructuredViewer viewer,
			final IObservableList input,
			final IBeanValueProperty[] labelProperties) {
		final ObservableListContentProvider contentProvider = new ObservableListContentProvider();
		if (viewer.getInput() != null) {
			viewer.setInput(null);
		}
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(
				new DataVolumesLabelProvider(Properties.observeEach(
						contentProvider.getKnownElements(), labelProperties)));
		if (input != null) {
			viewer.setInput(input);
		}

	}

	private ISelectionChangedListener onSelectionChanged(
			final Button... targetButtons) {
		return new ISelectionChangedListener() {

			@Override
			public void selectionChanged(final SelectionChangedEvent e) {
				if (e.getSelection().isEmpty()) {
					setControlsEnabled(targetButtons, false);
				} else {
					setControlsEnabled(targetButtons, true);
				}
			}

		};
	}

	private SelectionListener onAddDataVolume(
			final CheckboxTableViewer dataVolumesTableViewer) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				final ContainerDataVolumeDialog dialog = new ContainerDataVolumeDialog(
						getShell(), model.getConnection());
				dialog.create();
				if (dialog.open() == IDialogConstants.OK_ID) {
					model.getDataVolumes().add(dialog.getDataVolume());
					dataVolumesTableViewer.refresh();
				}
			}
		};
	}

	private SelectionListener onEditDataVolume(
			final CheckboxTableViewer dataVolumesTableViewer) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				final IStructuredSelection selection = (IStructuredSelection) dataVolumesTableViewer
						.getSelection();
				if (selection.isEmpty()) {
					return;
				}
				final DataVolumeModel selectedDataVolume = (DataVolumeModel) selection
						.getFirstElement();
				final ContainerDataVolumeDialog dialog = new ContainerDataVolumeDialog(
						getShell(), model.getConnection(), selectedDataVolume);
				dialog.create();
				if (dialog.open() == IDialogConstants.OK_ID) {
					final DataVolumeModel dialogDataVolume = dialog
							.getDataVolume();
					selectedDataVolume.setContainerMount(
							dialogDataVolume.getContainerMount());
					selectedDataVolume
							.setMountType(dialogDataVolume.getMountType());
					selectedDataVolume.setHostPathMount(
							dialogDataVolume.getHostPathMount());
					selectedDataVolume.setContainerMount(
							dialogDataVolume.getContainerMount());
					selectedDataVolume
							.setReadOnly(dialogDataVolume.isReadOnly());
				}
			}
		};
	}

	private SelectionListener onRemoveDataVolumes(
			final TableViewer dataVolumesTableViewer) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				final IStructuredSelection selection = dataVolumesTableViewer
						.getStructuredSelection();
				for (@SuppressWarnings("unchecked")
				Iterator<DataVolumeModel> iterator = selection
						.iterator(); iterator.hasNext();) {
					model.removeDataVolume(iterator.next());
				}

			}
		};
	}

	private CheckboxTableViewer createVolumesTable(final Composite container) {
		final Table table = new Table(container, SWT.CHECK | SWT.BORDER
				| SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		final CheckboxTableViewer tableViewer = new CheckboxTableViewer(table);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		dbc.bindSet(
				ViewersObservables.observeCheckedElements(tableViewer,
						DataVolumeModel.class),
				BeanProperties
						.set(ImageRunResourceVolumesVariablesModel.SELECTED_DATA_VOLUMES)
						.observe(model));
		addTableViewerColum(tableViewer, "Container Path", //$NON-NLS-1$
				150);
		addTableViewerColum(tableViewer, "Mount", //$NON-NLS-1$
				150);
		addTableViewerColum(tableViewer, "Read-only", //$NON-NLS-1$
				60);
		return tableViewer;
	}

	private TableViewerColumn addTableViewerColum(final TableViewer tableViewer,
			final String title, final int width) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(
				tableViewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		if (title != null) {
			column.setText(title);
		}
		column.setWidth(width);
		return viewerColumn;
	}

	private void createEnvironmentVariablesContainer(
			final Composite container) {
		final Label volumesLabel = new Label(container, SWT.NONE);
		volumesLabel.setText("Environment variables:"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).span(COLUMNS, 1).applyTo(volumesLabel);
		final TableViewer environmentVariablesTableViewer = createEnvironmentVariablesTable(
				container);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP)
				.grab(true, false).hint(200, 100)
				.applyTo(environmentVariablesTableViewer.getTable());
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
		addButton.setEnabled(true);
		addButton.addSelectionListener(onAddEnvironmentVariable());
		final Button editButton = new Button(buttonsContainers, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP)
				.grab(true, false).applyTo(editButton);
		editButton.setText("Edit..."); //$NON-NLS-1$
		editButton.setEnabled(true);
		editButton.addSelectionListener(
				onEditEnvironmentVariable(environmentVariablesTableViewer));
		editButton.setEnabled(false);
		final Button removeButton = new Button(buttonsContainers, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP)
				.grab(true, false).applyTo(removeButton);
		removeButton.setText("Remove"); //$NON-NLS-1$
		removeButton.addSelectionListener(
				onRemoveEnvironmentVariables(environmentVariablesTableViewer));
		removeButton.setEnabled(false);
		// update table content when selected image changes
		ViewerSupport.bind(environmentVariablesTableViewer,
				model.getEnvironmentVariables(),
				BeanProperties.values(EnvironmentVariableModel.class,
						new String[] { EnvironmentVariableModel.NAME,
								EnvironmentVariableModel.VALUE }));
		// disable the edit and removeButton if the table is empty
		environmentVariablesTableViewer.addSelectionChangedListener(
				onSelectionChanged(editButton, removeButton));
	}

	private TableViewer createEnvironmentVariablesTable(Composite container) {
		final Table table = new Table(container,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		final TableViewer tableViewer = new TableViewer(table);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		addTableViewerColum(tableViewer, "Name", //$NON-NLS-1$
				200);
		addTableViewerColum(tableViewer, "Value", //$NON-NLS-1$
				200);
		return tableViewer;
	}

	private SelectionListener onAddEnvironmentVariable() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final ContainerEnvironmentVariableDialog dialog = new ContainerEnvironmentVariableDialog(
						getShell());
				dialog.create();
				if (dialog.open() == IDialogConstants.OK_ID) {
					model.getEnvironmentVariables()
							.add(dialog.getEnvironmentVariable());

				}
			}
		};
	}

	private SelectionListener onEditEnvironmentVariable(
			final TableViewer environmentVariablesTableViewer) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final EnvironmentVariableModel selectedVariable = (EnvironmentVariableModel) environmentVariablesTableViewer
						.getStructuredSelection().getFirstElement();
				final ContainerEnvironmentVariableDialog dialog = new ContainerEnvironmentVariableDialog(
						getShell(), selectedVariable);
				dialog.create();
				if (dialog.open() == IDialogConstants.OK_ID) {
					selectedVariable.setName(
							dialog.getEnvironmentVariable().getName());
					selectedVariable.setValue(
							dialog.getEnvironmentVariable().getValue());
					environmentVariablesTableViewer.refresh();
				}
			}
		};
	}

	private SelectionListener onRemoveEnvironmentVariables(
			final TableViewer linksTableViewer) {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				final IStructuredSelection selection = linksTableViewer
						.getStructuredSelection();
				for (@SuppressWarnings("unchecked")
				Iterator<EnvironmentVariableModel> iterator = selection
						.iterator(); iterator.hasNext();) {
					model.removeEnvironmentVariable(iterator.next());
				}
			}
		};
	}

	private static void setControlsEnabled(final Control[] controls,
			final boolean enabled) {
		for (Control control : controls) {
			control.setEnabled(enabled);
		}
	}

	private static final class DataVolumesLabelProvider
			extends ObservableMapLabelProvider {

		public DataVolumesLabelProvider(final IObservableMap[] attributeMaps) {
			super(attributeMaps);
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			final DataVolumeModel dataVolume = ((DataVolumeModel) element);
			if (dataVolume.getMountType() != null && columnIndex == 1) {
				switch (dataVolume.getMountType()) {
				case CONTAINER:
					return SWTImagesFactory.DESC_CONTAINER.createImage();
				case HOST_FILE_SYSTEM:
					final File hostFile = new File(dataVolume.getMount());
					if (!hostFile.exists() || hostFile.isDirectory()) {
						return SWTImagesFactory.DESC_FOLDER_CLOSED
								.createImage();
					} else {
						return SWTImagesFactory.DESC_FILE.createImage();
					}
				default:
					return null;
				}
			}
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			final DataVolumeModel dataVolume = ((DataVolumeModel) element);
			switch (columnIndex) {
			case 0:
				return dataVolume.getContainerPath();
			case 1:
				return dataVolume.getMount();
			case 2:
				if (dataVolume.getMountType() != MountType.HOST_FILE_SYSTEM) {
					return null;
				} else if (dataVolume.isReadOnly()) {
					return "true";
				}
				return "false";
			default:
				return null;
			}
		}
	}

}
