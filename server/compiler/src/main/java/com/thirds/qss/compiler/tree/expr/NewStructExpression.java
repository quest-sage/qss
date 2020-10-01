package com.thirds.qss.compiler.tree.expr;

import com.thirds.qss.QualifiedName;
import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.indexer.Index;
import com.thirds.qss.compiler.resolve.ResolveResult;
import com.thirds.qss.compiler.resolve.Resolver;
import com.thirds.qss.compiler.tree.NameLiteral;
import com.thirds.qss.compiler.tree.Node;
import com.thirds.qss.compiler.tree.Type;
import com.thirds.qss.compiler.type.ExpressionTypeDeducer;
import com.thirds.qss.compiler.type.VariableTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Creates a new struct containing the supplied values.
 */
public class NewStructExpression extends Expression {
    private final Type type;
    private final ArrayList<StructField> values;

    public NewStructExpression(Range totalRange, Type type, ArrayList<StructField> values) {
        super(totalRange);
        this.type = type;
        this.values = values;
    }

    @Override
    protected VariableType deduceVariableType(ExpressionTypeDeducer expressionTypeDeducer, VariableTracker.ScopeTree scopeTree) {
        Resolver.resolveType(expressionTypeDeducer.getCompiler(), expressionTypeDeducer.getScript(), expressionTypeDeducer.getMessages(), "new struct", type);
        if (!(type.getResolvedType() instanceof VariableType.Struct)) {
            throw new UnsupportedOperationException(type.getResolvedType().toString());
        }

        // Ensure that all provided fields are valid struct fields of correct type.
        QualifiedName structName = ((VariableType.Struct) type.getResolvedType()).getName();
        for (StructField field : values) {
            ResolveResult<Resolver.StructFieldAlternative> fieldResolved = Resolver.resolveStructField(
                    expressionTypeDeducer.getCompiler(),
                    expressionTypeDeducer.getScript(),
                    expressionTypeDeducer.getMessages(),
                    structName,
                    field.getKey()
            );
            if (fieldResolved.alternatives.size() == 1) {
                VariableType fieldType = fieldResolved.alternatives.get(0).value.type;
                field.getValue().deduceAndAssignVariableType(expressionTypeDeducer, scopeTree);
                if (fieldType == VariableType.Primitive.TYPE_UNKNOWN) {
                    expressionTypeDeducer.getMessages().add(new Message(
                            field.getKey().getRange(),
                            Message.MessageSeverity.ERROR,
                            "Field " + field.getKey().toQualifiedName() + " had an unknown type; this is usually caused by not including a required dependency"
                    ));
                } else {
                    field.getValue().getVariableType().ifPresent(type -> expressionTypeDeducer.getCastChecker().attemptDowncast(
                            field.getValue().getRange(),
                            type, fieldType
                    ));
                }
            }
        }

        // Ensure that all fields of the struct are assigned.
        // To do this, we need to find the original definition of the struct.
        NameLiteral structNameLiteral = ((Type.StructType) type).getStructName();
        ResolveResult<Map<String, Index.FieldDefinition>> structFields = Resolver.resolveGlobalScope(expressionTypeDeducer.getCompiler(), expressionTypeDeducer.getScript(), index -> {
            if (!index.getPackage().equals(structName.trimLastSegment()))
                return List.of();
            Index.StructDefinition structDefinition = index.getStructDefinitions().get(structName.lastSegment());
            if (structDefinition == null)
                return List.of();
            return List.of(structDefinition.getFields());
        });
        if (structFields.alternatives.size() != 1) {
            expressionTypeDeducer.getMessages().add(new Message(
                    structNameLiteral.getRange(),
                    Message.MessageSeverity.ERROR,
                    "Could not re-resolve struct with detailed index (this is a compiler bug): " + structFields
            ));
        } else {
            Map<String, Index.FieldDefinition> fields = structFields.alternatives.get(0).value;
            for (String s : fields.keySet()) {
                if (fields.get(s).getVariableType() == VariableType.Primitive.TYPE_UNKNOWN) {
                    expressionTypeDeducer.getMessages().add(new Message(
                            structNameLiteral.getRange(),
                            Message.MessageSeverity.ERROR,
                            "Cannot create a new " + structName + " because field " + s + " had an unknown type; this is usually caused by not including a required dependency"
                    ));
                }

                QualifiedName expectedFieldName = structName.trimLastSegment().appendSegment(s);
                // Check that this field was assigned.
                boolean assigned = false;
                for (StructField value : values) {
                    if (value.getKey().getTargetQualifiedName() != null && value.getKey().getTargetQualifiedName().equals(expectedFieldName)) {
                        assigned = true;
                        break;
                    }
                }
                if (!assigned) {
                    expressionTypeDeducer.getMessages().add(new Message(
                            structNameLiteral.getRange(),
                            Message.MessageSeverity.ERROR,
                            "Field " + s + " must be assigned when creating a new " + structName
                    ));
                }
            }
        }

        return type.getResolvedType();
    }

    public ArrayList<StructField> getValues() {
        return values;
    }

    @Override
    public void forChildren(Consumer<Node> consumer) {
        consumer.accept(type);
        for (StructField value : values) {
            consumer.accept(value.getKey());
            consumer.accept(value.getValue());
        }
    }
}
