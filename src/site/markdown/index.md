# XFlat Utility

The XFlat Utility enables to parse XML files with argument properties into a list of values list.

# How to run the XFlat Utility inside Java

Add XFlat in the list of dependencies in your [Maven **pom.xml**](https://maven.apache.org/pom.html):

```xml
<dependencies>
	<!-- [...] -->
	<dependency>
		<groupId>${project.groupId}</groupId>
		<artifactId>${project.artifactId}</artifactId>
		<version>${project.version}</version>
	</dependency>
</dependencies>
```

Use it as follows:
```Java
import java.util.List;

import org.sentrysoftware.xflat.exceptions.XFlatException;

public class Main {

	public static void main(String[] args) throws XFlatException {
		
		/*
		 * <?xml version="1.0"?>
			<Document>
				<Owner>User</Owner>
				<Disks>
					<Disk name="Disk1" size="1000">
						<Free>500</Free>
						<Volumes>
							<Volume name="Vol1">
								<Subscribe>600</Subscribe>
							</Volume>
						</Volumes>
					</Disk>
					<Disk name="Disk2" size="2000">
						<Free>750</Free>
					</Disk>
					<Disk name="Disk3" size="2900">
						<Free>1500</Free>
						<Volumes>
							<Volume name="Vol3.0">
								<Subscribe>3000</Subscribe>
							</Volume>
							<Volume name="Vol3.1">
								<Subscribe>3100</Subscribe>
							</Volume>
							<Volume name="Vol3.2">
								<Subscribe>3200</Subscribe>
							</Volume>
						</Volumes>
					</Disk>
				</Disks>
				<OS name="Linux"/>
			</Document>
		 */
		final String xml = "<?xml version=\"1.0\"?><Document><Owner>User</Owner><Disks><Disk name=\"Disk1\" size=\"1000\"><Free>500</Free><Volumes><Volume name=\"Vol1\"><Subscribe>600</Subscribe></Volume></Volumes></Disk><Disk name=\"Disk2\" size=\"2000\"><Free>750</Free></Disk><Disk name=\"Disk3\" size=\"2900\"><Free>1500</Free><Volumes><Volume name=\"Vol3.0\"><Subscribe>3000</Subscribe></Volume><Volume name=\"Vol3.1\"><Subscribe>3100</Subscribe></Volume><Volume name=\"Vol3.2\"><Subscribe>3200</Subscribe></Volume></Volumes></Disk></Disks><OS name=\"Linux\"/></Document>";
		final String rootTag = "Document";
		final String properties = "Disks/Disk>size; Disks/Disk/Free";

		// Parsing the XML file into a list of values list
		final List<List<String>> flatXml = XFlat.parseXml(xml, properties, rootTag);

		// Print each line
		flatXml.forEach(System.out::println);	
	}
}
```