package com.thirds.qss;

import com.thirds.qss.protos.NameProtos;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class QualifiedName {
    private final ArrayList<String> segments = new ArrayList<>();

    public QualifiedName(NameProtos.QualifiedName qn) {
        this(qn.getSegmentsList());
    }

    public QualifiedName(String... segments) {
        this(Arrays.asList(segments));
    }

    public QualifiedName(Collection<String> segments) {
        if (segments.size() == 1) {
            // Implicitly splits a single string like "std::entity" into ["std", "entity"].
            this.segments.addAll(Arrays.asList(segments.iterator().next().split("::")));
        } else {
            this.segments.addAll(segments);
        }
    }

    public NameProtos.QualifiedName toProtobufName() {
        return NameProtos.QualifiedName.newBuilder().addAllSegments(segments).build();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            if (i != 0)
                sb.append("::");
            sb.append(segments.get(i));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QualifiedName that = (QualifiedName) o;

        return segments.equals(that.segments);
    }

    @Override
    public int hashCode() {
        return segments.hashCode();
    }

    public QualifiedName prependSegment(String name) {
        ArrayList<String> segments2 = new ArrayList<>();
        segments2.add(name);
        segments2.addAll(segments);
        return new QualifiedName(segments2);
    }

    public QualifiedName appendSegment(String name) {
        ArrayList<String> segments2 = new ArrayList<>(segments);
        segments2.add(name);
        return new QualifiedName(segments2);
    }

    public QualifiedName trimFirstSegment() {
        ArrayList<String> segments2 = new ArrayList<>(segments);
        segments2.remove(0);
        return new QualifiedName(segments2);
    }

    public QualifiedName trimLastSegment() {
        ArrayList<String> segments2 = new ArrayList<>(segments);
        segments2.remove(segments2.size() - 1);
        return new QualifiedName(segments2);
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

    public Path toPath() {
        if (segments.isEmpty())
            return Paths.get(".");
        return Paths.get(segments.get(0), trimFirstSegment().segments.toArray(new String[0]));
    }
}
