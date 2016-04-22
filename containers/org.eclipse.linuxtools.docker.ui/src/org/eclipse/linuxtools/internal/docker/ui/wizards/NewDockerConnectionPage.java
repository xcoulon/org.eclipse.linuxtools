/*******************************************************************************
 * Copyright (c) 2014, 2016 Red Hat.
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
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
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
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.linuxtools.docker.core.DockerConnectionManager;
import org.eclipse.linuxtools.docker.core.DockerException;
import org.eclipse.linuxtools.docker.core.IDockerRuntime;
import org.eclipse.linuxtools.docker.ui.Activator;
import org.eclipse.linuxtools.internal.docker.core.DockerConnection;
import org.eclipse.linuxtools.internal.docker.ui.SWTImagesFactory;
import org.eclipse.linuxtools.internal.docker.ui.preferences.PreferenceConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.spotify.docker.client.DockerCertificateException;

/**
 * {@link WizardPage} to input the settings to connect to a Docker daemon
 * (running natively or in a VM).
 *
 */
public class NewDockerConnectionPage extends WizardPage {

	private static final String DOCKER_MACHINE_PREFERENCE_PAGE_ID = "org.eclipse.linuxtools.docker.ui.preferences.DockerMachinePreferencePage"; //$NON-NLS-1$

	private final DataBindingContext dbc;

	private final NewDockerConnectionPageModel model;

	/**
	 * Constructor.
	 */
	public NewDockerConnectionPage() {
		super("NewDockerConnectionPage", //$NON-NLS-1$
				WizardMessages.getString("NewDockerConnectionPage.title"), //$NON-NLS-1$
				SWTImagesFactory.DESC_BANNER_REPOSITORY);
		setMessage(WizardMessages.getString("NewDockerConnectionPage.msg")); //$NON-NLS-1$
		this.model = new NewDockerConnectionPageModel();
		this.dbc = new DataBindingContext();
	}

	@Override
	public void createControl(final Composite parent) {
		final ScrolledComposite scrollTop = new ScrolledComposite(parent,
				SWT.H_SCROLL | SWT.V_SCROLL);
		scrollTop.setExpandVertical(true);
		scrollTop.setExpandHorizontal(true);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(scrollTop);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL)
				.grab(true, true).applyTo(scrollTop);

