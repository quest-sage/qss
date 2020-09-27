package com.thirds.qss.compiler.type;

import com.thirds.qss.BundleQualifiedName;
import com.thirds.qss.VariableType;
import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.Range;
import com.thirds.qss.compiler.resolve.ResolveAlternative;
import com.thirds.qss.compiler.resolve.ResolveResult;
import com.thirds.qss.compiler.tree.Script;
import com.thirds.qss.compiler.tree.Type;

import java.util.ArrayList;
import java.util.List;

public class FuncType extends Type {
    private final ArrayList<Type> params;
    private final Type returnType;

    /**
     * @param returnType Nullable.
     */
    public FuncType(Range totalRange, ArrayList<Type> params, Type returnType) {
        super(totalRange);
        this.params = params;
        this.returnType = returnType;
    }

    @Override
    protected ResolveResult<VariableType> resolveImpl(Compiler compiler, Script script) {
        ArrayList<BundleQualifiedName> imports = new ArrayList<>();
        ArrayList<VariableType> paramTypes = new ArrayList<>();

        for (Type param : params) {
            ResolveResult<VariableType> resolvedType = param.resolve(compiler, script);
            if (resolvedType.alternatives.size() == 1) {
                paramTypes.add(resolvedType.alternatives.get(0).value);
                imports.addAll(resolvedType.alternatives.get(0).imports);
            } else {
                return ResolveResult.nonImported(List.of());
            }
        }

        if (returnType != null) {
            ResolveResult<VariableType> returnTypeResolved = returnType.resolve(compiler, script);
            if (returnTypeResolved.alternatives.size() == 1) {
                VariableType returnTypeVariableType = returnTypeResolved.alternatives.get(0).value;
                imports.addAll(returnTypeResolved.alternatives.get(0).imports);
                return ResolveResult.success(List.of(
                        new ResolveAlternative<>(
                                new VariableType.Function(false, paramTypes, returnTypeVariableType),
                                imports
                        )
                ));
            } else {
                return ResolveResult.nonImported(List.of());
            }
        } else {
            return ResolveResult.success(List.of(
                    new ResolveAlternative<>(
                            new VariableType.Function(false, paramTypes, null),
                            imports
                    )
            ));
        }
    }
}
