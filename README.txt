qini4j

An extension for ini4j with quote-parsing support, providing more flexible
string handling for INI files.

Features:
- Double-quoted values with escape sequences (\", \\, \n, \t, \r)
- Single-quoted values
- Safe inline comment handling (# and ;)
- Round-trip load and store

Maven:
    <dependency>
        <groupId>io.github.lllichlet</groupId>
        <artifactId>qini4j</artifactId>
        <version>1.0</version>
    </dependency>

Usage:
    Ini ini = Qini4j.load(new StringReader("[section]\nkey = \"hello world\"\n"));
    String value = ini.get("section", "key"); // hello world

License: Apache License 2.0 (see LICENSE file)
