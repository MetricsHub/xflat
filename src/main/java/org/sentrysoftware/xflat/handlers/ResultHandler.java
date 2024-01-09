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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.sentrysoftware.xflat.Utils;

public class ResultHandler {

	private ResultHandler() { }

	static final String LINK_SEPARATOR = "=>";
	static final int ROOT_TAG_NOT_FOUND = -1;

	/**
	 * <p>Arrange the result map issued from the XML parsing into a list of values list.</p>
	 * Link and merge the rows fragments and keep the row order
	 * Reorder the values inside a row and the missing properties values with an empty string
	 *
	 * @param rowValuesMap The result map issued from the XML parsing
	 * @param totalProperties the total number of properties
	 * @return The list of values list
	 */
	public static List<List<String>> arrange(
			final Map<String, Map<Integer, String>> rowValuesMap,
			final int totalProperties) {

		// if there's no map return empty list
		if (null == rowValuesMap || rowValuesMap.isEmpty()) {
			return new ArrayList<>();
		}

		// Recursively Link and merge the rows fragments and keep the row order.
		final List<Map<Integer, String>> linkedRows = recursiveLinkAndMergeRows(rowValuesMap);

		// Reorder the values inside a row and the missing properties values with an empty string
		return linkedRows.stream()
				.map(values -> getValueData(values, totalProperties))
				.collect(Collectors.toList());
	}

	static List<String> getValueData(final Map<Integer, String> values, final int totalProperties) {
		return values.containsKey(ROOT_TAG_NOT_FOUND) ?
				new ArrayList<>() :
				IntStream.range(0, totalProperties).boxed()
					.map(id -> values.getOrDefault(id, Utils.EMPTY))
					.collect(Collectors.toList());
	}

	private static List<Map<Integer, String>> recursiveLinkAndMergeRows(
			final Map<String, Map<Integer, String>> rowValuesMap) {

		// 	For example this rowValuesMap for start :

		// 	linkKey = "1fb7505"	,											row values map = {0=Linux, 1=User}
		// 	linkKey = "1fb7505=>82e176f9",							row values map = {1=User, 2=Vol1, 3=600, 4=Disk1, 5=1000, 6=500}
		// 	linkKey = "1fb7505=>fa67f9d8",							row values map = {1=User, 4=Disk2, 5=2000, 6=750}
		// 	linkKey = "1fb7505=>62dc3ae1",							row values map = {1=User, 4=Disk3, 5=2900, 6=1500}
		// 	linkKey = "1fb7505=>62dc3ae1=>69442fb2",		row values map = {1=User, 2=Vol3.0, 3=3000, 4=Disk3, 5=2900, 6=1500}
		// 	linkKey = "1fb7505=>62dc3ae1=>39622424",	row values map = {1=User, 2=Vol3.1, 3=3100, 4=Disk3, 5=2900, 6=1500}
		// 	linkKey = " 1fb7505=>62dc3ae1=>bc8a4630",	row values map = {1=User, 2=Vol3.2, 3=3200, 4=Disk3, 5=2900, 6=1500}

		// All the rows which linKey doesn't contain a link separator.
		// in our example firstLevelMap would contain only the first record.
		// 	linkKey = "1fb7505" , row values map = {0=Linux, 1=User}
		final Map<String, Map<Integer, String>> firstLevelMap = rowValuesMap.entrySet().stream()
				.filter(entry -> !entry.getKey().contains(LINK_SEPARATOR))
				.collect(Collectors.toMap(
						Entry::getKey,
						Entry::getValue,
						(oldValue, newValue) -> oldValue,
						LinkedHashMap::new));

		// if there's no first level left (like a final array), remove the first linkKey part for all the keys.
		if (firstLevelMap.isEmpty()) {
			final Map<String, Map<Integer, String>> rowValuesMapWithoutFirstLinks = rowValuesMap.entrySet().stream()
					.collect(Collectors.toMap(
							entry -> removeFirstLinkPart(entry.getKey()),
							Entry::getValue,
							(oldValue, newValue) -> oldValue,
							LinkedHashMap::new));

			return recursiveLinkAndMergeRows(rowValuesMapWithoutFirstLinks);
		}

		// All the rows which linKey contains a link separator
		// in our example remainingMap would contain the others records.
		// 	linkKey = "1fb7505=>82e176f9",							row values map = {1=User, 2=Vol1, 3=600, 4=Disk1, 5=1000, 6=500}
		// 	linkKey = "1fb7505=>fa67f9d8",							row values map = {1=User, 4=Disk2, 5=2000, 6=750}
		// 	linkKey = "1fb7505=>62dc3ae1",							row values map = {1=User, 4=Disk3, 5=2900, 6=1500}
		// 	linkKey = "1fb7505=>62dc3ae1=>69442fb2",		row values map = {1=User, 2=Vol3.0, 3=3000, 4=Disk3, 5=2900, 6=1500}
		// 	linkKey = "1fb7505=>62dc3ae1=>39622424",	row values map = {1=User, 2=Vol3.1, 3=3100, 4=Disk3, 5=2900, 6=1500}
		// 	linkKey = " 1fb7505=>62dc3ae1=>bc8a4630",	row values map = {1=User, 2=Vol3.2, 3=3200, 4=Disk3, 5=2900, 6=1500}
		final Map<String, Map<Integer, String>> remainingMap = rowValuesMap.entrySet().stream()
				.filter(entry -> entry.getKey().contains(LINK_SEPARATOR))
				.collect(Collectors.toMap(
						Entry::getKey,
						Entry::getValue,
						(oldValue, newValue) -> oldValue,
						LinkedHashMap::new));

		// if there's only first keys, end the recursion and return the list of values maps
		return remainingMap.isEmpty() ?
				// No more remaining map, return rows value list without the link keys.
				// It's not the most straight way to create the list but the reason is to keep the order in the map of the values to the List.
				firstLevelMap.entrySet().stream()
				.map(Entry::getValue)
				.collect(Collectors.toList()) :
					// merge the remaining keys with the first levels.
					recursiveLinkAndMergeRows(
							linkAndMergeRowMap(firstLevelMap, remainingMap));
	}

