package org.sentrysoftware.xflat.handlers;

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

import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.sentrysoftware.xflat.Utils;
import org.sentrysoftware.xflat.exceptions.XFlatException;
import org.sentrysoftware.xflat.types.SearchPathElement;
import org.sentrysoftware.xflat.types.SearchPathElementAttribute;
import org.sentrysoftware.xflat.types.SearchPathElementProperty;
import org.sentrysoftware.xflat.types.SearchPathNode;

public class XmlHandler {

	private XmlHandler() { }

	private static final DocumentBuilderFactory DOCUMENT_FACTORY;
	static {
		DOCUMENT_FACTORY = DocumentBuilderFactory.newInstance(
				"com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl",
				ClassLoader.getSystemClassLoader()
		);
		DOCUMENT_FACTORY.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, Utils.EMPTY);
		DOCUMENT_FACTORY.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, Utils.EMPTY);
	}

	private final Set<String> linkKeys = new HashSet<>();
	private final Map<String, Map<Integer, String>> resultMap = new LinkedHashMap<>();

	/**
	 * Parse the XML recursively following the search path tree.
	 *
	 * @param xml The XML. (Mandatory)
	 * @param searchPathNode The first node of the search path tree. (Mandatory)
	 * @return The result map issued from the XML parsing
	 * @throws XFlatException for error in parsing
	 */
	public static Map<String, Map<Integer, String>> parse(
			final String xml,
			final SearchPathNode searchPathNode) throws XFlatException {

		// For security because build is a public function but in practical, it's impossible.
		Utils.checkNonNull(xml, "xml");
		Utils.checkNonNull(searchPathNode, "searchPathNode");

		try (final StringReader stringReader = new StringReader(xml)) {
			final Document document = DOCUMENT_FACTORY
					.newDocumentBuilder()
					.parse(new InputSource(stringReader));

			final XmlHandler xmlHandler = createXmlHandlerInstance();

			// recursively navigate the search path tree nodes.
			xmlHandler.initNavigation(searchPathNode, document);

			return xmlHandler.getResultMap();

		} catch (final Exception e) {
			throw new XFlatException("Error in parsing xml.", e);
		}
	}

	void initNavigation(final SearchPathNode searchPathNode, final Node node) {
		final Map<Integer, String> dataValues = new HashMap<>();
		final String linkKey = generateUniqueLinkKey();

		final SearchPathElement pathElement = searchPathNode.getElement();

		// "jump" to the requested node
		final NodeList nodeList =
				(node instanceof Document) ?
						((Document) node).getElementsByTagName(pathElement.getName()) :
							((Element) node).getElementsByTagName(pathElement.getName());
		final int totalNodes = nodeList.getLength();

		if (totalNodes == 0) {
			endNavigate(pathElement, linkKey, dataValues);

			// recursively navigate the search path tree nodes.
		} else {
			 if (totalNodes == 1) {
				 navigateNext(searchPathNode, nodeList.item(0), linkKey, dataValues);

			 } else {
				 for (int nodeIndex = 0; nodeIndex < totalNodes; nodeIndex++) {
						final String nextLinkKey = generateNextLinkKey(linkKey);
						navigateNext(searchPathNode, nodeList.item(nodeIndex), nextLinkKey, new HashMap<>(dataValues));
				 }
			 }
		}
	}

	String generateNextLinkKey(final String linkKey) {
		return new StringBuilder()
				.append(linkKey)
				.append(ResultHandler.LINK_SEPARATOR)
				.append(generateUniqueLinkKey())
				.toString();
	}

	void navigateNext(
			final SearchPathNode searchPathNode,
			final Node node,
			final String linkKey,
			final Map<Integer, String> dataValues) {
		if (searchPathNode.getNexts().isEmpty()) {
			endNavigate(linkKey, dataValues);
			return;
		}

		for (final SearchPathNode next : searchPathNode.getNexts()) {

			final SearchPathElement pathElement = next.getElement();

			if (pathElement instanceof SearchPathElementAttribute) {
				final Element element = (Element) node;
				final Attr attr = (Attr) element.getAttributes().getNamedItem(pathElement.getName());
				final String value = attr != null ? attr.getValue() : null;
				final SearchPathElementAttribute searchPathElementAttribute = (SearchPathElementAttribute) pathElement;

				dataValues.put(searchPathElementAttribute.getId(), value);

				navigateNext(next, element, linkKey, dataValues);

			} else {
				// Getting all the node children having the next seached element name
				final NodeList children = node.getChildNodes();
				final List<Element> elements = IntStream.range(0, children.getLength())
						.mapToObj(children::item)
						.filter(child -> pathElement.getName().equals(child.getNodeName()))
						.map(Element.class::cast)
						.collect(Collectors.toList());

				if (elements.isEmpty()) {
					endNavigate(pathElement, linkKey, dataValues);

				} else if (elements.size() == 1) {
					navigateElement(next, elements.get(0), linkKey, dataValues);

				} else {
					for (final Element element : elements) {
						final String nextLinkKey = generateNextLinkKey(linkKey);
						navigateElement(next, element, nextLinkKey, new HashMap<>(dataValues));
					}
				}
			}
		}
	}

	void navigateElement(
			final SearchPathNode searchPathNode,
			final Element element,
			final String linkKey,
			final Map<Integer, String> dataValues) {

		if (searchPathNode.getElement() instanceof SearchPathElementProperty) {
			final SearchPathElementProperty searchPathElementProperty =
					(SearchPathElementProperty) searchPathNode.getElement();
			dataValues.put(searchPathElementProperty.getId(), element.getTextContent());
		}

		navigateNext(searchPathNode, element, linkKey, dataValues);
	}

	void endNavigate(
			final SearchPathElement searchPathElement,
			final String linkKey,
			final Map<Integer, String> dataValues) {
		if (searchPathElement.isFromRootTag()) {
			dataValues.put(ResultHandler.ROOT_TAG_NOT_FOUND, Utils.EMPTY);
		}
		endNavigate(linkKey, dataValues);
	}

	void endNavigate(
			final String linkKey,
			final Map<Integer, String> dataValues) {
		getResultMap().compute(
				linkKey,
				(key, value) -> value == null ?
						dataValues :
							// merge result value map with dataValues map
							Stream.concat(value.entrySet().stream(), dataValues.entrySet().stream())
							.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (oldValue, newValue) -> oldValue)));
	}

	String generateUniqueLinkKey() {
		// generate a random Id
		final String linkKey = Integer.toHexString(Objects.hash(Math.random()));

		// if the Id already exist, generate a new one
		if (linkKeys.contains(linkKey)) {
			return generateUniqueLinkKey();
		}

		// Otherwise put it in the set and return the Id
		linkKeys.add(linkKey);
		return linkKey;
	}

	static XmlHandler createXmlHandlerInstance() {
		return new XmlHandler();
	}

	public Map<String, Map<Integer, String>> getResultMap() {
		return resultMap;
	}
}
