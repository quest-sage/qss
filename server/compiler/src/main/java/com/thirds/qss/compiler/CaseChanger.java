package com.thirds.qss.compiler;

public class CaseChanger {
    /**
     * Converts a camel case or snake case name to UpperCamelCase.
     */
    public static String toUpperCamelCase(String in) {
        StringBuilder sb = new StringBuilder();
        boolean nextCharIsUpperCase = true;
        for (int i = 0; i < in.length(); i++) {
            if (in.charAt(i) == '_') {
                nextCharIsUpperCase = true;
                continue;
            }
            if (nextCharIsUpperCase) {
                sb.append(Character.toUpperCase(in.charAt(i)));
            } else {
                sb.append(in.charAt(i));
            }
            nextCharIsUpperCase = false;
        }
        return sb.toString();
    }

    /**
     * Checks whether the given string is in UpperCamelCase.
     */
    public static boolean isUpperCamelCase(String in) {
        return !Character.isLowerCase(in.charAt(0)) && !in.contains("_");
    }

    /**
     * Converts a camel case or snake case name to lower_snake_case.
     */
    public static String toLowerSnakeCase(String in) {
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toLowerCase(in.charAt(0)));
        for (int i = 1; i < in.length(); i++) {
            if (Character.isUpperCase(in.charAt(i)) && !Character.isUpperCase(in.charAt(i - 1))) {
                sb.append('_');
            }
            sb.append(Character.toLowerCase(in.charAt(i)));
        }
        String result = sb.toString();
        while (result.contains("__")) {
            result = result.replaceAll("__", "_");
        }
        return result;
    }

    /**
     * Checks whether the given string is in lower_snake_case.
     */
    public static boolean isLowerSnakeCase(String in) {
        for (int i = 0; i < in.length(); i++) {
            if (Character.isUpperCase(in.charAt(i)))
                return false;
        }
        return true;
    }
}
