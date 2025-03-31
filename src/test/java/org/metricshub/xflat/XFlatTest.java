package org.metricshub.xflat;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.metricshub.xflat.Utils.EMPTY;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.metricshub.xflat.exceptions.XFlatException;

class XFlatTest extends XFlatTestUtils {

	@Test
	void testParseXml() throws Exception {
		final String xml = getXml("test.xml");

		// check arguments
		assertThrows(IllegalArgumentException.class, () -> XFlat.parseXml(null, PROPERTIES, ROOT_TAG));
		assertThrows(IllegalArgumentException.class, () -> XFlat.parseXml(EMPTY, PROPERTIES, ROOT_TAG));
		assertThrows(IllegalArgumentException.class, () -> XFlat.parseXml(" ", PROPERTIES, ROOT_TAG));
		assertThrows(IllegalArgumentException.class, () -> XFlat.parseXml(xml, null, ROOT_TAG));
		assertThrows(IllegalArgumentException.class, () -> XFlat.parseXml(xml, EMPTY, ROOT_TAG));
		assertThrows(IllegalArgumentException.class, () -> XFlat.parseXml(xml, " ", ROOT_TAG));
		assertThrows(IllegalArgumentException.class, () -> XFlat.parseXml(xml, PROPERTIES, null));
		assertThrows(IllegalArgumentException.class, () -> XFlat.parseXml(xml, PROPERTIES, EMPTY));
		assertThrows(IllegalArgumentException.class, () -> XFlat.parseXml(xml, PROPERTIES, " "));

		assertThrows(XFlatException.class, () -> XFlat.parseXml(xml, ";", ROOT_TAG));

		// check invalid attributes
		assertThrows(XFlatException.class, () -> XFlat.parseXml(xml, "Disks>Disk>name", ROOT_TAG));
		assertThrows(XFlatException.class, () -> XFlat.parseXml(xml, "Disks/Disk>>name", ROOT_TAG));
		assertThrows(XFlatException.class, () -> XFlat.parseXml(xml, "Disks/Disk/>", ROOT_TAG));
		assertThrows(XFlatException.class, () -> XFlat.parseXml(xml, "Disks/Disk>", ROOT_TAG));
		assertThrows(XFlatException.class, () -> XFlat.parseXml(xml, "Disks/Disk>Volumes/Volume>name", ROOT_TAG));
		assertThrows(XFlatException.class, () -> XFlat.parseXml(xml, "Disks/Disk>name/Volumes/Volume>name", ROOT_TAG));

		// check missing tag in rooth path
		{
			final List<List<String>> expected = asList(
				asList("Vol1", "600"),
				emptyList(),
				asList("Vol3.0", "3000"),
				asList("Vol3.1", "3100"),
				asList("Vol3.2", "3200")
			);

			assertEquals(
				expected,
				XFlat.parseXml(
					xml,
					" \t\r\n > \t\r\n name \t\r\n;" + " \t\r\n Subscribe \t\r\n ",
					" \t \r \n /  \t \t \r \n  Document \t\r\n /  \t\r\n Disks  \t\r\n / \t\r\n Disk \t\r\n / \t\r\n Volumes \t\n\r / Volume "
				)
			);

			// check missing Document tag in root tag
			assertEquals(
				expected,
				XFlat.parseXml(
					xml,
					" \t\r\n > \t\r\n name \t\r\n; \t\r\n Subscribe \t\r\n ",
					" \t \r \n /  \t \t \r \n  Disks  \t\r\n / \t\r\n Disk \t\r\n / \t\r\n Volumes \t\n\r / Volume "
				)
			);
		}

		// Check upper ".." tag in properties
		{
			final List<List<String>> expected = asList(
				asList("Vol1", "Disk1", "600"),
				emptyList(),
				asList("Vol3.0", "Disk3", "3000"),
				asList("Vol3.1", "Disk3", "3100"),
				asList("Vol3.2", "Disk3", "3200")
			);

			assertEquals(
				expected,
				XFlat.parseXml(
					xml,
					" \t\r\n > \t\r\n name \t\r\n;" + " ../.. / .. > name;" + " \t\r\n Subscribe \t\r\n ",
					" \t \r \n /  \t \t \r \n  Document \t\r\n /  \t\r\n Disks  \t\r\n / \t\r\n Disk \t\r\n / \t\r\n Volumes \t\n\r / Volume "
				)
			);
		}
		{
			final List<List<String>> expected = asList(
				asList("Vol1", "Disk1", "600"),
				asList(EMPTY, "Disk2", EMPTY),
				asList("Vol3.0", "Disk3", "3000"),
				asList("Vol3.1", "Disk3", "3100"),
				asList("Vol3.2", "Disk3", "3200")
			);

			assertEquals(
				expected,
				XFlat.parseXml(
					xml,
					"Disks/Disk/Volumes/Volume > \t\r\n name \t\r\n;" +
					" Disks/Disk/Volumes/Volume/../../ .. > name;" +
					"Disks/Disk/Volumes/Volume/Subscribe \t\r\n ",
					" \t \r \n /  \t \t \r \n  Document \t\r\n "
				)
			);
		}

		final List<List<String>> expected = asList(
			asList("Linux", "User", "Vol1", "600", "Disk1", "1000", "500"),
			asList("Linux", "User", EMPTY, EMPTY, "Disk2", "2000", "750"),
			asList("Linux", "User", "Vol3.0", "3000", "Disk3", "2900", "1500"),
			asList("Linux", "User", "Vol3.1", "3100", "Disk3", "2900", "1500"),
			asList("Linux", "User", "Vol3.2", "3200", "Disk3", "2900", "1500")
		);

		assertEquals(expected, XFlat.parseXml(xml, PROPERTIES, ROOT_TAG));
	}