		final Composite container = new Composite(scrollTop, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL)
				.applyTo(container);
		createConnectionSettingsContainer(container);
		scrollTop.setContent(container);
		Point point = container.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		scrollTop.setSize(point);
		scrollTop.setMinSize(point);
		setControl(container);
		// attach the Databinding context status to this wizard page.
		WizardPageSupport.create(this, this.dbc);
		// retrieveDefaultConnectionSettings();
		retrieveExistingInstances();
	}

	@Override
	public void dispose() {
		if (dbc != null) {
			dbc.dispose();
		}
		super.dispose();
	}

	/**
	 * Creates the connection settings container, where the user can choose an
	 * existing VM or specifies custom connection settings.
	 * 
	 * @param parent
	 *            the parent container
	 */
	private void createConnectionSettingsContainer(final Composite parent) {
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).span(1, 1)
				.grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().margins(6, 6).spacing(10, 10)
				.applyTo(container);

		// Table of existing instances (native and Docker machines)
		final Button useExistingInstancesButton = new Button(container,
				SWT.RADIO);
		useExistingInstancesButton.setText(WizardMessages.getString(
				"NewDockerConnectionPage.selectExistingInstanceLabel")); //$NON-NLS-1$
		createExistingInstancesTable(container);
		// specify custom settings
		final Button specifyCustomSettingsButton = new Button(container,
				SWT.RADIO);
		specifyCustomSettingsButton.setText(WizardMessages
				.getString("NewDockerConnectionPage.customSettingsLabel")); //$NON-NLS-1$
		// externalize
		createCustomSettingsContainer(container);
		// bind controls to model
		@SuppressWarnings("unchecked")
		final IObservableValue<Boolean> customConnectionSettingsModelObservable = BeanProperties
				.value(NewDockerConnectionPageModel.class,
						NewDockerConnectionPageModel.USE_CUSTOM_SETTINGS)
				.observe(model);
		dbc.bindValue(
				WidgetProperties.selection()
						.observe(useExistingInstancesButton),
				customConnectionSettingsModelObservable,
				new OppositeBooleanValueStrategy(),
				new OppositeBooleanValueStrategy());
		dbc.bindValue(
				WidgetProperties.selection()
						.observe(specifyCustomSettingsButton),
				customConnectionSettingsModelObservable);
	}

	/**
	 * Creates the {@link Table}(viewer) that will display the available Docker
	 * instances
	 * 
	 * @param container
	 *            the parent container
	 */
	private void createExistingInstancesTable(final Composite container) {
		final int INDENT = 20;
		final Table table = new Table(container,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.indent(INDENT, 0).hint(200, 100).applyTo(table);
		final TableViewer tableViewer = new TableViewer(table);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		addTableViewerColum(tableViewer, WizardMessages.getString(
				"NewDockerConnectionPage.existingInstanceNameColumnLabel"), //$NON-NLS-1$
				150);
		addTableViewerColum(tableViewer, WizardMessages.getString(
				"NewDockerConnectionPage.existingInstanceURLColumnLabel"), //$NON-NLS-1$
				150);
		addTableViewerColum(tableViewer, WizardMessages.getString(
				"NewDockerConnectionPage.existingInstanceStateColumnLabel"), //$NON-NLS-1$
				100);
		addTableViewerColum(tableViewer, WizardMessages.getString(
				"NewDockerConnectionPage.existingInstanceDockerVersionColumnLabel"), //$NON-NLS-1$
				100);

		// observe model to enable/disable the table(viewer)
		@SuppressWarnings("unchecked")
		final IObservableValue<Boolean> useCustomConnectionSettingsModelObservable = BeanProperties
				.value(NewDockerConnectionPageModel.class,
						NewDockerConnectionPageModel.USE_CUSTOM_SETTINGS)
				.observe(model);
		useCustomConnectionSettingsModelObservable.addValueChangeListener(
				toggleEnablement(true, tableViewer.getTable()));
		// set initial enablement state
		setEnabled(!useCustomConnectionSettingsModelObservable.getValue(),
				tableViewer.getTable());

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

	/**
	 * Creates a new Composite that displays the widgets to specify custom
	 * connection settings.
	 * 
	 * @param parent
	 *            the parent container
	 */
	private void createCustomSettingsContainer(final Composite parent) {
		final int COLUMNS = 3;
		final int INDENT = 20;
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).span(1, 1)
				.indent(INDENT, 0).grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(COLUMNS).spacing(10, 10)
				.applyTo(container);

		// Connection name
		final Label connectionNameLabel = new Label(container, SWT.NONE);
		connectionNameLabel.setText(WizardMessages.getString(
				"NewDockerConnectionPage.customConnectionNameLabel")); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.applyTo(connectionNameLabel);
		final Text connectionNameText = new Text(container, SWT.BORDER);
		connectionNameText.setToolTipText(WizardMessages.getString(
				"NewDockerConnectionPage.customConnectionNameTooltip")); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).span(2, 1).applyTo(connectionNameText);

		// Connection URL
		final Label connectionURLLabel = new Label(container, SWT.NONE);
		connectionURLLabel.setText(WizardMessages
				.getString("NewDockerConnectionPage.customConnectionURLLabel")); //$NON-NLS-1$
		connectionURLLabel.setToolTipText(WizardMessages.getString(
				"NewDockerConnectionPage.customConnectionURLTooltip")); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.applyTo(connectionURLLabel);
		final Text connectionURLText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(2, 1)
				.grab(true, false).applyTo(connectionURLText);

		// enable TLS
		final Button useAuthButton = new Button(container, SWT.CHECK);
		useAuthButton.setText(WizardMessages
				.getString("NewDockerConnectionPage.customUseAuthButton")); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(3, 1)
				.applyTo(useAuthButton);

		// Path to certs
		final Label certPathLabel = new Label(container, SWT.NONE);
		certPathLabel.setText(WizardMessages
				.getString("NewDockerConnectionPage.customCertPathLabel")); //$NON-NLS-1$
		certPathLabel.setToolTipText(WizardMessages
				.getString("NewDockerConnectionPage.customCertPathTooltip")); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.indent(INDENT, 0).applyTo(certPathLabel);
		final Text certPathText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).applyTo(certPathText);
		final Button certPathBrowseButton = new Button(container, SWT.BUTTON1);
		certPathBrowseButton.setText(WizardMessages
				.getString("NewDockerConnectionPage.browseButtonLabel")); //$NON-NLS-1$
		certPathBrowseButton.addSelectionListener(onBrowseCertPath());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.applyTo(certPathBrowseButton);

		// the 'test connection' button
		final Button testConnectionButton = new Button(container, SWT.NONE);
		testConnectionButton.setText(WizardMessages
				.getString("NewDockerConnectionPage.testConnectionLabel")); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(3, 1)
				.align(SWT.END, SWT.CENTER).applyTo(testConnectionButton);
		testConnectionButton
				.addSelectionListener(onTestConnectionButtonSelection());

		// bind controls to model
		@SuppressWarnings("unchecked")
		final IObservableValue<Boolean> useCustomConnectionSettingsModelObservable = BeanProperties
				.value(NewDockerConnectionPageModel.class,
						NewDockerConnectionPageModel.USE_CUSTOM_SETTINGS)
				.observe(model);
		@SuppressWarnings("unchecked")
		final IObservableValue<String> connectionNameModelObservable = BeanProperties
				.value(NewDockerConnectionPageModel.class,
						NewDockerConnectionPageModel.CONNECTION_NAME)
				.observe(model);
		dbc.bindValue(
				WidgetProperties.text(SWT.Modify).observe(connectionNameText),
				connectionNameModelObservable);
		@SuppressWarnings("unchecked")
		final IObservableValue<String> customHostModelObservable = BeanProperties
				.value(NewDockerConnectionPageModel.class,
						NewDockerConnectionPageModel.CUSTOM_HOST)
				.observe(model);
		dbc.bindValue(
				WidgetProperties.text(SWT.Modify).observe(connectionURLText),
				customHostModelObservable);
		@SuppressWarnings("unchecked")
		final IObservableValue<Boolean> customTLSVerifyModelObservable = BeanProperties
				.value(NewDockerConnectionPageModel.class,
						NewDockerConnectionPageModel.CUSTOM_TLS_VERIFY)
				.observe(model);
		dbc.bindValue(WidgetProperties.selection().observe(useAuthButton),
				customTLSVerifyModelObservable);
		@SuppressWarnings("unchecked")
		final IObservableValue<String> customCertPathModelObservable = BeanProperties
				.value(NewDockerConnectionPageModel.class,
						NewDockerConnectionPageModel.CUSTOM_CERT_PATH)
				.observe(model);
		dbc.bindValue(WidgetProperties.text(SWT.Modify).observe(certPathText),
				customCertPathModelObservable);
		// controls to enable when the 'custom settings' option is chosen
		useCustomConnectionSettingsModelObservable.addValueChangeListener(
				toggleEnablement(connectionNameText, connectionURLText,
						useAuthButton, testConnectionButton));
		// controls to enable when the 'custom settings' option is chosen, only
		// if the 'use TLS verify' option is selected.
		useCustomConnectionSettingsModelObservable.addValueChangeListener(
				toggleEnablement(customTLSVerifyModelObservable, certPathText,
						certPathBrowseButton));
		// controls to enable when the 'use TLS' option is selected
		customTLSVerifyModelObservable.addValueChangeListener(
				toggleEnablement(certPathText, certPathBrowseButton));
		// validations will be performed when the user changes the value
		// only, not at the dialog opening
		dbc.addValidationStatusProvider(
				new ConnectionNameValidator(connectionNameModelObservable));
		dbc.addValidationStatusProvider(new CertificatesPathValidator(
				customTLSVerifyModelObservable, customCertPathModelObservable));

		// set initial enablement for controls
		setEnabled(useCustomConnectionSettingsModelObservable.getValue(),
				connectionNameText, connectionURLText, useAuthButton,
				certPathText, certPathBrowseButton, testConnectionButton);

	}

	/**
	 * Toggle the enablement of the given {@link Composite} elements and give
	 * focus to the first one.
	 * 
	 * @param condition
	 * @param controls
	 *            the {@link Composite} to enable or disable, based upon the
	 *            event that will occur.
	 * @return the {@link IValueChangeListener}
	 */
	private IValueChangeListener<? super Boolean> toggleEnablement(
			final IObservableValue<Boolean> condition,
			final Control... controls) {
		return new IValueChangeListener<Boolean>() {

			@Override
			public void handleValueChange(
					ValueChangeEvent<? extends Boolean> event) {
				if (condition != null && condition.getValue() != null
						&& condition.getValue()) {
					final boolean selected = event.getObservableValue()
							.getValue().booleanValue();
					setEnabled(selected, controls);
				}
			}

		};
	}

	/**
	 * Toggle the enablement of the given {@link Composite} elements and give
	 * focus to the first one.
	 * 
	 * @param controls
	 *            the {@link Composite} to enable or disable, based upon the
	 *            event that will occur.
	 * @return the {@link IValueChangeListener}
	 */
	private IValueChangeListener<? super Boolean> toggleEnablement(
			final Control... controls) {
		return toggleEnablement(false, controls);
	}

	/**
	 * Toggle the enablement of the given {@link Composite} elements and give
	 * focus to the first one.
	 * 
	 * @param inverse
	 *            if the enablement should be inversed (<code>true</code>)
	 *            compared to the received event.
	 * @param controls
	 *            the {@link Composite} to enable or disable, based upon the
	 *            event that will occur.
	 * @return the {@link IValueChangeListener}
	 */
	private IValueChangeListener<? super Boolean> toggleEnablement(
			final boolean inverse, final Control... controls) {
		return new IValueChangeListener<Boolean>() {

			@Override
			public void handleValueChange(
					ValueChangeEvent<? extends Boolean> event) {
				final boolean selected = inverse
						? !event.getObservableValue().getValue().booleanValue()
						: event.getObservableValue().getValue().booleanValue();
				setEnabled(selected, controls);
			}

		};
	}

	private void setEnabled(final boolean selected, final Control... controls) {
		// apply selection
		Stream.of(controls).forEach(control -> control.setEnabled(selected));
		Stream.of(controls).findFirst()
				.ifPresent(control -> control.setFocus());
	}

	private SelectionListener onBrowseCertPath() {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final DirectoryDialog directoryDialog = new DirectoryDialog(
						getShell());
				final String selectedPath = directoryDialog.open();
				if (selectedPath != null) {
					model.setCustomCertPath(selectedPath);
				}
			}
		};
	}

	private void retrieveExistingInstances() {
		// FIXME: do not run in UI thread.
		Display.getCurrent().asyncExec(() -> {
			try {
				getContainer().run(true, false, new IRunnableWithProgress() {

					@Override
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException,
							InterruptedException {
						// TODO externalize message
						monitor.beginTask(
								"Looking for existing Docker instances", 2);
						// look for Unix socket if OS is Unix/Linux

						// look for Docker for Mac/Windows

						// look for Docker machines
						final String pathToDockerMachine = Activator
								.getDefault().getPreferenceStore().getString(
										PreferenceConstants.DOCKER_MACHINE_INSTALLATION_DIRECTORY);
						final String pathToVMDriver = Activator.getDefault()
								.getPreferenceStore().getString(
										PreferenceConstants.VM_DRIVER_INSTALLATION_DIRECTORY);
						final List<IDockerRuntime> availableRuntimes = DockerConnectionManager
								.getInstance()
								.findAvailableRuntimes(pathToDockerMachine,
										pathToVMDriver);
						model.setAvailableInstances(availableRuntimes);
						monitor.done();

					}
				});
			} catch (InvocationTargetException | InterruptedException e) {
				// TODO externalize message
				MessageDialog.openError(getShell(), "Error",
						"Error while trying to find existing Docker instances: "
								+ e.getMessage());
			}
		});
	}

	// /**
	// * Sets the default settings by looking for the:
	// * <ul>
	// * <li>a Unix socket at /var/run/docker.sock</li>
	// * <li>the following environment variables:
	// * <ul>
	// * <li>DOCKER_HOST</li>
	// * <li>DOCKER_CERT_PATH</li>
	// * <li>DOCKER_TLS_VERIFY</li>
	// * </ul>
	// * </li>
	// * </ul>
	// * and sets the default connection settings accordingly.
	// */
	// private void retrieveDefaultConnectionSettings() {
	// // let's run this in a job and show the progress in the wizard
	// // progressbar
	// try {
	// getWizard().getContainer().run(true, true,
	// new IRunnableWithProgress() {
	// @Override
	// public void run(final IProgressMonitor monitor) {
	// monitor.beginTask(WizardMessages.getString(
	// "NewDockerConnectionPage.retrieveTask"), //$NON-NLS-1$
	// 1);
	// final List<IDockerConnectionSettings> defaults = DockerConnectionManager
	// .getInstance().findConnectionSettings();
	// if (!defaults.isEmpty()) {
	// final IDockerConnectionSettings defaultConnectionSettings = defaults
	// .get(0);
	// model.setCustomSettings(
	// !defaultConnectionSettings
	// .isSettingsResolved());
	// model.setConnectionName(
	// defaultConnectionSettings.getName());
	// switch (defaultConnectionSettings.getType()) {
	// case TCP_CONNECTION:
	// final TCPConnectionSettings tcpConnectionSettings =
	// (TCPConnectionSettings) defaultConnectionSettings;
	// model.setTcpConnectionBindingMode(true);
	// model.setCustomCertPath(
	// tcpConnectionSettings
	// .getPathToCertificates());
	// model.setCustomTLSVerify(
	// tcpConnectionSettings
	// .isTlsVerify());
	// model.setCustomHost(
	// tcpConnectionSettings.getHost());
	// break;
	// case UNIX_SOCKET_CONNECTION:
	// model.setUnixSocketBindingMode(true);
	// final UnixSocketConnectionSettings unixSocketConnectionSettings =
	// (UnixSocketConnectionSettings) defaultConnectionSettings;
	// model.setUnixSocketPath(
	// unixSocketConnectionSettings
	// .getPath());
	// break;
	// }
	// } else {
	// // fall-back to custom settings, suggesting a
	// // Unix Socket connection to the user.
	// model.setCustomSettings(true);
	// model.setUnixSocketBindingMode(true);
	// }
	//
	// monitor.done();
	// }
	// });
	// } catch (InvocationTargetException | InterruptedException e) {
	// Activator.log(e);
	// }
	//
	// }

	private void updateWidgetsState(
			final Control[] bindingModeSelectionControls,
			final Control[] unixSocketControls,
			final Control[] tcpConnectionControls,
			final Control[] tcpAuthControls) {
		// setWidgetsEnabled(
		// model.isCustomSettings() && model.isTcpConnectionBindingMode()
		// && model.isCustomTLSVerify(),
		// tcpAuthControls);
		// setWidgetsEnabled(
		// model.isCustomSettings() && model.isTcpConnectionBindingMode(),
		// tcpConnectionControls);
		// setWidgetsEnabled(
		// model.isCustomSettings() && model.isUnixSocketBindingMode(),
		// unixSocketControls);
		// setWidgetsEnabled(model.isCustomSettings(),
		// bindingModeSelectionControls);
	}

	private IValueChangeListener onTcpAuthSelection(
			final Control[] tcpAuthControls) {
		return new IValueChangeListener() {

			@Override
			public void handleValueChange(final ValueChangeEvent event) {
				// setWidgetsEnabled(model.isCustomSettings()
				// && model.isTcpConnectionBindingMode()
				// && model.isCustomTLSVerify(), tcpAuthControls);
			}
		};
	}

	private void setWidgetsEnabled(final boolean enabled,
			final Control... controls) {
		for (Control control : controls) {
			control.setEnabled(enabled);
		}
		// set the focus on the fist element of the group.
		if (enabled) {
			for (Control control : controls) {
				if (control instanceof Text) {
					control.setFocus();
					break;
				}
			}
		}
	}

	/**
	 * Verifies that the given connection settings work by trying to connect to
	 * the target Docker daemon
	 * 
	 * @return
	 */
	private SelectionListener onTestConnectionButtonSelection() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					getWizard().getContainer().run(true, false,
							new IRunnableWithProgress() {
								@Override
								public void run(
										final IProgressMonitor monitor) {
									monitor.beginTask(WizardMessages.getString(
											"NewDockerConnectionPage.pingTask"), //$NON-NLS-1$
											IProgressMonitor.UNKNOWN);
									try {
										final DockerConnection dockerConnection = getDockerConnection();
										dockerConnection.open(false);
										dockerConnection.ping();
										dockerConnection.close();
										// ping succeeded
										displaySuccessDialog();
									} catch (DockerException e) {
										// only log if there's an underlying
										// cause.
										if (e.getCause() != null) {
											Activator.log(e);
										}
										displayErrorDialog();
									}
								}

							});
				} catch (InvocationTargetException | InterruptedException o_O) {
					Activator.log(o_O);
				}

			}

			private void displaySuccessDialog() {
				displayDialog(
						WizardMessages
								.getString("NewDockerConnectionPage.success"), //$NON-NLS-1$
						WizardMessages.getString(
								"NewDockerConnectionPage.pingSuccess"), //$NON-NLS-1$
						SWT.ICON_INFORMATION, new String[] { WizardMessages
								.getString("NewDockerConnectionPage.ok") } //$NON-NLS-1$
				);
			}

			private void displayErrorDialog() {
				displayDialog(
						WizardMessages
								.getString("NewDockerConnectionPage.failure"), //$NON-NLS-1$
						WizardMessages.getString(
								"NewDockerConnectionPage.pingFailure"), //$NON-NLS-1$
						SWT.ICON_ERROR, new String[] { WizardMessages
								.getString("NewDockerConnectionPage.ok") } //$NON-NLS-1$
				);
			}

			private void displayDialog(final String dialogTitle,
					final String dialogMessage, final int icon,
					final String[] buttonLabels) {
				Display.getDefault().syncExec(new Runnable() {

					@Override
					public void run() {
						new MessageDialog(
								PlatformUI.getWorkbench()
										.getActiveWorkbenchWindow().getShell(),
								dialogTitle, null, dialogMessage, icon,
								buttonLabels, 0).open();
					}
				});
			}

		};
	}

	/**
	 * Opens a new {@link DockerConnection} using the settings of this
	 * {@link NewDockerConnectionPage}.
	 * 
	 * @return
	 * @throws DockerCertificateException
	 */
	protected DockerConnection getDockerConnection() {
		// if (model.getBindingMode() == UNIX_SOCKET) {
		// return new DockerConnection.Builder()
		// .name(model.getConnectionName())
		// .unixSocket(model.getUnixSocketPath()).build();
		// } else {
		// final Builder tcpConnectionBuilder = new DockerConnection.Builder()
		// .name(model.getConnectionName())
		// .tcpHost(model.getCustomHost());
		// if (model.isCustomTLSVerify()) {
		// tcpConnectionBuilder.tcpCertPath(model.getCustomCertPath());
		// }
		// return tcpConnectionBuilder.build();
		// }
		return null;
	}

	/**
	 * Custom UpdateValueStrategy that returns the opposite value for a given
	 * {@link Boolean}
	 */
	private static final class OppositeBooleanValueStrategy
			extends UpdateValueStrategy {
		@Override
		public Object convert(final Object value) {
			if (value instanceof Boolean) {
				return !(Boolean) value;
			}
			return super.convert(value);
		}
	}

	private static class ConnectionNameValidator extends MultiValidator {

		private final IObservableValue<String> connectionNameModelObservable;

		public ConnectionNameValidator(
				final IObservableValue<String> connectionNameModelObservable) {
			this.connectionNameModelObservable = connectionNameModelObservable;
		}

		@Override
		public IObservableList<IObservableValue<String>> getTargets() {
			WritableList<IObservableValue<String>> targets = new WritableList<>();
			targets.add(connectionNameModelObservable);
			return targets;
		}

		@Override
		protected IStatus validate() {
			final String connectionName = this.connectionNameModelObservable
					.getValue();
			if (connectionName == null || connectionName.isEmpty()) {
				return ValidationStatus.error(WizardMessages.getString(
						"NewDockerConnectionPage.validation.missingConnectionName.msg")); //$NON-NLS-1$
			} else if (DockerConnectionManager.getInstance()
					.findConnection(connectionName) != null) {
				return ValidationStatus.error(WizardMessages.getString(
						"NewDockerConnectionPage.validation.duplicateConnectionName.msg")); //$NON-NLS-1$
			}
			return ValidationStatus.ok();
		}
	}

	private static class TcpHostValidator extends MultiValidator {

		private final IObservableValue<Boolean> tcpConnectionBindingModeModelObservable;
		private final IObservableValue<String> tcpHostModelObservable;

		public TcpHostValidator(
				final IObservableValue<Boolean> tcpConnectionBindingModeModelObservable,
				final IObservableValue<String> tcpHostModelObservable) {
			this.tcpConnectionBindingModeModelObservable = tcpConnectionBindingModeModelObservable;
			this.tcpHostModelObservable = tcpHostModelObservable;
		}

		@Override
		public IObservableList<IObservableValue<String>> getTargets() {
			WritableList<IObservableValue<String>> targets = new WritableList<>();
			targets.add(tcpHostModelObservable);
			return targets;
		}

		@Override
		protected IStatus validate() {
			final Boolean tcpConnectionBindingMode = this.tcpConnectionBindingModeModelObservable
					.getValue();
			final String tcpHost = this.tcpHostModelObservable.getValue();
			if (tcpConnectionBindingMode) {
				if (tcpHost == null || tcpHost.isEmpty()) {
					return ValidationStatus.error(WizardMessages.getString(
							"NewDockerConnectionPage.validation.missingTcpConnectionURI.msg")); //$NON-NLS-1$
				}
				try {
					final URI uri = new URI(tcpHost);
					final String scheme = uri.getScheme() != null
							? uri.getScheme().toLowerCase() : null;
					final String host = uri.getHost();
					final int port = uri.getPort();
					if (scheme != null
							&& !(scheme.equals("tcp") || scheme.equals("http") //$NON-NLS-1$ //$NON-NLS-2$
									|| scheme.equals("https"))) { //$NON-NLS-1$
						return ValidationStatus.error(WizardMessages.getString(
								"NewDockerConnectionPage.validation.invalidTcpConnectionScheme.msg")); //$NON-NLS-1$
					} else if (host == null) {
						return ValidationStatus.error(WizardMessages.getString(
								"NewDockerConnectionPage.validation.invalidTcpConnectionHost.msg")); //$NON-NLS-1$

					} else if (port == -1) {
						return ValidationStatus.error(WizardMessages.getString(
								"NewDockerConnectionPage.validation.invalidTcpConnectionPort.msg")); //$NON-NLS-1$

					}
				} catch (URISyntaxException e) {
					// URI is not valid
					return ValidationStatus.error(WizardMessages.getString(
							"NewDockerConnectionPage.validation.invalidTcpConnectionURI.msg")); //$NON-NLS-1$
				}
			}
			return ValidationStatus.ok();
		}

	}

	private static class CertificatesPathValidator extends MultiValidator {

		private final IObservableValue<Boolean> tcpTlsVerifyModelObservable;
		private final IObservableValue<String> tcpCertPathModelObservable;

		public CertificatesPathValidator(
				final IObservableValue<Boolean> tcpTlsVerifyModelObservable,
				final IObservableValue<String> tcpCertPathModelObservable) {
			this.tcpTlsVerifyModelObservable = tcpTlsVerifyModelObservable;
			this.tcpCertPathModelObservable = tcpCertPathModelObservable;
		}

		@Override
		public IObservableList<IObservableValue<String>> getTargets() {
			WritableList<IObservableValue<String>> targets = new WritableList<>();
			targets.add(tcpCertPathModelObservable);
			return targets;
		}

		@Override
		protected IStatus validate() {
			final Boolean tcpTlsVerify = this.tcpTlsVerifyModelObservable
					.getValue();
			final String tcpCertPath = this.tcpCertPathModelObservable
					.getValue();
			if (tcpTlsVerify) {
				if (tcpCertPath == null || tcpCertPath.isEmpty()) {
					return ValidationStatus.error(WizardMessages.getString(
							"NewDockerConnectionPage.validation.missingTcpCertPath.msg")); //$NON-NLS-1$
				}
				final File tcpCert = new File(tcpCertPath);
				if (!tcpCert.exists()) {
					return ValidationStatus.error(WizardMessages.getString(
							"NewDockerConnectionPage.validation.invalidTcpCertPath.msg")); //$NON-NLS-1$
				} else if (!tcpCert.canRead() || !tcpCert.canRead()) {
					return ValidationStatus.error(WizardMessages.getString(
							"NewDockerConnectionPage.validation.unreadableTcpCertPath.msg")); //$NON-NLS-1$
				}
			}
			return ValidationStatus.ok();
		}

	}

	private class ConnectionSelectionContentProvider
			implements IStructuredContentProvider {
		@Override
		public void inputChanged(Viewer viewer, Object oldInput,
				Object newInput) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return (String[]) (inputElement);
		}
	}

	private class ConnectionSelectionLabelProvider implements ILabelProvider {
		@Override
		public void removeListener(ILabelProviderListener listener) {
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void dispose() {
		}

		@Override
		public void addListener(ILabelProviderListener listener) {
		}

		@Override
		public String getText(Object element) {
			return element.toString();
		}

		@Override
		public Image getImage(Object element) {
			return SWTImagesFactory.DESC_REPOSITORY_MIDDLE.createImage();
		}
	}

}
