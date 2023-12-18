package org.sentrysoftware.xflat;

/*-
 * ╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲
 * XFlat Utility
 * ჻჻჻჻჻჻
 * Copyright 2023 Sentry Software
 * ჻჻჻჻჻჻
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱
 */

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sentrysoftware.xflat.handlers.SearchPathTreeHandler;
import org.sentrysoftware.xflat.handlers.XmlHandler;
import org.sentrysoftware.xflat.types.SearchPathNode;
import org.sentrysoftware.xflat.handlers.ResultHandler;
import org.sentrysoftware.xflat.exceptions.XFlatException;



public class XFlat {

	private XFlat() { }

	/**
	 * Parse a XML with the argument properties into a list of values list.
	 *
	 * @param xml The XML (Mandatory)
	 * @param properties A string containing the paths to properties to retrieve separated by a semi-colon character.
	 * If the property comes from an attribute, it will be preceded by a superior character: '&gt;'.  (Mandatory)
	 * @param rootTag A string containing the first element xml tags path to convert. example: /rootTag/tag2 (Mandatory)
	 * @return The list of values list.
	 * @throws XFlatException
	 */
	public static List<List<String>> parseXml(
			final String xml,
			final String properties,
			final String rootTag) throws XFlatException {
		Utils.checkNonBlank(xml, "xml");
		Utils.checkNonBlank(properties, "properties");
		Utils.checkNonBlank(rootTag, "rootTag");

		// Init complete search path for each properties.
		final List<String> propertiesList = Stream.of(properties.replaceAll("\\s", Utils.EMPTY).split(";"))
				.filter(property -> !Utils.isBlank(property))
				.collect(Collectors.toList());

		final SearchPathNode rootTreeNode = SearchPathTreeHandler.build(propertiesList, rootTag);

		final Map<String, Map<Integer, String>> dataValueNodesMap = XmlHandler.parse(xml, rootTreeNode);

		return ResultHandler.arrange(dataValueNodesMap, propertiesList.size());
	}
}
