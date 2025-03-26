package org.metricshub.xflat.handlers;

import static org.metricshub.xflat.Utils.EMPTY;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.metricshub.xflat.XFlatTestUtils;
import org.metricshub.xflat.exceptions.XFlatException;
import org.metricshub.xflat.exceptions.XFlatRunTimeException;
import org.metricshub.xflat.types.SearchPathElement;
import org.metricshub.xflat.types.SearchPathElementAttribute;
import org.metricshub.xflat.types.SearchPathElementProperty;
import org.metricshub.xflat.types.SearchPathNode;

class SearchPathTreeHandlerTest extends XFlatTestUtils {

	private static final SearchPathNode EXPECTED_SEARCH_PATH_NODE;
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

		EXPECTED_SEARCH_PATH_NODE = documentNode;
	}


	@Test
	void testBuild() throws Exception {

		final List<String> propertiesPathList = asList(
				"OS>name",
				"Owner",
				"Disks/Disk/Volumes/Volume>name",
				"Disks/Disk/Volumes/Volume/Subscribe",
				"Disks/Disk>name",
				"Disks/Disk>size",
				"Disks/Disk/Free");

		assertThrows(IllegalArgumentException.class, () -> SearchPathTreeHandler.build(null, ROOT_TAG));
		assertThrows(IllegalArgumentException.class, () -> SearchPathTreeHandler.build(propertiesPathList, null));
		assertThrows(IllegalArgumentException.class, () -> SearchPathTreeHandler.build(propertiesPathList, EMPTY));
		assertThrows(IllegalArgumentException.class, () -> SearchPathTreeHandler.build(propertiesPathList, " "));

		assertThrows(XFlatException.class, () -> SearchPathTreeHandler.build(emptyList(), ROOT_TAG));

		verifySearchPathNode(
				EXPECTED_SEARCH_PATH_NODE,
				SearchPathTreeHandler.build(propertiesPathList, ROOT_TAG));
	}

	@Test
	void testBuildSearchPathElements() {

		final String rootTag = "Document/";

		assertThrows(
				IllegalArgumentException.class, () -> SearchPathTreeHandler.buildSearchPathElements(0, null, rootTag));
		assertThrows(
				IllegalArgumentException.class, () -> SearchPathTreeHandler.buildSearchPathElements(0, "", rootTag));
		assertThrows(IllegalArgumentException.class, () -> SearchPathTreeHandler.buildSearchPathElements(0, " ", "/"));

		// check attribute should be the last property
		assertThrows(
				XFlatRunTimeException.class,
				() -> SearchPathTreeHandler.buildSearchPathElements(0, "OS>name/value", rootTag));

		// check attribute should be unique
		assertThrows(
				XFlatRunTimeException.class,
				() -> SearchPathTreeHandler.buildSearchPathElements(0, "OS>name>value", rootTag));

		{
			final Deque<SearchPathElement> expected = new LinkedList<>();
			expected.add(new SearchPathElement("root_tree", true));
			expected.add(new SearchPathElement("doc", true));
			expected.add(new SearchPathElementProperty(1, "name"));

			assertEquals(expected, SearchPathTreeHandler.buildSearchPathElements(1, "name", "root_tree/doc/"));
		}

		{
			final Deque<SearchPathElement> expected = new LinkedList<>();
			expected.add(new SearchPathElement("root_tree", true));
			expected.add(new SearchPathElement("Document", true));
			expected.add(new SearchPathElementProperty(1, "Owner"));

			assertEquals(
					expected,
					SearchPathTreeHandler.buildSearchPathElements(1, "Owner", "root_tree/Document//"));
		}

		{
			final Deque<SearchPathElement> expected = new LinkedList<>();
			expected.add(new SearchPathElement("root_tree", true));
			expected.add(new SearchPathElement("Document", true));
			expected.add(new SearchPathElement("OS", false));
			expected.add(new SearchPathElementAttribute(1, "name"));

			assertEquals(
					expected,
					SearchPathTreeHandler.buildSearchPathElements(1, "OS>name", "root_tree//Document/"));
		}

		{
			final Deque<SearchPathElement> expected = new LinkedList<>();
			expected.add(new SearchPathElement("root_tree", true));
			expected.add(new SearchPathElement("Document", true));
			expected.add(new SearchPathElementAttribute(1, "name"));

			assertEquals(
					expected,
					SearchPathTreeHandler.buildSearchPathElements(1, ">name", "root_tree/Document/"));
		}

		{
			final Deque<SearchPathElement> expected = new LinkedList<>();
			expected.add(new SearchPathElement("root_tree", true));
			expected.add(new SearchPathElement("doc", true));
			expected.add(new SearchPathElement("elem", true));
			expected.add(new SearchPathElementAttribute(1, "name"));

			assertEquals(
					expected,
					SearchPathTreeHandler.buildSearchPathElements(1, "..>name", "root_tree/doc/elem/"));
		}

		{
			final Deque<SearchPathElement> expected = new LinkedList<>();
			expected.add(new SearchPathElement("root_tree", true));
			expected.add(new SearchPathElement("doc", true));
			expected.add(new SearchPathElement("Disks", true));
			expected.add(new SearchPathElementAttribute(1, "total"));

			assertEquals(
					expected,
					SearchPathTreeHandler.buildSearchPathElements(1, "../..>total", "root_tree/doc/Disks/Disk/"));
		}

		{
			final Deque<SearchPathElement> expected = new LinkedList<>();
			expected.add(new SearchPathElement("root_tree", true));
			expected.add(new SearchPathElement("Document", true));
			expected.add(new SearchPathElement("Disks", true));
			expected.add(new SearchPathElement("Disk", true));
			expected.add(new SearchPathElementProperty(1, "Free"));

			assertEquals(
					expected,
					SearchPathTreeHandler.buildSearchPathElements(1, "../Free", "root_tree/Document/Disks/Disk/Volumes/"));
		}
	}

	@Test
	void testBuildSearchPathNodes() {

		final Deque<SearchPathElement> owner = new LinkedList<>();
		owner.add(new SearchPathElement("Document", true));
		owner.add(new SearchPathElementProperty(1, "Owner"));

		final Deque<SearchPathElement> os = new LinkedList<>();
		os.add(new SearchPathElement("Document", true));
		os.add(new SearchPathElement("OS", false));
		os.add(new SearchPathElementAttribute(0, "name"));

		final Deque<SearchPathElement> diskName = new LinkedList<>();
		diskName.add(new SearchPathElement("Document", true));
		diskName.add(new SearchPathElement("Disks", false));
		diskName.add(new SearchPathElement("Disk", false));
		diskName.add(new SearchPathElementAttribute(4, "name"));

		final Deque<SearchPathElement> diskSize = new LinkedList<>();
		diskSize.add(new SearchPathElement("Document", true));
		diskSize.add(new SearchPathElement("Disks", false));
		diskSize.add(new SearchPathElement("Disk", false));
		diskSize.add(new SearchPathElementAttribute(5, "size"));

		final Deque<SearchPathElement> diskFree = new LinkedList<>();
		diskFree.add(new SearchPathElement("Document", true));
		diskFree.add(new SearchPathElement("Disks", false));
		diskFree.add(new SearchPathElement("Disk", false));
		diskFree.add(new SearchPathElementProperty(6, "Free"));

		final Deque<SearchPathElement> volumeName = new LinkedList<>();
		volumeName.add(new SearchPathElement("Document", true));
		volumeName.add(new SearchPathElement("Disks", false));
		volumeName.add(new SearchPathElement("Disk", false));
		volumeName.add(new SearchPathElement("Volumes", false));
		volumeName.add(new SearchPathElement("Volume", false));
		volumeName.add(new SearchPathElementAttribute(2, "name"));

		final Deque<SearchPathElement> volumeSize = new LinkedList<>();
		volumeSize.add(new SearchPathElement("Document", true));
		volumeSize.add(new SearchPathElement("Disks", false));
		volumeSize.add(new SearchPathElement("Disk", false));
		volumeSize.add(new SearchPathElement("Volumes", false));
		volumeSize.add(new SearchPathElement("Volume", false));
		volumeSize.add(new SearchPathElementProperty(3, "Subscribe"));

		final List<Deque<SearchPathElement>> searchPathElements =
				asList(owner, os, diskName, diskSize, diskFree, volumeName, volumeSize);

		verifySearchPathNode(
				EXPECTED_SEARCH_PATH_NODE,
				SearchPathTreeHandler.buildSearchPathNodes(searchPathElements));
	}

	private static void verifySearchPathNode(final SearchPathNode expected, final SearchPathNode actual) {

		assertEquals(expected.getElement(), actual.getElement());
		assertEquals(expected.getNexts().size(), actual.getNexts().size());

		if (expected.getNexts().size() == 0) {
			return;
		}

		final List<SearchPathNode> expectedNexts = expected.getNexts().stream().collect(toList());
		final List<SearchPathNode> actualNexts = actual.getNexts().stream().collect(toList());

		for (int i=0; i<expectedNexts.size(); i++) {
			verifySearchPathNode(expectedNexts.get(i), actualNexts.get(i));
		}
	}
}
