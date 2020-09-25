/*
 * Copyright © 2020 PRISMA European Capacity Platform GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.prismacapacity.spring.cqs.apt;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ManifestHelper {
	public static String getVersion(String implTitle) {

		Enumeration<URL> resEnum;
		try {
			resEnum = CqsProcessor.class.getClassLoader().getResources(JarFile.MANIFEST_NAME);
			while (resEnum.hasMoreElements()) {
				try {
					URL url = resEnum.nextElement();
					InputStream is = url.openStream();
					if (is != null) {
						Manifest manifest = new Manifest(is);
						Attributes mainAttribs = manifest.getMainAttributes();
						String name = mainAttribs.getValue("Implementation-Title");
						if (name.startsWith(implTitle)) {
							String version = mainAttribs.getValue("Implementation-Version");
							if (version != null) {
								return version;
							}
						}
					}
				} catch (Exception e) {
				}
			}
		} catch (IOException e1) {
		}
		return null;
	}
}
