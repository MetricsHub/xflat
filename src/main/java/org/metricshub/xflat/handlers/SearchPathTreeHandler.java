package org.metricshub.xflat.handlers;

/*-
 * ╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲
 * XFlat Utility
 * ჻჻჻჻჻჻
 * Copyright (C) 2023 - 2025 MetricsHub
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

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.metricshub.xflat.Utils;
import org.metricshub.xflat.exceptions.XFlatException;
import org.metricshub.xflat.exceptions.XFlatRunTimeException;
import org.metricshub.xflat.types.SearchPathElement;
import org.metricshub.xflat.types.SearchPathElementAttribute;
import org.metricshub.xflat.types.SearchPathElementProperty;
import org.metricshub.xflat.types.SearchPathNode;

public class SearchPathTreeHandler {

	private SearchPathTreeHandler() { }

	private static final String ATTRIBUTE_TAG = ">";
	private static final String SLASH = "/";
	private static final String ELEMENT_BEFORE_TAG = "..";

	/**
	 * <p>Build a search path tree node with a properties list and a root tag.</p>
	 * <p>Example: </p>
	 * <pre>
	 * properties:
	 * "E"
	 * "B&gt;B1"
	 * "B&gt;B2"
	 * "B/C/D"
	 *
	 * rootPath="A"
	 * </pre>
	 * <p>==&gt;</p>
	 * <pre>
	 *  root_tree +-&gt; element_A +-&gt; property_E
	 *                                            +-&gt; element_B +-&gt; property_attribute_B1
	 *                                                                     +-&gt; property_attribute_B2
	 *                                                                     +-&gt; element_C +-&gt; property_D
	 * </pre>
	 *
	 * @param propertiesPathList A string list containing the paths to properties to retrieve. (mandatory)
	 * @param rootTag A string containing the XML tags path to the first element to convert. example: /rootTag/tag2
	 * (Mandatory)
	 * @return The first node of the search path tree
	 * @throws XFlatException for errors in the search path tree build
	 */
	public static SearchPathNode build(
			final List<String> propertiesPathList,
			final String rootTag) throws XFlatException {

		// For security because build is a public function but in practical, it's impossible.
		Utils.checkNonNull(propertiesPathList, "propertiesPathList");
		Utils.checkNonBlank(rootTag, "rootTag");

		final String root = new StringBuilder()
				.append(SLASH)
				.append(rootTag.replaceAll("\\s", Utils.EMPTY))
				.append(SLASH)
				.toString();

		if (propertiesPathList.isEmpty()) {
			throw new XFlatException("Should have at least one property.");
		}

		try {

			// Combine the rootPath and the properties list into an ordered searchPathElements list.
			// rootPath = /
			// properties:
			// A/B>B1
			// A/B>B2
			// A/B/C/D
			// A/E
			// ==>
			// root_tree, element_A, property_E
			// root_tree, element_A, element_B, property_attribute_B1
			// root_tree, element_A, element_B, property_attribute_B2
			// root_tree, element_A, element_B, element_C, property_D

			final List<Deque<SearchPathElement>> searchPathElements = IntStream
					.range(0, propertiesPathList.size())
					.mapToObj(i -> buildSearchPathElements(i, propertiesPathList.get(i), root))
					.sorted((q1,q2) -> q1.stream().map(SearchPathElement::getName).collect(Collectors.joining(SLASH))
						.compareToIgnoreCase(q2.stream().map(SearchPathElement::getName)
																	.collect(Collectors.joining(SLASH))))
					.collect(Collectors.toList());

			return buildSearchPathNodes(searchPathElements);

		} catch (final XFlatRunTimeException e) {
			throw new XFlatException(e.getMessage(), e);
		}
	}

	/**
	 * Build the search path elements for a property path.
	 *
	 * @param index Index of the property in the properties list
	 * @param propertyPath The property path
	 * @param rootTag The root tag
	 * @return The search path elements of the property
	 */
	static Deque<SearchPathElement> buildSearchPathElements(
			final int index,
			final String propertyPath,
			final String rootTag) {
		// For security because build is a public function but in practical, it's impossible.
		Utils.checkNonBlank(propertyPath, "propertyPath");

		final String path = new StringBuilder(rootTag)
				.append(propertyPath)
				.toString()
				.replace("/>", ATTRIBUTE_TAG);

		final List<String> pathElements = Stream.of(path.split(SLASH))
				.filter(Utils::isNotBlank)
				.collect(Collectors.toList());

		final Queue<String> rootTags = Stream.of(rootTag.split(SLASH))
				.filter(Utils::isNotBlank)
				.collect(Collectors.toCollection(LinkedList::new));

		// normalize path by changing .. to upper element
		final Deque<SearchPathElement> pathElementQueue = new LinkedList<>();
		for (final String element: pathElements) {

			final String rootTagElement = rootTags.poll();

			if (ELEMENT_BEFORE_TAG.equals(element)) {
				pathElementQueue.removeLast();
			} else {
				final SearchPathElement precedent = pathElementQueue.peekLast();
				if (precedent != null && precedent.getName().contains(ATTRIBUTE_TAG)) {
					throw new XFlatRunTimeException(
							String.format(
									"attribute %s is not the last element of the searchingPath %s",
									precedent,
									path));
				}
				pathElementQueue.add(
						new SearchPathElement(element, rootTagElement != null && element.contains(rootTagElement)));
			}
		}

		// change last element in searched property
		final SearchPathElement lastElement = pathElementQueue.removeLast();
		if (lastElement.getName().contains(ATTRIBUTE_TAG)) {
			final String[] elements = lastElement.getName().split(ATTRIBUTE_TAG);
			if (elements.length != 2) {
				throw new XFlatRunTimeException(String.format(
						"Invalide attribute tag in element %s of the searchingPath %s", lastElement.getName(), path));
			}

			if (!ELEMENT_BEFORE_TAG.equals(elements[0])) {
				pathElementQueue.add(new SearchPathElement(elements[0], lastElement.isFromRootTag()));
			}
			pathElementQueue.add(new SearchPathElementAttribute(index, elements[1]));

		} else {
			pathElementQueue.add(new SearchPathElementProperty(index, lastElement.getName()));
		}

		return pathElementQueue;
	}

	/**
	 * <p>Convert a searchPathElements list to a tree node.</p>
	 * <p>Build a tree node from a searchPathElements list.</p>
	 * <p>  Example: </p>
	 * <pre>
	 * root_tree, element_A, property_E
	 * root_tree, element_A, element_B, property_attribute_B1
	 * root_tree, element_A, element_B, property_attribute_B2
	 * root_tree, element_A, element_B, element_C, property_D
	 * </pre>
	 * <p/>
	 * <p>==></p>
	 * <p/>
	 * <pre>
	 *  root_tree +-> element_A +-> property_E
	 *                                            +-> element_B +-> property_attribute_B1
	 *                                                                     +-> property_attribute_B2
	 *                                                                     +-> element_C +-> property_D
	 * </pre>
	 *
	 * @param searchPathElements
	 * @return
	 */
	static SearchPathNode buildSearchPathNodes(final List<Deque<SearchPathElement>> searchPathElements) {

		SearchPathNode rootTreeNode = null;
		final Map<Integer, SearchPathNode> previousNodeMap = new HashMap<>();

		final int maxQueueSize = searchPathElements.stream().mapToInt(Deque::size).max().orElse(0);
		for (int i = 0; i < maxQueueSize; i++) {

			final Map<Integer, SearchPathNode> currentNodeMap = new HashMap<>();
			final Map<SearchPathElement, SearchPathNode> elementsFoundMap = new HashMap<>();

			for (int elementIndex = 0; elementIndex < searchPathElements.size(); elementIndex++) {
				final SearchPathElement element = searchPathElements.get(elementIndex).pollFirst();
				if (element == null) {
					continue;
				}

				final SearchPathNode currentNode = elementsFoundMap.computeIfAbsent(element, SearchPathNode::new);

				if (rootTreeNode == null) {
					rootTreeNode = currentNode;
				}

				currentNodeMap.put(elementIndex, currentNode);

				// link the previous node with the current node fot the current search path elements list.
				final SearchPathNode previousNode = previousNodeMap.get(elementIndex);
				if (previousNode != null) {
					previousNode.addNode(currentNode);
				}
			}

			// the currentMap become the previous map for the next element to browse.
			previousNodeMap.clear();
			previousNodeMap.putAll(currentNodeMap);
		}

		return rootTreeNode;
	}
}
