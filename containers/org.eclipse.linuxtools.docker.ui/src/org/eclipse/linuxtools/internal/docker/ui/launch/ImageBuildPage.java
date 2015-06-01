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

package org.eclipse.linuxtools.internal.docker.ui.launch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.linuxtools.docker.core.DockerConnectionManager;
import org.eclipse.linuxtools.docker.core.EnumImageBuildParameter;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.ui.Activator;
import org.eclipse.linuxtools.internal.docker.ui.SWTImagesFactory;
import org.eclipse.linuxtools.internal.docker.ui.databinding.BaseDatabindingModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.Preferences;

/**
 * @author xcoulon
 *
 */
public class ImageBuildPage extends WizardPage {

	private final DataBindingContext dbc = new DataBindingContext();
	private final ImageBuildPageModel model;

	public ImageBuildPage(final IFile dockerFile) {
		super("ImageBuildPage", "Build a Docker Image",
				SWTImagesFactory.DESC_BANNER_REPOSITORY);
		setMessage("Select the connection and the options to build the image"); //$NON-NLS-1$
		this.model = new ImageBuildPageModel(dockerFile);
	}

	public void saveState() {
		this.model.saveState();
	}

	@Override
	public void dispose() {
		dbc.dispose();
		super.dispose();
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(final Composite parent) {
		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).margins(6, 6)
				.applyTo(container);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL)
				.grab(true, true).applyTo(container);
		final Label connectionSelectionLabel = new Label(container, SWT.NONE);
		connectionSelectionLabel.setText("Connection");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).applyTo(connectionSelectionLabel);
		final Combo connectionSelectionCombo = new Combo(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).applyTo(connectionSelectionCombo);
		final ComboViewer connectionSelectionComboViewer = new ComboViewer(
				connectionSelectionCombo);
		connectionSelectionComboViewer
				.setContentProvider(new ObservableListContentProvider());
		dbc.bindList(WidgetProperties.items().observe(connectionSelectionCombo),
				BeanProperties
						.list(ImageBuildPageModel.class,
								ImageBuildPageModel.CONNECTION_NAMES)
						.observe(model));
		final IObservableValue connectionNameObservable = BeanProperties
				.value(ImageBuildPageModel.class,
						ImageBuildPageModel.CONNECTION_NAME)
				.observe(model);
		dbc.bindValue(
				WidgetProperties.selection().observe(connectionSelectionCombo),
				connectionNameObservable);
		new ContentProposalAdapter(connectionSelectionCombo,
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
				}, getConnectionNameContentProposalProvider(
						connectionSelectionCombo),
				null, null);

		// Repo/tag
		final Label repoTagLabel = new Label(container, SWT.NONE);
		repoTagLabel.setText("Repository name/tag:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(false, false).applyTo(repoTagLabel);
		final Text repoTagText = new Text(container, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER)
				.grab(true, false).applyTo(repoTagText);
		final IObservableValue repoTagObservable = BeanProperties
				.value(ImageBuildPageModel.class,
						ImageBuildPageModel.IMAGE_NAME)
				.observe(model);
		dbc.bindValue(WidgetProperties.text(SWT.Modify).observe(repoTagText),
				repoTagObservable);

		// options
		final Label optionsLabel = new Label(container, SWT.NONE);
		optionsLabel.setText("Options:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(2, 1)
				.grab(true, false).applyTo(optionsLabel);
		final int INDENT = 20;
		final Button quietBuildButton = new Button(container, SWT.CHECK);
		quietBuildButton.setText("Quiet build");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(2, 1)
				.indent(INDENT, 0).grab(true, false).applyTo(quietBuildButton);
		final IObservableValue quietBuildObservable = BeanProperties
				.value(ImageBuildPageModel.class,
						ImageBuildPageModel.QUIET_BUILD)
				.observe(model);
		dbc.bindValue(WidgetProperties.selection().observe(quietBuildButton),
				quietBuildObservable);

		final Button nocacheButton = new Button(container, SWT.CHECK);
		nocacheButton.setText("Do not use the cache when building the image");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(2, 1)
				.indent(INDENT, 0).grab(true, false).applyTo(nocacheButton);
		final IObservableValue nocacheObservable = BeanProperties
				.value(ImageBuildPageModel.class, ImageBuildPageModel.NO_CACHE)
				.observe(model);
		dbc.bindValue(WidgetProperties.selection().observe(nocacheButton),
				nocacheObservable);

		final Button noRemoveButton = new Button(container, SWT.RADIO);
		noRemoveButton.setText(
				"Do not remove intermediate containers after a successful build.");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(2, 1)
				.indent(INDENT, 0).grab(true, false).applyTo(noRemoveButton);
		final IObservableValue noRemoveObservable = BeanProperties
				.value(ImageBuildPageModel.class, ImageBuildPageModel.NO_REMOVE)
				.observe(model);
		dbc.bindValue(WidgetProperties.selection().observe(noRemoveButton),
				noRemoveObservable);

		final Button forceRemoveButton = new Button(container, SWT.RADIO);
		forceRemoveButton.setText("Always remove intermediate containers.");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).span(2, 1)
				.indent(INDENT, 0).grab(true, false).applyTo(forceRemoveButton);
		final IObservableValue forceRemoveObservable = BeanProperties
				.value(ImageBuildPageModel.class,
						ImageBuildPageModel.FORCE_REMOVE)
				.observe(model);
		dbc.bindValue(WidgetProperties.selection().observe(forceRemoveButton),
				forceRemoveObservable);

		// set validation
		final ImageBuildPageValidator validator = new ImageBuildPageValidator(
				connectionNameObservable, repoTagObservable);
		dbc.addValidationStatusProvider(validator);
		// setup validation support
		WizardPageSupport.create(this, dbc);

		setControl(container);
	}

	/**
	 * Creates an {@link IContentProposalProvider} to propose
	 * {@link IDockerConnection} names based on the current text.
	 * 
	 * @param items
	 * @return
	 */
	private IContentProposalProvider getConnectionNameContentProposalProvider(
			final Combo connectionSelectionCombo) {
		return new IContentProposalProvider() {

			@Override
			public IContentProposal[] getProposals(final String contents,
					final int position) {
				final List<IContentProposal> proposals = new ArrayList<>();
				for (String imageName : connectionSelectionCombo.getItems()) {
					if (imageName.contains(contents)) {
						proposals.add(new ContentProposal(imageName, imageName,
								imageName, position));
					}
				}
				return proposals.toArray(new IContentProposal[0]);
			}
		};
	}

	public IDockerConnection getConnection() {
		return model.getConnection();
	}

	public String getImageRepoTag() {
		return model.getImageName();
	}

	public List<EnumImageBuildParameter> getBuildOptions() {
		final List<EnumImageBuildParameter> buildParameters = new ArrayList<>();
		if (model.isAlwaysRemoveIntermediateContainers()) {
			buildParameters.add(EnumImageBuildParameter.FORCE_RM);
		}
		if (model.isNoCache()) {
			buildParameters.add(EnumImageBuildParameter.NO_CACHE);
		}
		if (model.isNoRemoveIntermediateContainers()) {
			buildParameters.add(EnumImageBuildParameter.NO_RM);
		}
		if (model.isQuietBuild()) {
			buildParameters.add(EnumImageBuildParameter.QUIET);
		}
		return buildParameters;
	}

	class ImageBuildPageValidator extends MultiValidator {

		private final IObservableValue connectionNameObservable;
		private final IObservableValue repoTagObservable;

		ImageBuildPageValidator(final IObservableValue connectionObservable,
				final IObservableValue repoTagObservable) {
			this.connectionNameObservable = connectionObservable;
			this.repoTagObservable = repoTagObservable;
		}

		@Override
		protected IStatus validate() {
			final String connectionName = (String) connectionNameObservable
					.getValue();
			final String repoTag = (String) repoTagObservable.getValue();
			if (connectionName == null || connectionName.isEmpty()
					|| !model.getConnectionNames().contains(connectionName)) {
				return ValidationStatus.error(
						"An existing connection to a Docker daemon must be selected.");
			} else if (repoTag == null || repoTag.isEmpty()) {
				return ValidationStatus.error(
						"An repository (and optional tag) for the image must be provided.");
			} else if (!repoTag.matches("[a-z0-9\\-\\_\\.\\:\\/]+")) {
				return ValidationStatus.error(
						"Invalid repository name. Only [a-z0-9\\-\\_\\.\\:\\/]+ are allowed");
			}
			return ValidationStatus.ok();
		}

		@Override
		public IObservableList getTargets() {
			final WritableList targets = new WritableList();
			targets.add(connectionNameObservable);
			targets.add(repoTagObservable);
			return targets;
		}

	}

	class ImageBuildPageModel extends BaseDatabindingModel {

		static final String STATE_NODE = "imageBuild";

		static final String CONNECTION_NAMES = "connectionNames";
		static final String CONNECTION_NAME = "connectionName";
		static final String IMAGE_NAME = "imageName";
		static final String QUIET_BUILD = "quietBuild";
		static final String NO_CACHE = "noCache";
		static final String NO_REMOVE = "noRemoveIntermediateContainers";
		static final String FORCE_REMOVE = "alwaysRemoveIntermediateContainers";

		private final IFile dockerFile;

		private final Map<String, IDockerConnection> connections = new HashMap<>();

		private final List<String> connectionNames = new ArrayList<>();

		private String connectionName;

		private String imageName;

		private boolean quietBuild;

		private boolean noCache;

		private boolean noRemoveIntermediateContainers;

		private boolean alwaysRemoveIntermediateContainers;

		public ImageBuildPageModel(final IFile dockerFile) {
			this.dockerFile = dockerFile;
			for (IDockerConnection connection : DockerConnectionManager
					.getInstance().getConnections()) {
				connections.put(connection.getName(), connection);
				connectionNames.add(connection.getName());
			}
			Collections.sort(connectionNames);
			this.restoreState();
		}

		/**
		 * Restores this Wizard Page Model state from Preferences so user does
		 * not have to input all the data again.
		 */
		private void restoreState() {
			// keep data in memory so user won't have to input them again when
			// calling the wizard
			final Preferences stateNode = getStateNode();
			setConnectionName(stateNode.get(CONNECTION_NAME,
					connectionNames.isEmpty() ? "" : connectionNames.get(0)));
			setImageName(stateNode.get(IMAGE_NAME, ""));
			setAlwaysRemoveIntermediateContainers(
					stateNode.getBoolean(FORCE_REMOVE, false));
			setNoCache(stateNode.getBoolean(NO_CACHE, false));
			setNoRemoveIntermediateContainers(
					stateNode.getBoolean(NO_REMOVE, false));
			setQuietBuild(stateNode.getBoolean(QUIET_BUILD, false));

		}

		/**
		 * Saves this Wizard Page Model state in Preferences so they can be
		 * restored at the next call.
		 */
		void saveState() {
			// keep data in memory so user won't have to input them again when
			// calling the wizard
			final Preferences stateNode = getStateNode();
			stateNode.put(CONNECTION_NAME, getConnectionName());
			stateNode.put(IMAGE_NAME, getImageName());
			stateNode.putBoolean(FORCE_REMOVE,
					isAlwaysRemoveIntermediateContainers());
			stateNode.putBoolean(NO_CACHE, isNoCache());
			stateNode.putBoolean(NO_REMOVE, isNoRemoveIntermediateContainers());
			stateNode.putBoolean(QUIET_BUILD, isQuietBuild());
		}

		/**
		 * @return the Preferences to save and restore the wizard settings
		 *         associated with the current Docker file. Preferences will be
		 *         created if they did not exist before.
		 */
		private Preferences getStateNode() {
			final String dockerFileNodeKey = dockerFile.getFullPath()
					.toPortableString();
			final Preferences imageBuildWizardStateNode = DefaultScope.INSTANCE
					.getNode(Activator.PLUGIN_ID).node(STATE_NODE);
			final Preferences stateNode = imageBuildWizardStateNode
					.node(dockerFileNodeKey);
			return stateNode;
		}

		public IDockerConnection getConnection() {
			return connections.get(connectionName);
		}

		public List<String> getConnectionNames() {
			return connectionNames;
		}

		public String getConnectionName() {
			return connectionName;
		}

		public void setConnectionName(String connectionName) {
			firePropertyChange(CONNECTION_NAME, this.connectionName,
					this.connectionName = connectionName);
		}

		public String getImageName() {
			return imageName;
		}

		public void setImageName(String imageName) {
			firePropertyChange(IMAGE_NAME, this.imageName,
					this.imageName = imageName);
		}

		public boolean isQuietBuild() {
			return quietBuild;
		}

		public void setQuietBuild(boolean quietBuild) {
			firePropertyChange(QUIET_BUILD, this.quietBuild,
					this.quietBuild = quietBuild);
		}

		public boolean isAlwaysRemoveIntermediateContainers() {
			return alwaysRemoveIntermediateContainers;
		}

		public void setAlwaysRemoveIntermediateContainers(
				boolean alwaysRemoveIntermediateContainers) {
			firePropertyChange(FORCE_REMOVE,
					this.alwaysRemoveIntermediateContainers,
					this.alwaysRemoveIntermediateContainers = alwaysRemoveIntermediateContainers);
		}

		public boolean isNoCache() {
			return noCache;
		}

		public void setNoCache(boolean noCache) {
			firePropertyChange(NO_CACHE, this.noCache, this.noCache = noCache);
		}

		public boolean isNoRemoveIntermediateContainers() {
			return noRemoveIntermediateContainers;
		}

		public void setNoRemoveIntermediateContainers(
				boolean noRemoveIntermediateContainers) {
			firePropertyChange(NO_REMOVE, this.noRemoveIntermediateContainers,
					this.noRemoveIntermediateContainers = noRemoveIntermediateContainers);
		}

	}

}
