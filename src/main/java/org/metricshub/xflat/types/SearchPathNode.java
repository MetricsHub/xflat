package org.metricshub.xflat.types;

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

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class SearchPathNode {

	private static final int IS_BEFORE = -1;
	private static final int IS_AFTER = 1;

	private final SearchPathElement element;
	private final Set<SearchPathNode> nexts = new TreeSet<>(SearchPathNode::compare);

	public SearchPathNode(final SearchPathElement element) {
		this.element = element;
	}

	public void addNode(final SearchPathNode element) {
		nexts.add(element);
	}

	public SearchPathElement getElement() {
		return element;
	}

	public Set<SearchPathNode> getNexts() {
		return nexts;
	}

	@Override
	public int hashCode() {
		return Objects.hash(element, nexts);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof SearchPathNode)) {
			return false;
		}
		final SearchPathNode other = (SearchPathNode) obj;
		return Objects.equals(element, other.element) && Objects.equals(nexts, other.nexts);
	}

	@Override
	public String toString() {
		return "SearchPathNode [element=" + element + ", nexts=" + nexts + "]";
	}

	/**
	 * <p>compare element by type and name.
	 * the order is by type:
	 * <li>property in attribute</li>
	 * <li>property in tag</i>
	 * <li>element</li>
	 * <li>if the type is the same: compare by name</i>
	 * </p>
	 *
	 * @param node1
	 * @param node2
	 * @return
	 */
	private static int compare(final SearchPathNode node1, final SearchPathNode node2) {
		if ((node1.getElement() instanceof SearchPathElementAttribute) &&
				!(node2.getElement() instanceof SearchPathElementAttribute)) {
			return IS_BEFORE;
		}

		if (!(node1.getElement() instanceof SearchPathElementAttribute) &&
				(node2.getElement() instanceof SearchPathElementAttribute)) {
			return IS_AFTER;
		}

		if ((node1.getElement() instanceof SearchPathElementProperty) &&
				(node2.getElement() instanceof SearchPathElement)) {
			return IS_BEFORE;
		}

		if ((node1.getElement() instanceof SearchPathElement) &&
				(node2.getElement() instanceof SearchPathElementProperty)) {
			return IS_AFTER;
		}

		return node1.getElement().getName().compareToIgnoreCase(node2.getElement().getName());
	}
}
