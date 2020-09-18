package com.thirds.qss.compiler;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Represents the file path of a script (*.qss) file or a script package, relative to the bundle root.
 */
public class ScriptPath {
    private final ArrayList<String> segments = new ArrayList<>();

    public ScriptPath(String... segments) {
        this(Arrays.asList(segments));
    }

    public ScriptPath(Collection<String> segments) {
        if (segments.size() == 1) {
            // Implicitly splits a single string like "std/entity" into ["std", "entity"].
            this.segments.addAll(Arrays.asList(segments.iterator().next().split("/")));
        } else {
            this.segments.addAll(segments);
        }
    }

    /**
     * @param path A path relative to the bundle root.
     */
    public ScriptPath(Path path) {
        segments.ensureCapacity(path.getNameCount());
        for (int i = 0; i < path.getNameCount(); i++) {
            segments.add(path.getName(i).toString());
        }
    }

    /**
     * Converts the path into a Path object relative to the bundle root.
     */
    public Path toPath() {
        return Paths.get(segments.get(0), trimFirstSegment().segments.toArray(new String[0]));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            if (i != 0)
                sb.append("/");
            sb.append(segments.get(i));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScriptPath that = (ScriptPath) o;

        return segments.equals(that.segments);
    }

    @Override
    public int hashCode() {
        return segments.hashCode();
    }

    public ScriptPath prependSegment(String name) {
        ArrayList<String> segments2 = new ArrayList<>();
        segments2.add(name);
        segments2.addAll(segments);
        return new ScriptPath(segments2);
    }

    public ScriptPath appendSegment(String name) {
        ArrayList<String> segments2 = new ArrayList<>(segments);
        segments2.add(name);
        return new ScriptPath(segments2);
    }

    public ScriptPath trimFirstSegment() {
        ArrayList<String> segments2 = new ArrayList<>(segments);
        segments2.remove(0);
        return new ScriptPath(segments2);
    }

    public ScriptPath trimLastSegment() {
        ArrayList<String> segments2 = new ArrayList<>(segments);
        segments2.remove(segments2.size() - 1);
        return new ScriptPath(segments2);
    }

    public String firstSegment() {
        return segments.get(0);
    }

    public String lastSegment() {
        return segments.get(segments.size() - 1);
    }

    public ArrayList<String> getSegments() {
        return segments;
    }
}
