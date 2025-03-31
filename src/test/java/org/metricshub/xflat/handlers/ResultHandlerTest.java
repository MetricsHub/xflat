package org.metricshub.xflat.handlers;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.metricshub.xflat.Utils.EMPTY;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ResultHandlerTest {

	@Test
	void testArrange() {
		assertEquals(emptyList(), ResultHandler.arrange(null, 0));
		assertEquals(emptyList(), ResultHandler.arrange(emptyMap(), 0));

		final int totalProperties = 7;
		final Map<String, Map<Integer, String>> rowValuesMap = new LinkedHashMap<>();
		{
			final Map<Integer, String> values = new HashMap<>();
			values.put(0, "Linux");
			values.put(1, "User");
			rowValuesMap.put("1fb7505", values);
		}
		{
			final Map<Integer, String> values = new HashMap<>();
			values.put(1, "User");
			values.put(2, "Vol1");
			values.put(3, "600");
			values.put(4, "Disk1");
			values.put(5, "1000");
			values.put(6, "500");
			rowValuesMap.put("1fb7505=>82e176f9", values);
		}
		{
			final Map<Integer, String> values = new HashMap<>();
			values.put(1, "User");
			values.put(4, "Disk2");
			values.put(5, "2000");
			values.put(6, "750");
			rowValuesMap.put("1fb7505=>fa67f9d8", values);
		}
		{
			final Map<Integer, String> values = new HashMap<>();
			values.put(1, "User");
			values.put(4, "Disk3");
			values.put(5, "2900");
			values.put(6, "1500");
			rowValuesMap.put("1fb7505=>62dc3ae1", values);
		}
		{
			final Map<Integer, String> values = new HashMap<>();
			values.put(1, "User");
			values.put(2, "Vol3.0");
			values.put(3, "3000");
			values.put(4, "Disk3");
			values.put(5, "2900");
			values.put(6, "1500");
			rowValuesMap.put("1fb7505=>62dc3ae1=>69442fb2", values);
		}
		{
			final Map<Integer, String> values = new HashMap<>();
			values.put(1, "User");
			values.put(2, "Vol3.1");
			values.put(3, "3100");
			values.put(4, "Disk3");
			values.put(5, "2900");
			values.put(6, "1500");
			rowValuesMap.put("1fb7505=>62dc3ae1=>39622424", values);
		}
		{
			final Map<Integer, String> values = new HashMap<>();
			values.put(1, "User");
			values.put(2, "Vol3.2");
			values.put(3, "3200");
			values.put(4, "Disk3");
			values.put(5, "2900");
			values.put(6, "1500");
			rowValuesMap.put("1fb7505=>62dc3ae1=>bc8a4630", values);
		}

		final List<List<String>> expected = asList(
			asList("Linux", "User", "Vol1", "600", "Disk1", "1000", "500"),
			asList("Linux", "User", "", "", "Disk2", "2000", "750"),
			asList("Linux", "User", "Vol3.0", "3000", "Disk3", "2900", "1500"),
			asList("Linux", "User", "Vol3.1", "3100", "Disk3", "2900", "1500"),
			asList("Linux", "User", "Vol3.2", "3200", "Disk3", "2900", "1500")
		);

		assertEquals(expected, ResultHandler.arrange(rowValuesMap, totalProperties));
	}

	@Test
	void testLinkAndMergeRowMap() {
		// First iteration of the example
		{
			final Map<Integer, String> firstLevelMapValues = new HashMap<>();
			firstLevelMapValues.put(0, "Linux");
			firstLevelMapValues.put(1, "User");
			final Map<String, Map<Integer, String>> firstLevelMap = singletonMap("1fb7505", firstLevelMapValues);

			final Map<String, Map<Integer, String>> remainingMap = new LinkedHashMap<>();
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(1, "User");
				values.put(2, "Vol1");
				values.put(3, "600");
				values.put(4, "Disk1");
				values.put(5, "1000");
				values.put(6, "500");
				remainingMap.put("1fb7505=>82e176f9", values);
			}
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(1, "User");
				values.put(4, "Disk2");
				values.put(5, "2000");
				values.put(6, "750");
				remainingMap.put("1fb7505=>fa67f9d8", values);
			}
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(1, "User");
				values.put(4, "Disk3");
				values.put(5, "2900");
				values.put(6, "1500");
				remainingMap.put("1fb7505=>62dc3ae1", values);
			}
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(1, "User");
				values.put(2, "Vol3.0");
				values.put(3, "3000");
				values.put(4, "Disk3");
				values.put(5, "2900");
				values.put(6, "1500");
				remainingMap.put("1fb7505=>62dc3ae1=>69442fb2", values);
			}
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(1, "User");
				values.put(2, "Vol3.1");
				values.put(3, "3100");
				values.put(4, "Disk3");
				values.put(5, "2900");
				values.put(6, "1500");
				remainingMap.put("1fb7505=>62dc3ae1=>39622424", values);
			}
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(1, "User");
				values.put(2, "Vol3.2");
				values.put(3, "3200");
				values.put(4, "Disk3");
				values.put(5, "2900");
				values.put(6, "1500");
				remainingMap.put("1fb7505=>62dc3ae1=>bc8a4630", values);
			}

			final Map<String, Map<Integer, String>> expected = new LinkedHashMap<>();
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(0, "Linux");
				values.put(1, "User");
				values.put(2, "Vol1");
				values.put(3, "600");
				values.put(4, "Disk1");
				values.put(5, "1000");
				values.put(6, "500");
				expected.put("82e176f9", values);
			}
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(0, "Linux");
				values.put(1, "User");
				values.put(4, "Disk2");
				values.put(5, "2000");
				values.put(6, "750");
				expected.put("fa67f9d8", values);
			}
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(0, "Linux");
				values.put(1, "User");
				values.put(4, "Disk3");
				values.put(5, "2900");
				values.put(6, "1500");
				expected.put("62dc3ae1", values);
			}
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(0, "Linux");
				values.put(1, "User");
				values.put(2, "Vol3.0");
				values.put(3, "3000");
				values.put(4, "Disk3");
				values.put(5, "2900");
				values.put(6, "1500");
				expected.put("62dc3ae1=>69442fb2", values);
			}
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(0, "Linux");
				values.put(1, "User");
				values.put(2, "Vol3.1");
				values.put(3, "3100");
				values.put(4, "Disk3");
				values.put(5, "2900");
				values.put(6, "1500");
				expected.put("62dc3ae1=>39622424", values);
			}
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(0, "Linux");
				values.put(1, "User");
				values.put(2, "Vol3.2");
				values.put(3, "3200");
				values.put(4, "Disk3");
				values.put(5, "2900");
				values.put(6, "1500");
				expected.put("62dc3ae1=>bc8a4630", values);
			}

			assertEquals(expected, ResultHandler.linkAndMergeRowMap(firstLevelMap, remainingMap));
		}

		// Second iteration of the example
		{
			final Map<String, Map<Integer, String>> firstLevelMap = new LinkedHashMap<>();
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(0, "Linux");
				values.put(1, "User");
				values.put(2, "Vol1");
				values.put(3, "600");
				values.put(4, "Disk1");
				values.put(5, "1000");
				values.put(6, "500");
				firstLevelMap.put("82e176f9", values);
			}
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(0, "Linux");
				values.put(1, "User");
				values.put(4, "Disk2");
				values.put(5, "2000");
				values.put(6, "750");
				firstLevelMap.put("fa67f9d8", values);
			}
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(0, "Linux");
				values.put(1, "User");
				values.put(4, "Disk3");
				values.put(5, "2900");
				values.put(6, "1500");
				firstLevelMap.put("62dc3ae1", values);
			}

			final Map<String, Map<Integer, String>> remainingMap = new LinkedHashMap<>();
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(0, "Linux");
				values.put(1, "User");
				values.put(2, "Vol3.0");
				values.put(3, "3000");
				values.put(4, "Disk3");
				values.put(5, "2900");
				values.put(6, "1500");
				remainingMap.put("62dc3ae1=>69442fb2", values);
			}
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(0, "Linux");
				values.put(1, "User");
				values.put(2, "Vol3.1");
				values.put(3, "3100");
				values.put(4, "Disk3");
				values.put(5, "2900");
				values.put(6, "1500");
				remainingMap.put("62dc3ae1=>39622424", values);
			}
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(0, "Linux");
				values.put(1, "User");
				values.put(2, "Vol3.2");
				values.put(3, "3200");
				values.put(4, "Disk3");
				values.put(5, "2900");
				values.put(6, "1500");
				remainingMap.put("62dc3ae1=>bc8a4630", values);
			}

			final Map<String, Map<Integer, String>> expected = new LinkedHashMap<>();
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(0, "Linux");
				values.put(1, "User");
				values.put(2, "Vol1");
				values.put(3, "600");
				values.put(4, "Disk1");
				values.put(5, "1000");
				values.put(6, "500");
				expected.put("82e176f9", values);
			}
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(0, "Linux");
				values.put(1, "User");
				values.put(4, "Disk2");
				values.put(5, "2000");
				values.put(6, "750");
				expected.put("fa67f9d8", values);
			}
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(0, "Linux");
				values.put(1, "User");
				values.put(2, "Vol3.0");
				values.put(3, "3000");
				values.put(4, "Disk3");
				values.put(5, "2900");
				values.put(6, "1500");
				expected.put("69442fb2", values);
			}
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(0, "Linux");
				values.put(1, "User");
				values.put(2, "Vol3.1");
				values.put(3, "3100");
				values.put(4, "Disk3");
				values.put(5, "2900");
				values.put(6, "1500");
				expected.put("39622424", values);
			}
			{
				final Map<Integer, String> values = new HashMap<>();
				values.put(0, "Linux");
				values.put(1, "User");
				values.put(2, "Vol3.2");
				values.put(3, "3200");
				values.put(4, "Disk3");
				values.put(5, "2900");
				values.put(6, "1500");
				expected.put("bc8a4630", values);
			}

			assertEquals(expected, ResultHandler.linkAndMergeRowMap(firstLevelMap, remainingMap));
		}
	}

	@Test
	void testGetValueData() {
		final int totalProperties = 4;

		{
			final Map<Integer, String> values = new HashMap<>();
			values.put(0, "a");
			values.put(1, "b");
			values.put(ResultHandler.ROOT_TAG_NOT_FOUND, EMPTY);

			assertEquals(Collections.emptyList(), ResultHandler.getValueData(values, totalProperties));
		}
		{
			final Map<Integer, String> values = new HashMap<>();
			values.put(0, "a");
			values.put(3, "d");
			values.put(1, "b");

			assertEquals(asList("a", "b", EMPTY, "d"), ResultHandler.getValueData(values, totalProperties));
		}
	}

	@Test
	void testRemoveFirstLinkPart() {
		assertNull(ResultHandler.removeFirstLinkPart(null));
		assertEquals(EMPTY, ResultHandler.removeFirstLinkPart(EMPTY));
		assertEquals("ec9950f0", ResultHandler.removeFirstLinkPart("ec9950f0"));
		assertEquals("8cd5fadb", ResultHandler.removeFirstLinkPart("8cd5fadb"));
		assertEquals("be5ebf49", ResultHandler.removeFirstLinkPart("ec9950f0=>be5ebf49"));
		assertEquals("74760e39", ResultHandler.removeFirstLinkPart("ec9950f0=>74760e39"));
		assertEquals("88f3b609=>42705d9", ResultHandler.removeFirstLinkPart("8cd5fadb=>88f3b609=>42705d9"));
		assertEquals("88f3b609=>8dcc003a", ResultHandler.removeFirstLinkPart("8cd5fadb=>88f3b609=>8dcc003a"));
	}
}
