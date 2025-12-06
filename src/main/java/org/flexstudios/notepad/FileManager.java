package org.flexstudios.notepad;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileManager {

    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    public static String readFile(File file) throws IOException {
        // Try to detect encoding
        String content = readFileWithEncoding(file, UTF8);
        if (content != null) {
            return content;
        }

        // Fallback to Windows-1252
        content = readFileWithEncoding(file, WINDOWS_1252);
        if (content != null) {
            return content;
        }

        throw new IOException("Unable to read file with supported encodings");
    }

    private static String readFileWithEncoding(File file, Charset encoding) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding))) {
            StringBuilder content = new StringBuilder();
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (!firstLine) {
                    content.append(System.lineSeparator());
                }
                content.append(line);
                firstLine = false;
            }

            return content.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static void writeFile(File file, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), UTF8))) {
            writer.write(content);
        }
    }

    public static boolean fileExists(File file) {
        return file != null && file.exists() && file.isFile();
    }

    public static String getFileExtension(File file) {
        if (file == null) {
            return "";
        }
        String name = file.getName();
        int lastDotIndex = name.lastIndexOf('.');
        return lastDotIndex > 0 ? name.substring(lastDotIndex + 1) : "";
    }

    public static String getFileNameWithoutExtension(File file) {
        if (file == null) {
            return "";
        }
        String name = file.getName();
        int lastDotIndex = name.lastIndexOf('.');
        return lastDotIndex > 0 ? name.substring(0, lastDotIndex) : name;
    }

    public static File getLastUsedDirectory() {
        String userHome = System.getProperty("user.home");
        File documentsDir = new File(userHome, "Documents");
        return documentsDir.exists() ? documentsDir : new File(userHome);
    }
}