	@Test
	void testPartialRootPath() throws Exception {
		final String xml = getXml("test2.xml");
		final String properties = "id;name";

		// invalid rootTag
		assertEquals(asList(emptyList()), XFlat.parseXml(xml, properties, "document/properties"));
		assertEquals(asList(emptyList()), XFlat.parseXml(xml, properties, "document/properties/network"));
		assertEquals(asList(emptyList(), emptyList()), XFlat.parseXml(xml, properties, "properties/network"));

		// complete rootTag
		assertEquals(
			asList(asList("HPB7B952B1BA-subnet_one", "subnet_one")),
			XFlat.parseXml(xml, properties, "/document/network/properties")
		);

		// partial rootTag
		assertEquals(
			asList(asList("HPB7B952B1BA-subnet_one", "subnet_one")),
			XFlat.parseXml(xml, properties, "/network/properties")
		);

		assertEquals(
			asList(asList("HPB7B952B1BA-subnet_one", "subnet_one"), asList("bond-1", "bond-name")),
			XFlat.parseXml(xml, properties, "properties")
		);
	}

	@Test
	void testUCSEquipmentFan() throws Exception {
		final String rootTag = "/configResolveClass";

		final String properties =
			" >classId;" +
			"outConfigs/equipmentFan>dn; " +
			"outConfigs/equipmentFan>serial ;" +
			" outConfigs/equipmentFan>model;" +
			"outConfigs/equipmentFan>vendor ;" +
			"outConfigs/equipmentFan>operState ";

		final List<List<String>> expected = asList(
			asList("equipmentFan", "sys/switch-A/fan-module-1-1/fan-1", "N/A", "N10-FAN1", "Cisco Systems, Inc.", "operable"),
			asList("equipmentFan", "sys/switch-A/fan-module-1-1/fan-2", "N/A", "N10-FAN1", "Cisco Systems, Inc.", "operable"),
			asList("equipmentFan", "sys/switch-A/fan-module-1-1/fan-3", "N/A", "N10-FAN1", "Cisco Systems, Inc.", "operable"),
			asList("equipmentFan", "sys/switch-A/fan-module-1-1/fan-4", "N/A", "N10-FAN1", "Cisco Systems, Inc.", "operable"),
			asList("equipmentFan", "sys/switch-A/fan-module-1-1/fan-5", "N/A", "N10-FAN1", "Cisco Systems, Inc.", "operable"),
			asList("equipmentFan", "sys/switch-A/fan-module-1-1/fan-6", "N/A", "N10-FAN1", "Cisco Systems, Inc.", "operable"),
			asList("equipmentFan", "sys/switch-A/fan-module-1-2/fan-1", "N/A", "N10-FAN1", "Cisco Systems, Inc.", "operable"),
			asList("equipmentFan", "sys/switch-A/fan-module-1-2/fan-2", "N/A", "N10-FAN1", "Cisco Systems, Inc.", "operable"),
			asList("equipmentFan", "sys/switch-A/fan-module-1-2/fan-3", "N/A", "N10-FAN1", "Cisco Systems, Inc.", "operable"),
			asList("equipmentFan", "sys/switch-A/fan-module-1-2/fan-4", "N/A", "N10-FAN1", "Cisco Systems, Inc.", "operable"),
			asList("equipmentFan", "sys/switch-A/fan-module-1-2/fan-5", "N/A", "N10-FAN1", "Cisco Systems, Inc.", "operable"),
			asList("equipmentFan", "sys/switch-A/fan-module-1-2/fan-6", "N/A", "N10-FAN1", "Cisco Systems, Inc.", "operable"),
			asList(
				"equipmentFan",
				"sys/chassis-1/fan-module-1-1/fan-1",
				"NWG15030613",
				"N20-FAN5",
				"Cisco Systems Inc",
				"operable"
			),
			asList(
				"equipmentFan",
				"sys/chassis-1/fan-module-1-1/fan-2",
				"NWG15030613",
				"N20-FAN5",
				"Cisco Systems Inc",
				"operable"
			),
			asList(
				"equipmentFan",
				"sys/chassis-1/fan-module-1-2/fan-1",
				"NWG150305AQ",
				"N20-FAN5",
				"Cisco Systems Inc",
				"operable"
			),
			asList(
				"equipmentFan",
				"sys/chassis-1/fan-module-1-2/fan-2",
				"NWG150305AQ",
				"N20-FAN5",
				"Cisco Systems Inc",
				"operable"
			),
			asList(
				"equipmentFan",
				"sys/chassis-1/fan-module-1-3/fan-1",
				"NWG15030653",
				"N20-FAN5",
				"Cisco Systems Inc",
				"operable"
			),
			asList(
				"equipmentFan",
				"sys/chassis-1/fan-module-1-3/fan-2",
				"NWG15030653",
				"N20-FAN5",
				"Cisco Systems Inc",
				"operable"
			),
			asList(
				"equipmentFan",
				"sys/chassis-1/fan-module-1-4/fan-1",
				"NWG1503055C",
				"N20-FAN5",
				"Cisco Systems Inc",
				"operable"
			),
			asList(
				"equipmentFan",
				"sys/chassis-1/fan-module-1-4/fan-2",
				"NWG1503055C",
				"N20-FAN5",
				"Cisco Systems Inc",
				"operable"
			),
			asList(
				"equipmentFan",
				"sys/chassis-1/fan-module-1-5/fan-1",
				"NWG150305CM",
				"N20-FAN5",
				"Cisco Systems Inc",
				"operable"
			),
			asList(
				"equipmentFan",
				"sys/chassis-1/fan-module-1-5/fan-2",
				"NWG150305CM",
				"N20-FAN5",
				"Cisco Systems Inc",
				"operable"
			),
			asList(
				"equipmentFan",
				"sys/chassis-1/fan-module-1-6/fan-1",
				"NWG150306ZR",
				"N20-FAN5",
				"Cisco Systems Inc",
				"operable"
			),
			asList(
				"equipmentFan",
				"sys/chassis-1/fan-module-1-6/fan-2",
				"NWG150306ZR",
				"N20-FAN5",
				"Cisco Systems Inc",
				"operable"
			),
			asList(
				"equipmentFan",
				"sys/chassis-1/fan-module-1-7/fan-1",
				"NWG150305QP",
				"N20-FAN5",
				"Cisco Systems Inc",
				"operable"
			),
			asList(
				"equipmentFan",
				"sys/chassis-1/fan-module-1-7/fan-2",
				"NWG150305QP",
				"N20-FAN5",
				"Cisco Systems Inc",
				"operable"
			),
			asList(
				"equipmentFan",
				"sys/chassis-1/fan-module-1-8/fan-1",
				"NWG150306VZ",
				"N20-FAN5",
				"Cisco Systems Inc",
				"operable"
			),
			asList(
				"equipmentFan",
				"sys/chassis-1/fan-module-1-8/fan-2",
				"NWG150306VZ",
				"N20-FAN5",
				"Cisco Systems Inc",
				"operable"
			)
		);

		assertEquals(expected, XFlat.parseXml(getXml("ucsEquipementFan.xml"), properties, rootTag));
	}
}
