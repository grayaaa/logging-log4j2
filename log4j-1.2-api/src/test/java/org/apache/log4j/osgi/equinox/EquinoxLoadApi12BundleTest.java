/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.log4j.osgi.equinox;

import org.apache.logging.log4j.osgi.equinox.AbstractEquinoxLoadBundleTest;
import org.junit.Assume;
import org.junit.Ignore;

/**
 * Tests loading the Core bundle into an Eclipse Equinox OSGi container.
 * <p>
 * Requires that "mvn package" has been previously run, otherwise test fails its JUnit {@link Assume}.
 * </p>
 * <p>
 * For example, on Windows: "mvn clean package -DskipTests & mvn test"
 * </p>
 */
@Ignore
public class EquinoxLoadApi12BundleTest extends AbstractEquinoxLoadBundleTest {

    // more?
}
