package com.thirds.qss.compiler.indexer;

import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.Location;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.Messenger;
import com.thirds.qss.compiler.tree.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The type name index is an index used to store the names and fields of each type in a given package.
 */
public class TypeIndex {
    private final Map<String, StructDefinition> structDefinitions = new HashMap<>();
    private final Compiler compiler;

    /**
     * The type name index is used for determining whether a name is defined.
     */
    public TypeIndex(Compiler compiler) {
        this.compiler = compiler;
    }

    private static class FieldDefinition {
        private final Location location;
        private final VariableType variableType;

        /**
         * @param variableType May be null; if so, the type in the type index will show as <code>&lt;unknown&gt;</code>.
         */
        private FieldDefinition(Location location, VariableType variableType) {
            this.location = location;
            this.variableType = variableType;
        }

        public Location getLocation() {
            return location;
        }

        public VariableType getVariableType() {
            return variableType;
        }
    }

    private static class StructDefinition {
        private final Location location;
        private final Map<String, FieldDefinition> fields = new HashMap<>();

        private StructDefinition(Location location) {
            this.location = location;
        }

        public Location getLocation() {
            return location;
        }

        public Map<String, FieldDefinition> getFields() {
            return fields;
        }
    }

    /**
     * Adds types to this index from the given script.
     * @param script The package of this script must match the package of the index itself.
     * @return <code>this</code> for chaining.
     */
    public Messenger<TypeIndex> addFrom(Script script) {
        ArrayList<Message> messages = new ArrayList<>();
        for (Documentable<Struct> struct : script.getStructs()) {
            StructDefinition def = new StructDefinition(
                    new Location(script.getFilePath(), struct.getContent().getRange())
            );

            for (Field field : struct.getContent().getFields()) {
                if (def.fields.containsKey(field.getName().contents)) {
                    messages.add(new Message(
                            field.getName().getRange(),
                            Message.MessageSeverity.ERROR,
                            "Field " + field.getName().contents + " was already defined"
                    ).addInfo(new Message.MessageRelatedInformation(
                            def.fields.get(field.getName().contents).location,
                            "Previously defined here"
                    )));
                } else {
                    // TODO fill the import set
                    Type.ResolveResult fieldTypeAlternatives = field.getType().resolve(Set.of(
                            script.getPackageName()
                    ), compiler.getTypeNameIndices());

                    if (fieldTypeAlternatives.alternatives.isEmpty()) {
                        StringBuilder message = new StringBuilder("Could not resolve type of ").append(field.getName().contents);
                        if (!fieldTypeAlternatives.nonImportedAlternatives.isEmpty()) {
                            message.append("; try one of the following:");
                            for (Type.ResolveAlternative alt : fieldTypeAlternatives.nonImportedAlternatives) {
                                // \u2022 is the bullet character
                                message.append("\n").append("\u2022 import ").append(alt.imports.stream().map(i -> i.name.toString()).collect(Collectors.joining(", ")));
                            }
                        }
                        messages.add(new Message(
                                field.getType().getRange(),
                                Message.MessageSeverity.ERROR,
                                message.toString()
                        ));
                    } else if (fieldTypeAlternatives.alternatives.size() > 1) {
                        messages.add(new Message(
                                field.getType().getRange(),
                                Message.MessageSeverity.ERROR,
                                "Type of " + field.getName().contents + " was ambiguous, possibilities were: " +
                                        fieldTypeAlternatives.alternatives.stream().map(alt -> alt.type.toString()).collect(Collectors.joining(", "))
                        ));
                    }

                    def.fields.put(field.getName().contents, new FieldDefinition(
                            new Location(script.getFilePath(), field.getRange()),
                            fieldTypeAlternatives.alternatives.size() == 1 ? fieldTypeAlternatives.alternatives.get(0).type : null
                    ));
                }
            }

            structDefinitions.put(struct.getContent().getName().contents, def);
        }
        return Messenger.success(this, messages);
    }
}
