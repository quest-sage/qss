package com.thirds.qss.compiler.indexer;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.thirds.qss.QualifiedName;
import com.thirds.qss.compiler.Location;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.Messenger;
import com.thirds.qss.compiler.tree.Script;
import com.thirds.qss.compiler.tree.Struct;

import java.util.ArrayList;

/**
 * The type name index is an intermediate index used to cache the names of each type in a given package.
 * This does not cache, for example, the fields of structs or the functions of traits.
 */
public class TypeNameIndex {
    private static class StructDefinition {
        private final Location location;

        private StructDefinition(Location location) {
            this.location = location;
        }
    }

    private final QualifiedName thePackage;
    /**
     * Maps names of structs onto the struct itself.
     */
    private final Multimap<String, StructDefinition> structDefinitions = MultimapBuilder.hashKeys().arrayListValues().build();

    public TypeNameIndex(QualifiedName thePackage) {
        this.thePackage = thePackage;
    }

    /**
     * Adds types to this index from the given script.
     * @param script The package of this script must match the package of the index itself.
     */
    public Messenger<Object> addFrom(Script script) {
        ArrayList<Message> messages = new ArrayList<>();
        for (Struct struct : script.getStructs()) {
            String name = struct.getName().contents;
            if (structDefinitions.containsKey(name)) {
                Message message = new Message(
                        struct.getName().range,
                        Message.MessageSeverity.ERROR,
                        "Struct " + name + " was already defined"
                );
                for (StructDefinition definition : structDefinitions.get(name)) {
                    message.addInfo(new Message.MessageRelatedInformation(
                            definition.location,
                            "Previously defined here"
                    ));
                }
                messages.add(message);
            }

            structDefinitions.put(name, new StructDefinition(
                    new Location(script.getFilePath(), struct.getRange())
            ));
        }
        return Messenger.success(new Object(), messages);
    }
}
