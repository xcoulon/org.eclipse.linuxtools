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

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.linuxtools.internal.docker.ui.databinding.BaseDatabindingModel;
import org.eclipse.linuxtools.internal.docker.ui.wizards.ImageRunSelectionModel.ExposedPortModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author xcoulon
 *
 */
public class ContainerPortDialog extends Dialog {

	private static final String PORT_TYPE = "/tcp";

	private final ContainerPortDialogModel model;

	private final DataBindingContext dbc = new DataBindingContext();

	protected ContainerPortDialog(final Shell parentShell) {
		super(parentShell);
		this.model = new ContainerPortDialogModel();
	}

	public ContainerPortDialog(final Shell parentShell,
			final ExposedPortModel selectedContainerPort) {
		super(parentShell);
		this.model = new ContainerPortDialogModel(
				selectedContainerPort.getContainerPort(),
				selectedContainerPort.getHostAddress(),
				selectedContainerPort.getHostPort());
	}

	@Override
	protected void configureShell(final Shell shell) {
		super.configureShell(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		shell.setText("Exposing a Container Port");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(400, super.getInitialSize().y);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final int COLUMNS = 2;
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL)
				.span(COLUMNS, 1).grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(COLUMNS).margins(6, 6)
				.applyTo(container);
		final Label explanationLabel = new Label(container, SWT.NONE);
		explanationLabel.setText("Specify the container port to expose:"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.span(COLUMNS, 1).grab(false, false).applyTo(explanationLabel);
		final Label containerLabel = new Label(container, SWT.NONE);
		containerLabel.setText("Container port:"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).applyTo(containerLabel);
		final Text containerPortText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).applyTo(containerPortText);
		final Label hostAddressLabel = new Label(container, SWT.NONE);
		hostAddressLabel.setText("Host address:"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).applyTo(hostAddressLabel);
		final Text hostAddressText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).applyTo(hostAddressText);
		final Label hostPortLabel = new Label(container, SWT.NONE);
		hostPortLabel.setText("Host port:"); //$NON-NLS-1$
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).applyTo(hostPortLabel);
		final Text hostPortText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).applyTo(hostPortText);
		// error message
		final Label errorMessageLabel = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.span(COLUMNS, 1).grab(true, false).applyTo(errorMessageLabel);

		// listening to changes
		final ISWTObservableValue containerPortObservable = WidgetProperties
				.text(SWT.Modify).observe(containerPortText);
		dbc.bindValue(containerPortObservable,
				BeanProperties
						.value(ContainerPortDialogModel.class,
								ContainerPortDialogModel.CONTAINER_PORT)
						.observe(model));
		final ISWTObservableValue hostAddressObservable = WidgetProperties
				.text(SWT.Modify).observe(hostAddressText);
		dbc.bindValue(hostAddressObservable,
				BeanProperties
						.value(ContainerPortDialogModel.class,
								ContainerPortDialogModel.HOST_ADDRESS)
						.observe(model));
		final ISWTObservableValue hostPortObservable = WidgetProperties
				.text(SWT.Modify).observe(hostPortText);
		dbc.bindValue(hostPortObservable,
				BeanProperties
						.value(ContainerPortDialogModel.class,
								ContainerPortDialogModel.HOST_PORT)
						.observe(model));

		containerPortObservable.addValueChangeListener(
				onContainerPortSettingsChanged(errorMessageLabel));
		hostPortObservable.addValueChangeListener(
				onContainerPortSettingsChanged(errorMessageLabel));
		hostAddressObservable.addValueChangeListener(
				onContainerPortSettingsChanged(errorMessageLabel));
		return container;
	}

	private IValueChangeListener onContainerPortSettingsChanged(
			final Label errorMessageLabel) {
		return new IValueChangeListener() {

			@Override
			public void handleValueChange(ValueChangeEvent event) {
				validateInput(errorMessageLabel);
			}
		};
	}

	private void validateInput(final Label errorMessageLabel) {
		final String containerPort = model.getContainerPort();
		final String hostAddress = model.getHostAddress();
		final String hostPort = model.getHostPort();
		if (containerPort == null || containerPort.isEmpty()) {
			setOkButtonEnabled(false);
		} else {
			setOkButtonEnabled(true);
		}
	}

	private void setOkButtonEnabled(final boolean enabled) {
		getButton(IDialogConstants.OK_ID).setEnabled(enabled);
	}

	public ExposedPortModel getPort() {
		return new ExposedPortModel(model.getContainerPort(), PORT_TYPE,
				model.getHostAddress(), model.getHostPort());
	}

	class ContainerPortDialogModel extends BaseDatabindingModel {

		public static final String CONTAINER_PORT = "containerPort";

		public static final String HOST_ADDRESS = "hostAddress";

		public static final String HOST_PORT = "hostPort";

		private String containerPort;

		private String hostAddress;

		private String hostPort;

		public ContainerPortDialogModel() {
		}

		public ContainerPortDialogModel(final String containerPort,
				final String hostAddress, final String hostPort) {
			this.containerPort = containerPort;
			this.hostAddress = hostAddress;
			this.hostPort = hostPort;
		}

		public String getContainerPort() {
			return containerPort;
		}

		public void setContainerPort(final String containerPort) {
			firePropertyChange(CONTAINER_PORT, this.containerPort,
					this.containerPort = containerPort);
		}

		public String getHostAddress() {
			return hostAddress;
		}

		public void setHostAddress(final String hostName) {
			firePropertyChange(HOST_ADDRESS, this.hostAddress,
					this.hostAddress = hostName);
		}

		public String getHostPort() {
			return hostPort;
		}

		public void setHostPort(final String hostPort) {
			firePropertyChange(HOST_PORT, this.hostPort,
					this.hostPort = hostPort);
		}
	}

}
