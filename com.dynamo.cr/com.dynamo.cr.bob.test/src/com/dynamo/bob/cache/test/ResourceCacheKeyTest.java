// Copyright 2021 The Defold Foundation
// Licensed under the Defold License version 1.0 (the "License"); you may not use
// this file except in compliance with the License.
//
// You may obtain a copy of the License, together with FAQs at
// https://www.defold.com/license
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

package com.dynamo.bob.cache.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.dynamo.bob.fs.DefaultResource;
import com.dynamo.bob.fs.DefaultFileSystem;
import com.dynamo.bob.fs.IResource;
import com.dynamo.bob.Builder;
import com.dynamo.bob.Task;
import com.dynamo.bob.Task.TaskBuilder;
import com.dynamo.bob.CompileExceptionError;
import com.dynamo.bob.cache.ResourceCacheKey;
import com.dynamo.bob.test.util.MockFileSystem;
import com.dynamo.bob.test.util.MockResource;
import com.dynamo.bob.archive.EngineVersion;

public class ResourceCacheKeyTest {

	private class DummyBuilder extends Builder<Void> {
		private TaskBuilder<Void> builder;

		public DummyBuilder() {
			builder = Task.<Void> newBuilder(this);
		}
		public DummyBuilder addInput(IResource input) {
			builder.addInput(input);
			return this;
		}
		public DummyBuilder addOutput(IResource output) {
			builder.addOutput(output);
			return this;
		}
		@Override
		public Task<Void> create(IResource input) throws IOException, CompileExceptionError {
			if (input != null) {
				builder.addInput(input);
			}
			return builder.build();
		}

		@Override
		public void build(Task<Void> task) throws CompileExceptionError, IOException {

		}
	}

	private MockFileSystem fs;

	private int resourceCount = 0;

	private MockResource createResource(String content) throws IOException {
		resourceCount = resourceCount + 1;
		String name = "tmpresource" + resourceCount;
		return fs.addFile(name, content.getBytes());
	}

	private Map<String, String> createOptions(int keyCount) {
		Map<String, String> options = new HashMap<String, String>();
		for(int i = 0; i < keyCount; i++) {
			options.put("key" + i, "value" + i);
		}
		return options;
	}

	@Before
	public void setUp() throws Exception {
		fs = new MockFileSystem();
		fs.setBuildDirectory("build");
	}

	@After
	public void tearDown() throws IOException {
	}

	// can we create a cache key at all?
	@Test
	public void testBasicKeyCreation() throws CompileExceptionError, IOException {
		IResource input = createResource("someInput");
		IResource output = createResource("someOutput").output();

		DummyBuilder builder = new DummyBuilder();
		Task<?> task = builder.addInput(input).addOutput(output).create(null);
		String key = ResourceCacheKey.calculate(task, createOptions(1), output);
		assertTrue(key != null);
	}

	// do we always get the same key with the same input?
	@Test
	public void testDeterministicKeyCreation() throws CompileExceptionError, IOException {
		IResource input = createResource("someInput");
		IResource output = createResource("someOutput").output();

		DummyBuilder builder1 = new DummyBuilder();
		Task<?> task1 = builder1.addInput(input).addOutput(output).create(null);
		String key1 = ResourceCacheKey.calculate(task1, createOptions(1), output);

		DummyBuilder builder2 = new DummyBuilder();
		Task<?> task2 = builder2.addInput(input).addOutput(output).create(null);
		String key2 = ResourceCacheKey.calculate(task2, createOptions(1), output);

		assertEquals(key1, key2);
	}

	// is project options taken into account and produce different keys?
	@Test
	public void testProjectOptions() throws CompileExceptionError, IOException {
		IResource input = createResource("someInput");
		IResource output = createResource("someOutput").output();

		DummyBuilder builder = new DummyBuilder();
		Task<?> task = builder.addInput(input).addOutput(output).create(null);
		String key1 = ResourceCacheKey.calculate(task, createOptions(0), output);
		String key2 = ResourceCacheKey.calculate(task, createOptions(10), output);

		assertNotEquals(key1, key2);
	}

	// are input resources taken into account and produce different keys?
	@Test
	public void testInputs() throws CompileExceptionError, IOException {
		IResource input = createResource("someInput");
		IResource output = createResource("someOutput").output();

		DummyBuilder builder1 = new DummyBuilder();
		Task<?> task1 = builder1.addInput(input).addOutput(output).create(null);
		String key1 = ResourceCacheKey.calculate(task1, createOptions(1), output);

		DummyBuilder builder2 = new DummyBuilder();
		input.setContent("someOtherInput".getBytes());
		Task<?> task2 = builder2.addInput(input).addOutput(output).create(null);
		String key2 = ResourceCacheKey.calculate(task2, createOptions(1), output);

		assertNotEquals(key1, key2);
	}

	// are output resource names taken into account and produce different keys?
	@Test
	public void testOutputNames() throws CompileExceptionError, IOException {
		IResource input = createResource("someInput");

		DummyBuilder builder1 = new DummyBuilder();
		IResource output1 = createResource("someOutput").output();
		Task<?> task1 = builder1.addInput(input).addOutput(output1).create(null);
		String key1 = ResourceCacheKey.calculate(task1, createOptions(1), output1);

		System.out.println("testProjectOutputs output sha1");
		DummyBuilder builder2 = new DummyBuilder();
		IResource output2 = createResource("someOutput").output();
		Task<?> task2 = builder2.addInput(input).addOutput(output2).create(null);
		String key2 = ResourceCacheKey.calculate(task2, createOptions(1), output2);

		assertNotEquals(key1, key2);
	}
}
