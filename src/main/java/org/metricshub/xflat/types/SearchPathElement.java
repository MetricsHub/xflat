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

public class SearchPathElement {

	private final String name;
	private final boolean fromRootTag;

	public SearchPathElement(final String name, final boolean fromRootTag) {
		this.name = name;
		this.fromRootTag = fromRootTag;
	}

	public String getName() {
		return name;
	}

	public boolean isFromRootTag() {
		return fromRootTag;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (fromRootTag ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SearchPathElement other = (SearchPathElement) obj;
		if (fromRootTag != other.fromRootTag) {
			return false;
		}
		if (name == null) {
			return other.name == null;
		} else {
			return name.equals(other.name);
		}
	}

	@Override
	public String toString() {
		return new StringBuilder("SearchPathElement [")
			.append("name=")
			.append(name)
			.append(", fromRootTag=")
			.append(fromRootTag)
			.append("]")
			.toString();
	}
}
