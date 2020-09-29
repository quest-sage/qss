package com.thirds.qss;

import com.thirds.qss.protos.ScriptProtos;
import com.thirds.qss.protos.TypeProtos;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class VariableType {
    public static VariableType.Function fromFunction(ScriptProtos.Func value) {
        // TODO serialise function purity
        return new VariableType.Function(false,
                value.getParamsList().stream().map(VariableType::from).collect(Collectors.toCollection(ArrayList::new)),
                value.getReturnType().getValueCase() == TypeProtos.Type.ValueCase.VALUE_NOT_SET ? null : VariableType.from(value.getReturnType()));
    }

    /**
     * Creates a semantically identical deep copy of this variable type.
     */
    protected abstract VariableType copy();

    public abstract TypeProtos.Type serialise();

    public static VariableType from(TypeProtos.Type type) {
        switch (type.getValueCase()) {
            case STRUCT:
                return new Struct(new QualifiedName(type.getStruct()));
            case BOOL:
                return Primitive.TYPE_BOOL;
            case INT:
                return Primitive.TYPE_INT;
            case STRING:
                return Primitive.TYPE_STRING;
            case TEXT:
                return Primitive.TYPE_TEXT;
            case ENTITY:
                return Primitive.TYPE_ENTITY;
            case RATIO:
                return Primitive.TYPE_RATIO;
            case COL:
                return Primitive.TYPE_COL;
            case POS:
                return Primitive.TYPE_POS;
            case STAT:
                return Primitive.TYPE_STAT;
            case TEXTURE:
                return Primitive.TYPE_TEXTURE;
            case PLAYER:
                return Primitive.TYPE_PLAYER;
            case FUNC:
                ArrayList<VariableType> args = new ArrayList<>();
                for (TypeProtos.Type type1 : type.getFunc().getParamsList()) {
                    args.add(from(type1));
                }
                VariableType result = Primitive.TYPE_VOID;
                if (type.getFunc().hasReturnType()) {
                    result = from(type.getFunc().getReturnType());
                }
                return new VariableType.Function(type.getFunc().getReceiverStyle(), args, result);
            case MAYBE:
                return new Maybe(from(type.getMaybe()));
            case LIST:
                return new List(from(type.getList()));
            case MAP:
                return new Map(from(type.getMap().getKeyType()), from(type.getMap().getValueType()));
            case ANYSTRUCT:
                return Primitive.TYPE_ANY_STRUCT;
            case TRAIT:
                return new Trait(new QualifiedName(type.getTrait()));
            case VALUE_NOT_SET:
            default:
                throw new UnsupportedOperationException(type.toString());
        }
    }

    private abstract static class Qualified extends VariableType {
        final QualifiedName name;

        private Qualified(QualifiedName name) {
            this.name = name;
        }

        public QualifiedName getName() {
            return name;
        }

        @Override
        public String toString() {
            return name.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Qualified qualified = (Qualified) o;
            return Objects.equals(name, qualified.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash("Qualified", name);
        }
    }

    public static class Struct extends Qualified {
        public Struct(QualifiedName name) {
            super(name);
        }

        @Override
        protected VariableType copy() {
            return new Struct(name);
        }

        @Override
        public TypeProtos.Type serialise() {
            return TypeProtos.Type.newBuilder().setStruct(name.toProtobufName()).build();
        }
    }

    /**
     * Represents an instance of a given trait.
     * TODO rename to Any
     */
    public static class Trait extends Qualified {
        public Trait(QualifiedName name) {
            super(name);
        }

        @Override
        protected VariableType copy() {
            return new Trait(name);
        }

        @Override
        public TypeProtos.Type serialise() {
            return TypeProtos.Type.newBuilder().setTrait(name.toProtobufName()).build();
        }
    }

    /**
     * The "This" variable type is silently converted into the concrete type we're implementing when used inside
     * an <code>impl X for Y</code> block. You'll only ever see this class in traits themselves, when This is not
     * defined to be any specific type.
     */
    public static class This extends VariableType {
        public This() {
        }

        @Override
        protected VariableType copy() {
            return new This();
        }

        @Override
        public TypeProtos.Type serialise() {
            throw new UnsupportedOperationException("The This data type should never be found in compiled code!");
        }

        @Override
        public String toString() {
            return "This";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof This;
        }

        @Override
        public int hashCode() {
            return "This".hashCode();
        }
    }

    public static class Primitive extends VariableType {
        public static final VariableType TYPE_BOOL = new VariableType.Primitive("Bool");
        public static final VariableType TYPE_INT = new VariableType.Primitive("Int");
        public static final VariableType TYPE_STRING = new VariableType.Primitive("String");
        public static final VariableType TYPE_TEXT = new VariableType.Primitive("Text");
        public static final VariableType TYPE_ENTITY = new VariableType.Primitive("Entity");
        public static final VariableType TYPE_RATIO = new VariableType.Primitive("Ratio");
        public static final VariableType TYPE_COL = new VariableType.Primitive("Col");
        public static final VariableType TYPE_POS = new VariableType.Primitive("Pos");
        public static final VariableType TYPE_STAT = new VariableType.Primitive("Stat");
        public static final VariableType TYPE_TEXTURE = new VariableType.Primitive("Texture");
        public static final VariableType TYPE_PLAYER = new VariableType.Primitive("Player");
        public static final VariableType TYPE_ANY_STRUCT = new VariableType.Primitive("AnyStruct");
        public static final VariableType TYPE_UNKNOWN = new VariableType.Primitive("<unknown>");
        public static final VariableType TYPE_VOID = new VariableType.Primitive("<nothing>");

        private final String name;

        private Primitive(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        protected VariableType copy() {
            return new Primitive(name);
        }

        @Override
        public TypeProtos.Type serialise() {
            TypeProtos.Type.Builder b = TypeProtos.Type.newBuilder();
            switch (name) {
                case "Bool": return b.setBool(true).build();
                case "Int": return b.setInt(true).build();
                case "String": return b.setString(true).build();
                case "Text": return b.setText(true).build();
                case "Entity": return b.setEntity(true).build();
                case "Ratio": return b.setRatio(true).build();
                case "Col": return b.setCol(true).build();
                case "Pos": return b.setPos(true).build();
                case "Stat": return b.setStat(true).build();
                case "Texture": return b.setTexture(true).build();
                case "Player": return b.setPlayer(true).build();
            }
            throw new UnsupportedOperationException(this.toString());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Primitive primitive = (Primitive) o;
            return Objects.equals(name, primitive.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash("Primitive", name);
        }
    }

    public static class Maybe extends VariableType {
        private final VariableType contentsType;

        public Maybe(VariableType contentsType) {
            this.contentsType = contentsType;
        }

        public VariableType getContentsType() {
            return contentsType;
        }

        @Override
        public String toString() {
            return contentsType + "?";
        }

        @Override
        protected VariableType copy() {
            return new Maybe(contentsType.copy());
        }

        @Override
        public TypeProtos.Type serialise() {
            return TypeProtos.Type.newBuilder().setMaybe(contentsType.serialise()).build();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Maybe maybe = (Maybe) o;
            return Objects.equals(contentsType, maybe.contentsType);
        }

        @Override
        public int hashCode() {
            return Objects.hash("Maybe", contentsType);
        }
    }

    public static class List extends VariableType {
        private final VariableType elementType;

        public List(VariableType elementType) {
            this.elementType = elementType;
        }

        public VariableType getElementType() {
            return elementType;
        }

        @Override
        public String toString() {
            return "[" + elementType + "]";
        }

        @Override
        protected VariableType copy() {
            return new List(elementType.copy());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            List list = (List) o;
            return elementType.equals(list.elementType);
        }

        @Override
        public int hashCode() {
            return Objects.hash("List", elementType);
        }

        @Override
        public TypeProtos.Type serialise() {
            return TypeProtos.Type.newBuilder().setList(elementType.serialise()).build();
        }
    }

    public static class Map extends VariableType {
        private final VariableType keyType, valueType;

        public Map(VariableType keyType, VariableType valueType) {
            this.keyType = keyType;
            this.valueType = valueType;
        }

        public VariableType getKeyType() {
            return keyType;
        }

        public VariableType getValueType() {
            return valueType;
        }

        @Override
        public String toString() {
            return "{" + keyType + " => " + valueType + "}";
        }

        @Override
        protected VariableType copy() {
            return new Map(keyType.copy(), valueType.copy());
        }

        @Override
        public TypeProtos.Type serialise() {
            return TypeProtos.Type.newBuilder().setMap(TypeProtos.Type.Map.newBuilder()
                    .setKeyType(keyType.serialise())
                    .setValueType(valueType.serialise())).build();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Map map = (Map) o;
            return keyType.equals(map.keyType) &&
                    valueType.equals(map.valueType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(keyType, valueType);
        }
    }

    public static class Function extends VariableType {
        /**
         * If true, this function can only be called using the syntax <code>receiver.function(arguments)</code>.
         * If false, this function can only be called using the syntax <code>function(arguments)</code>.
         * This field is not taken into account when determining whether functions have equal types.
         */
        private final boolean receiverStyle;
        private final ArrayList<VariableType> params;
        private final VariableType returnType;

        /**
         * If not null, this is the name of the trait that contains this function.
         */
        private QualifiedName containerTrait;
        private Purity purity = Purity.IMPURE;
        private boolean isNative = false;

        @Override
        protected VariableType copy() {
            Function function = new Function(receiverStyle, params.stream().map(VariableType::copy).collect(Collectors.toCollection(ArrayList::new)), returnType.copy());
            function.purity = purity;
            function.containerTrait = containerTrait;
            return function;
        }

        @Override
        public TypeProtos.Type serialise() {
            throw new UnsupportedOperationException("haven't done this yet");
        }

        public void setContainerTrait(QualifiedName containerTrait) {
            this.containerTrait = containerTrait;
        }

        public Optional<QualifiedName> getContainerTrait() {
            return Optional.ofNullable(containerTrait);
        }

        public boolean isTraitFunction() {
            return containerTrait != null;
        }

        public boolean isNative() {
            return isNative;
        }

        /**
         * @param returnType Null or Primitive.TYPE_VOID (both are converted into VOID) if the function does not return a value.
         */
        public Function(boolean receiverStyle, ArrayList<VariableType> params, VariableType returnType) {
            this.receiverStyle = receiverStyle;
            this.params = params;
            if (returnType == null)
                returnType = Primitive.TYPE_VOID;
            this.returnType = returnType;
        }

        public boolean isReceiverStyle() {
            return receiverStyle;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            switch (purity) {
                case EAGER:
                    sb.append("EagerFunc");
                    break;
                case UI:
                    sb.append("UiFunc");
                    break;
                case PURE:
                    sb.append("PureFunc");
                    break;
                case IMPURE:
                    sb.append("Func");
                    break;
            }

            if (params.size() == 0) {
                sb.append("()");
            } else if (params.size() == 1) {
                sb.append("(").append(params.get(0)).append(")");
            } else {
                sb.append("(");
                for (int i = 0; i < params.size(); i++) {
                    if (i != 0) sb.append(", ");
                    sb.append(params.get(i));
                }
                sb.append(")");
            }

            if (returnType != Primitive.TYPE_VOID) {
                sb.append(" -> ").append(returnType);
            }

            return sb.toString();
        }

        public ArrayList<VariableType> getParams() {
            return params;
        }

        public VariableType getReturnType() {
            return returnType;
        }

        public void setNative(boolean aNative) {
            isNative = aNative;
        }

        public Purity getPurity() {
            return purity;
        }

        public void setPurity(Purity purity) {
            this.purity = purity;
        }

        public enum Purity {
            EAGER,
            UI,
            PURE,
            IMPURE;

            public boolean canExecuteInside(Purity containingFunc) {
                switch (this) {
                    case EAGER:
                        switch (containingFunc) {
                            case EAGER:
                            case IMPURE:
                                return true;
                            case PURE:
                            case UI:
                                return false;
                        }
                    case UI:
                        switch (containingFunc) {
                            case UI:
                                return true;
                            case PURE:
                            case EAGER:
                            case IMPURE:
                                return false;
                        }
                    case PURE:
                        return true;
                    case IMPURE:
                        switch (containingFunc) {
                            case IMPURE:
                                return true;
                            case UI:
                            case PURE:
                            case EAGER:
                                return false;
                        }
                }
                throw new UnsupportedOperationException("Purity mismatch: " + this + " " + containingFunc);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Function function = (Function) o;
            return Objects.equals(params, function.params) &&
                    Objects.equals(returnType, function.returnType) &&
                    purity == function.purity;
        }

        @Override
        public int hashCode() {
            return Objects.hash("Func", params, returnType, purity);
        }
    }
}
