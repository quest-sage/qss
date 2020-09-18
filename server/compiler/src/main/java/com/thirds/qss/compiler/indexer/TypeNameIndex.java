package com.thirds.qss.compiler.indexer;

import com.thirds.qss.QualifiedName;
import com.thirds.qss.compiler.Location;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.Messenger;
import com.thirds.qss.compiler.tree.Documentable;
import com.thirds.qss.compiler.tree.Script;
import com.thirds.qss.compiler.tree.Struct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The type name index is an intermediate index used to cache the names of each type in a given package.
 * This does not cache, for example, the fields of structs or the functions of traits.
 */
public class TypeNameIndex {
    private final QualifiedName thePackage;

    public QualifiedName getPackage() {
        return thePackage;
    }

    public static class StructDefinition {
        private final Location location;

        private StructDefinition(Location location) {
            this.location = location;
        }

        public Location getLocation() {
            return location;
        }
    }

    /**
     * Maps names of structs onto the struct itself.
     */
    private final Map<String, StructDefinition> structDefinitions = new HashMap<>();

    public TypeNameIndex(QualifiedName thePackage) {
        this.thePackage = thePackage;
    }

    /**
     * Adds types to this index from the given script.
     * @param script The package of this script must match the package of the index itself.
     * @return <code>this</code> for chaining.
     */
    public Messenger<TypeNameIndex> addFrom(Script script) {
        ArrayList<Message> messages = new ArrayList<>();
        for (Documentable<Struct> struct : script.getStructs()) {
            String name = struct.getContent().getName().contents;
            if (structDefinitions.containsKey(name)) {
                messages.add(new Message(
                        struct.getContent().getName().getRange(),
                        Message.MessageSeverity.ERROR,
                        "Struct " + name + " was already defined"
                ).addInfo(new Message.MessageRelatedInformation(
                        structDefinitions.get(name).location,
                        "Previously defined here"
                )));
            }

            structDefinitions.put(name, new StructDefinition(
                    new Location(script.getFilePath(), struct.getContent().getRange())
            ));
        }
        return Messenger.success(this, messages);
    }

    public Map<String, StructDefinition> getStructDefinitions() {
        return structDefinitions;
    }
}
