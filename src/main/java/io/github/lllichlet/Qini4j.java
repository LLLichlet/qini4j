package io.github.lllichlet;

import org.ini4j.Ini;

import java.io.*;

public class Qini4j {
    private Qini4j() {}

    public static Ini load(Reader reader) throws IOException {
        if (reader == null) {
            throw new NullPointerException("reader is null");
        }
        Ini ini = new Ini();
        BufferedReader br = new BufferedReader(reader);
        String line;
        String currentSection = null;
        int lineNumber = 0;

        while ((line = br.readLine()) != null) {
            lineNumber++;
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = line.substring(1, line.length() - 1).trim();
                continue;
            }
            if (currentSection == null) {
                throw new IOException("Line " + lineNumber + ": key/value outside of a section");
            }
            int sepIdx = findFirstSeparator(line);
            if (sepIdx < 0) continue;
            String key = line.substring(0, sepIdx).trim();
            String valuePart = line.substring(sepIdx + 1).trim();
            String actualValue = parseQiniValue(valuePart);
            ini.put(currentSection, key, actualValue);
        }
        return ini;
    }

    public static void store(Ini ini, Writer writer) throws IOException {
        if (ini == null) {
            throw new NullPointerException("ini is null");
        }
        if (writer == null) {
            throw new NullPointerException("writer is null");
        }
        BufferedWriter bw = new BufferedWriter(writer);
        for (String sectionName : ini.keySet()) {
            bw.write("[" + sectionName + "]");
            bw.newLine();
            Ini.Section section = ini.get(sectionName);
            for (String key : section.keySet()) {
                String value = section.get(key);
                if (value == null) {
                    value = "";
                }
                String escaped = escapeValue(value);
                String outputValue = needQuotes(value) ? "\"" + escaped + "\"" : value;
                bw.write(key + " = " + outputValue);
                bw.newLine();
            }
            bw.newLine();
        }
        bw.flush();
    }

    private static String parseQiniValue(String raw) {
        if (raw.startsWith("\"")) {
            int end = findClosingQuote(raw);
            if (end > 0) {
                return unescape(raw.substring(1, end));
            }
        }
        if (raw.startsWith("'")) {
            int end = raw.indexOf('\'', 1);
            if (end > 0) {
                return raw.substring(1, end);
            }
        }
        int commentIdx = raw.indexOf('#');
        if (commentIdx == -1) commentIdx = raw.indexOf(';');
        if (commentIdx >= 0) {
            raw = raw.substring(0, commentIdx).trim();
        }
        return raw;
    }

    private static int findClosingQuote(String s) {
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) == '\\') {
                i++; // skip escaped character
            } else if (s.charAt(i) == '"') {
                return i;
            }
        }
        return -1;
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '\"': sb.append('\"'); i++; break;
                    case '\\': sb.append('\\'); i++; break;
                    case 'n': sb.append('\n'); i++; break;
                    case 't': sb.append('\t'); i++; break;
                    case 'r': sb.append('\r'); i++; break;
                    default: sb.append(c); break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String escapeValue(String value) {
        if (value == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }

    private static int findFirstSeparator(String line) {
        int eq = line.indexOf('=');
        int colon = line.indexOf(':');
        if (eq == -1) return colon;
        if (colon == -1) return eq;
        return Math.min(eq, colon);
    }

    private static boolean needQuotes(String value) {
        if (value == null) return false;
        return value.contains("=") || value.contains(":") || value.contains("#") ||
                value.contains(";") || value.contains("\"") || value.contains("'") ||
                value.startsWith(" ") || value.endsWith(" ") ||
                value.contains("\n") || value.contains("\r") || value.contains("\t") ||
                value.trim().isEmpty();
    }
}
