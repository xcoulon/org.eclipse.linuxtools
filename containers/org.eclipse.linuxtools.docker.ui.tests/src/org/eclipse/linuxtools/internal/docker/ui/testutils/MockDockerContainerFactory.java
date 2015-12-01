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

package org.eclipse.linuxtools.internal.docker.ui.testutils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.linuxtools.docker.core.Messages;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableList;
import com.spotify.docker.client.messages.Container;

/**
 * 
 */
public class MockDockerContainerFactory {
	
	public static Builder id(final String id) {
		return new Builder().id(id);
	}

	public static Builder name(final String repoTag, final String... otherRepoTags) {
		return new Builder().randomId().name(repoTag, otherRepoTags);
	}

	public static class Builder {
		
		private static char[] hexa = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
		
		private final Container container;
		
		private Builder() {
			this.container = Mockito.mock(Container.class);
		}

		private Builder id(final String id) {
			Mockito.when(this.container.id()).thenReturn(id);
			return this;
		}
		
		private Builder randomId() {
			// generate a random id for the container
			final String id = IntStream.range(0, 12)
					.mapToObj(i -> Character.valueOf(hexa[new Random().nextInt(16)]).toString())
					.collect(Collectors.joining());
			Mockito.when(this.container.id()).thenReturn(id);
			return this;
		}
		
		public Builder name(final String name, final String... otherNames) {
			final List<String> repoTags = new ArrayList<>();
			repoTags.add(name);
			Stream.of(otherNames).forEach(r -> repoTags.add(r));
			Mockito.when(this.container.status()).thenReturn(Messages.Running_specifier);
			Mockito.when(this.container.names()).thenReturn(ImmutableList.copyOf(repoTags));
			Mockito.when(this.container.created()).thenReturn(new Date().getTime());
			return this;
		}

		public Builder imageId(final String imageId) {
			Mockito.when(this.container.image()).thenReturn(imageId);
			return this;
		}
		
		public Builder status(final String status) {
			Mockito.when(this.container.status()).thenReturn(status);
			return this;
		}
		
		public Container build() {
			return container;
		}

	}

}
