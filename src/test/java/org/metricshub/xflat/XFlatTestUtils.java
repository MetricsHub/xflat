package org.metricshub.xflat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class XFlatTestUtils {

	public static final String PROPERTIES =
		" \t \n \r OS   \t  \r >  \r  \t    \n    name     \t \n   ; \r \t \n " +
		"Owner \t \n ;  \t \n " +
		"  \t Disks  \t  \n  /  \t  Disk   \t    /   \n \t Volumes  \t  / \n    \t   Volume       >        name   \r\t\n   ;   \t \n \r  " +
		"Disks/Disk/Volumes/Volume/Subscribe;  " +
		"Disks/Disk>name    ;" +
		"Disks/Disk>size;" +
		"Disks/Disk/Free \t\r\n ";

	public static final String ROOT_TAG = " \t \r \n /  \t \t \r \n  Document \t\r\n ";

	public String getFileAbsolutePath(final String name) throws Exception {
		return getFilePath(name).toAbsolutePath().toString();
	}

	public String getXml(final String name) throws Exception {
		try (final Stream<String> lines = Files.lines(getFilePath(name))) {
			return lines.collect(Collectors.joining());
		}
	}

	private Path getFilePath(final String name) throws Exception {
		return Paths.get(this.getClass().getClassLoader().getResource(name).toURI());
	}
}
