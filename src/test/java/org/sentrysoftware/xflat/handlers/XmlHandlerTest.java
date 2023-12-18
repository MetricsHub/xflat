package org.sentrysoftware.xflat.handlers;

import static org.sentrysoftware.xflat.Utils.EMPTY;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.sentrysoftware.xflat.XFlatTestUtils;
import org.sentrysoftware.xflat.exceptions.XFlatException;
import org.sentrysoftware.xflat.types.SearchPathElement;
import org.sentrysoftware.xflat.types.SearchPathElementAttribute;
import org.sentrysoftware.xflat.types.SearchPathElementProperty;
import org.sentrysoftware.xflat.types.SearchPathNode;

class XmlHandlerTest extends XFlatTestUtils {

	private static final SearchPathNode SEARCH_PATH_NODE;
	static {
		final SearchPathNode ownerNode = new SearchPathNode(new SearchPathElementProperty(1, "Owner"));

		final SearchPathNode osNameNode = new SearchPathNode(new SearchPathElementAttribute(0, "name"));
		final SearchPathNode osNode = new SearchPathNode(new SearchPathElement("OS", false));
		osNode.addNode(osNameNode);

		final SearchPathNode volumeNameNode = new SearchPathNode(new SearchPathElementAttribute(2, "name"));
		final SearchPathNode volumeSubscribeNode =
				new SearchPathNode(new SearchPathElementProperty(3, "Subscribe"));
		final SearchPathNode volumeNode = new SearchPathNode(new SearchPathElement("Volume", false));
		volumeNode.addNode(volumeNameNode);
		volumeNode.addNode(volumeSubscribeNode);

		final SearchPathNode volumesNode = new SearchPathNode(new SearchPathElement("Volumes", false));
		volumesNode.addNode(volumeNode);

		final SearchPathNode diskNameNode = new SearchPathNode(new SearchPathElementAttribute(4, "name"));
		final SearchPathNode diskSizeNode = new SearchPathNode(new SearchPathElementAttribute(5, "size"));
		final SearchPathNode diskFreeNode = new SearchPathNode(new SearchPathElementProperty(6, "Free"));
		final SearchPathNode diskNode = new SearchPathNode(new SearchPathElement("Disk", false));
		diskNode.addNode(diskNameNode);
		diskNode.addNode(diskSizeNode);
		diskNode.addNode(diskFreeNode);
		diskNode.addNode(volumesNode);

		final SearchPathNode disksNode = new SearchPathNode(new SearchPathElement("Disks", false));
		disksNode.addNode(diskNode);

		final SearchPathNode documentNode = new SearchPathNode(new SearchPathElement("Document", true));
		documentNode.addNode(ownerNode);
		documentNode.addNode(osNode);
		documentNode.addNode(disksNode);

		SEARCH_PATH_NODE = documentNode;
	}

	@Test
	void testParse() throws Exception {
		final String xml = getXml("test.xml");

		assertThrows(IllegalArgumentException.class, () -> XmlHandler.parse(null, SEARCH_PATH_NODE));
		assertThrows(IllegalArgumentException.class, () -> XmlHandler.parse(xml, null));

		assertThrows(XFlatException.class, () -> XmlHandler.parse(EMPTY, SEARCH_PATH_NODE));
		assertThrows(XFlatException.class, () -> XmlHandler.parse("<?xml version=\"1.0\"?>", SEARCH_PATH_NODE));

		final Map<String, Map<Integer, String>> actual = XmlHandler.parse("<Document/>", SEARCH_PATH_NODE);
		assertEquals(1, actual.size());
		assertEquals(emptyMap(), actual.values().stream().findFirst().get());

		final Map<String, Map<Integer, String>> expected = new LinkedHashMap<>();
		{
			final Map<Integer, String> values = new HashMap<>();
			values.put(0, "Linux");
			values.put(1, "User");
			expected.put("1fb7505", values);
		}
		{
			final Map<Integer, String> values = new HashMap<>();
			values.put(1, "User");
			values.put(2, "Vol1");
			values.put(3, "600");
			values.put(4, "Disk1");
			values.put(5, "1000");
			values.put(6, "500");
			expected.put("1fb7505=>82e176f9", values);
		}
		{
			final Map<Integer, String> values = new HashMap<>();
			values.put(1, "User");
			values.put(4, "Disk2");
			values.put(5, "2000");
			values.put(6, "750");
			expected.put("1fb7505=>fa67f9d8", values);
		}
		{
			final Map<Integer, String> values = new HashMap<>();
			values.put(1, "User");
			values.put(4, "Disk3");
			values.put(5, "2900");
			values.put(6, "1500");
			expected.put("1fb7505=>62dc3ae1", values);
		}
		{
			final Map<Integer, String> values = new HashMap<>();
			values.put(1, "User");
			values.put(2, "Vol3.0");
			values.put(3, "3000");
			values.put(4, "Disk3");
			values.put(5, "2900");
			values.put(6, "1500");
			expected.put("1fb7505=>62dc3ae1=>69442fb2", values);
		}
		{
			final Map<Integer, String> values = new HashMap<>();
			values.put(1, "User");
			values.put(2, "Vol3.1");
			values.put(3, "3100");
			values.put(4, "Disk3");
			values.put(5, "2900");
			values.put(6, "1500");
			expected.put("1fb7505=>62dc3ae1=>39622424", values);
		}
		{
			final Map<Integer, String> values = new HashMap<>();
			values.put(1, "User");
			values.put(2, "Vol3.2");
			values.put(3, "3200");
			values.put(4, "Disk3");
			values.put(5, "2900");
			values.put(6, "1500");
			expected.put("1fb7505=>62dc3ae1=>bc8a4630", values);
		}

		final Map<String, Map<Integer, String>> resultMap = new LinkedHashMap<>();
		final XmlHandler xmlHandler = mock(XmlHandler.class);

		try (final MockedStatic<XmlHandler> mockedXmlHandler = mockStatic(XmlHandler.class)) {
			mockedXmlHandler.when(XmlHandler::createXmlHandlerInstance).thenReturn(xmlHandler);
			mockedXmlHandler.when(() -> XmlHandler.parse(xml, SEARCH_PATH_NODE)).thenCallRealMethod();

			doReturn(resultMap).when(xmlHandler).getResultMap();

			doReturn("1fb7505").when(xmlHandler).generateUniqueLinkKey();

			doReturn(
			"1fb7505=>82e176f9",
			"1fb7505=>fa67f9d8",
			"1fb7505=>62dc3ae1")
			.when(xmlHandler).generateNextLinkKey("1fb7505");

			doReturn(
			"1fb7505=>62dc3ae1=>69442fb2",
			"1fb7505=>62dc3ae1=>39622424",
			"1fb7505=>62dc3ae1=>bc8a4630")
			.when(xmlHandler).generateNextLinkKey("1fb7505=>62dc3ae1");

			doCallRealMethod().when(xmlHandler).initNavigation(any(SearchPathNode.class), any(Node.class));

			doCallRealMethod().when(xmlHandler).navigateNext(
					any(SearchPathNode.class),
					any(Node.class),
					anyString(),
					anyMap());

			doCallRealMethod().when(xmlHandler).navigateElement(
					any(SearchPathNode.class),
					any(Element.class),
					anyString(),
					anyMap());

			doCallRealMethod().when(xmlHandler).endNavigate(any(SearchPathElement.class), anyString(), anyMap());

			doCallRealMethod().when(xmlHandler).endNavigate(anyString(), anyMap());

			assertEquals(expected, XmlHandler.parse(xml, SEARCH_PATH_NODE));
		}
	}
}