	static Map<String, Map<Integer, String>> linkAndMergeRowMap(
			final Map<String, Map<Integer, String>> firstLevelMap,
			final Map<String, Map<Integer, String>> remainingMap) {

		// In our example:

		// firstLevelMap:
		// 	linkKey = "1fb7505" , row values map = {0=Linux, 1=User}

		// remainingMap
		// 	linkKey = "1fb7505=>82e176f9",							row values map = {1=User, 2=Vol1, 3=600, 4=Disk1, 5=1000, 6=500}
		// 	linkKey = "1fb7505=>fa67f9d8",							row values map = {1=User, 4=Disk2, 5=2000, 6=750}
		// 	linkKey = "1fb7505=>62dc3ae1",							row values map = {1=User, 4=Disk3, 5=2900, 6=1500}
		// 	linkKey = "1fb7505=>62dc3ae1=>69442fb2",		row values map = {1=User, 2=Vol3.0, 3=3000, 4=Disk3, 5=2900, 6=1500}
		// 	linkKey = "1fb7505=>62dc3ae1=>39622424",	row values map = {1=User, 2=Vol3.1, 3=3100, 4=Disk3, 5=2900, 6=1500}
		// 	linkKey = " 1fb7505=>62dc3ae1=>bc8a4630",	row values map = {1=User, 2=Vol3.2, 3=3200, 4=Disk3, 5=2900, 6=1500}

		// firstLevelMap merged in remainingMap would return:
		// 	linkKey = "82e176f9",						row values map = {0=Linux, 1=User, 2=Vol1, 3=600, 4=Disk1, 5=1000, 6=500}
		// 	linkKey = "fa67f9d8",							row values map = {0=Linux, 1=User, 4=Disk2, 5=2000, 6=750}
		// 	linkKey = "62dc3ae1",						row values map = {0=Linux, 1=User, 4=Disk3, 5=2900, 6=1500}
		// 	linkKey = "62dc3ae1=>69442fb2",	row values map = {0=Linux, 1=User, 2=Vol3.0, 3=3000, 4=Disk3, 5=2900, 6=1500}
		// 	linkKey = "62dc3ae1=>39622424",	row values map = {0=Linux, 1=User, 2=Vol3.1, 3=3100, 4=Disk3, 5=2900, 6=1500}
		// 	linkKey = "62dc3ae1=>bc8a4630",	row values map = {0=Linux, 1=User, 2=Vol3.2, 3=3200, 4=Disk3, 5=2900, 6=1500}

		final Map<String, Map<Integer, String>> result = new LinkedHashMap<>();
		final Set<String> sharedLinks = new HashSet<>();

		// for each first level linkKey update all the related next linkKeys with
		// removing the first linkKey part of the key and merging the rows maps.
		firstLevelMap.forEach(
				(key, value) -> {
					final List<Entry<String, Map<Integer, String>>> shared = remainingMap.entrySet().stream()
							.filter(entry -> entry.getKey().startsWith(key))
							.collect(Collectors.toList());

					final Map<String, Map<Integer, String>> remainingMerged = shared.stream()
							.filter(entry -> entry.getKey().startsWith(key))
							.collect(Collectors.toMap(
									// remove the key and the unique linkKey part in to the new LinkKey
									entry -> entry.getKey().substring(key.length() + LINK_SEPARATOR.length()),
									// merge the row map.
									entry -> Stream.concat(entry.getValue().entrySet().stream(), value.entrySet().stream())
									.collect(Collectors.toMap(
											Entry::getKey,
											Entry::getValue,
											(oldValue, newValue) -> oldValue)),
									(oldValue, newValue) -> oldValue,
									LinkedHashMap::new));

					if (remainingMerged.isEmpty()) {
						result.put(key, value);
					} else {

						sharedLinks.addAll(shared.stream().map(Entry::getKey).collect(Collectors.toSet()));
						result.putAll(remainingMerged);
					}
				});

		// Remove the first links level to all all the links left that hasn't been merged into firstLink before
		// and add them to the result.
		final Map<String, Map<Integer, String>> remainingMapAfterMerged = remainingMap.entrySet().stream()
				.filter(entry -> !sharedLinks.contains(entry.getKey()))
				.collect(Collectors.toMap(
						entry -> removeFirstLinkPart(entry.getKey()),
						Entry::getValue,
						(oldValue, newValue) -> oldValue,
						LinkedHashMap::new));
		if (!remainingMapAfterMerged.isEmpty()) {
			result.putAll(remainingMapAfterMerged);
		}

		return result;
	}

	static String removeFirstLinkPart(final String link) {
		if (Utils.isBlank(link)) {
			return link;
		}
		final int indexOfLinkSeparator = link.indexOf(LINK_SEPARATOR);
		if (indexOfLinkSeparator == -1) {
			return link;
		}
		return link.substring(indexOfLinkSeparator + LINK_SEPARATOR.length());
	}
}
