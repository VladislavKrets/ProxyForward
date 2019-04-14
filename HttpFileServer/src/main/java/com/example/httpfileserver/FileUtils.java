package com.example.httpfileserver;

public class FileUtils {
    private static String rootDirectory;

    public static String getRootDirectory() {
        if (rootDirectory == null) rootDirectory = "/";
        return rootDirectory;
    }

    public static void setRootDirectory(String rootDirectory) {
        FileUtils.rootDirectory = rootDirectory;
    }
}
