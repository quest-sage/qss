package com.thirds.qss.compiler.indexer;

import com.thirds.qss.QualifiedName;
import com.thirds.qss.compiler.Location;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.Messenger;
import com.thirds.qss.compiler.tree.Documentable;
import com.thirds.qss.compiler.tree.Script;
import com.thirds.qss.compiler.tree.script.Func;
import com.thirds.qss.compiler.tree.script.Struct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The name index is an intermediate index used to cache the names of each item in a given package.
 * This does not cache, for example, the fields of structs or the functions of traits.
 */
public class NameIndex {
    private final String bundleName;
    private final QualifiedName thePackage;

    public QualifiedName getPackage() {
        return thePackage;
    }

    public static class StructDefinition {
        private final String documentation;
        private final Location location;

        private StructDefinition(String documentation, Location location) {
            this.documentation = documentation;
            this.location = location;
        }

        public String getDocumentation() {
            return documentation;
        }

        public Location getLocation() {
            return location;
        }
    }

    public static class FuncDefinition {
        private final String documentation;
        private final Location location;

        private FuncDefinition(String documentation, Location location) {
            this.documentation = documentation;
            this.location = location;
        }

        public String getDocumentation() {
            return documentation;
        }

        public Location getLocation() {
            return location;
        }
    }

    /**
     * Maps names of structs onto the struct itself.
     */
    private final Map<String, StructDefinition> structDefinitions = new HashMap<>();

    /**
     * Maps names of funcs onto the func itself.
     */
    private final Map<String, FuncDefinition> funcDefinitions = new HashMap<>();

    public NameIndex(String bundleName, QualifiedName thePackage) {
        this.bundleName = bundleName;
        this.thePackage = thePackage;
    }

    /**
     * Adds types to this index from the given script.
     * @param script The package of this script must match the package of the index itself.
     * @return <code>this</code> for chaining.
     */
    public Messenger<NameIndex> addFrom(Script script) {
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
                    struct.getDocumentation().map(tk -> tk.contents).orElse(null),
                    new Location(script.getFilePath(), struct.getContent().getRange())
            ));
        }
        for (Documentable<Func> func : script.getFuncs()) {
            String name = func.getContent().getName().contents;
            if (funcDefinitions.containsKey(name)) {
                messages.add(new Message(
                        func.getContent().getName().getRange(),
                        Message.MessageSeverity.ERROR,
                        "Func " + name + " was already defined"
                ).addInfo(new Message.MessageRelatedInformation(
                        structDefinitions.get(name).location,
                        "Previously defined here"
                )));
            }

            funcDefinitions.put(name, new FuncDefinition(
                    func.getDocumentation().map(tk -> tk.contents).orElse(null),
                    new Location(script.getFilePath(), func.getContent().getRange())
            ));
        }
        return Messenger.success(this, messages);
    }

    public Map<String, StructDefinition> getStructDefinitions() {
        return structDefinitions;
    }

    public Map<String, FuncDefinition> getFuncDefinitions() {
        return funcDefinitions;
    }

    @Override
    public String toString() {
        return "NameIndex{" +
                "bundleName='" + bundleName + '\'' +
                ", thePackage=" + thePackage +
                ", structDefinitions=" + structDefinitions +
                ", funcDefinitions=" + funcDefinitions +
                '}';
    }
}
