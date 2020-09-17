package com.thirds.qss.langserver;

import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.Message;
import com.thirds.qss.compiler.Messenger;
import com.thirds.qss.compiler.tree.Script;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class QssTextDocumentService implements TextDocumentService {
    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams completionParams) {
        // Provide completion item.
        return CompletableFuture.supplyAsync(() -> {
            List<CompletionItem> completionItems = new ArrayList<>();
            try {
                // Sample Completion item for sayHello
                CompletionItem completionItem = new CompletionItem();
                // Define the text to be inserted in to the file if the completion item is selected.
                completionItem.setInsertText("sayHello() {\n    print(\"hello\")\n}");
                // Set the label that shows when the completion drop down appears in the Editor.
                completionItem.setLabel("sayHello()");
                // Set the completion kind. This is a snippet.
                // That means it replace character which trigger the completion and
                // replace it with what defined in inserted text.
                completionItem.setKind(CompletionItemKind.Snippet);
                // This will set the details for the snippet code which will help user to
                // understand what this completion item is.
                completionItem.setDetail("sayHello()\n this will say hello to the people");

                // Add the sample completion item to the list.
                completionItems.add(completionItem);
            } catch (Exception e) {
                //TODO: Handle the exception.
            }

            // Return the list of completion items.
            return Either.forLeft(completionItems);
        });
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem completionItem) {
        return null;
    }

    @Override
    public CompletableFuture<Hover> hover(TextDocumentPositionParams textDocumentPositionParams) {
        return null;
    }

    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(TextDocumentPositionParams textDocumentPositionParams) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends Location>> definition(TextDocumentPositionParams textDocumentPositionParams) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams referenceParams) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(TextDocumentPositionParams textDocumentPositionParams) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends SymbolInformation>> documentSymbol(DocumentSymbolParams documentSymbolParams) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends Command>> codeAction(CodeActionParams codeActionParams) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams codeLensParams) {
        return null;
    }

    @Override
    public CompletableFuture<CodeLens> resolveCodeLens(CodeLens codeLens) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams documentFormattingParams) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams documentRangeFormattingParams) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams documentOnTypeFormattingParams) {
        return null;
    }

    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams renameParams) {
        return null;
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams didOpenTextDocumentParams) {

    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        URI uri = QssLanguageServer.relativize(params.getTextDocument().getUri());
        //QssLogger.logger.atInfo().log("File %s changed", uri);

        for (TextDocumentContentChangeEvent change : params.getContentChanges()) {
            if (change.getRange() != null || change.getRangeLength() != null) {
                QssLogger.logger.atSevere().log("Incremental file change not supported: %s", params);
            }

            QssLogger.logger.atInfo().log("Compiling %s", uri);
            Compiler compiler = new Compiler(QssLanguageServer.getRootDir());
            Messenger<Script> result = compiler.compile(Paths.get(uri.getPath()), change.getText());
            QssLogger.logger.atInfo().log("Compile result: %s", result);
            PublishDiagnosticsParams diagnostics = new PublishDiagnosticsParams();
            diagnostics.setUri(params.getTextDocument().getUri());
            for (Message message : result.getMessages()) {
                Diagnostic diagnostic = new Diagnostic();
                diagnostic.setRange(new Range(
                        new Position(message.range.start.line, message.range.start.character),
                        new Position(message.range.end.line, message.range.end.character)
                ));
                switch (message.severity) {
                    case HINT:
                        diagnostic.setSeverity(DiagnosticSeverity.Hint);
                        break;
                    case INFORMATION:
                        diagnostic.setSeverity(DiagnosticSeverity.Information);
                        break;
                    case WARNING:
                        diagnostic.setSeverity(DiagnosticSeverity.Warning);
                        break;
                    case ERROR:
                        diagnostic.setSeverity(DiagnosticSeverity.Error);
                        break;
                }
                diagnostic.setMessage(message.message);
                diagnostic.setSource("qss");
                diagnostics.getDiagnostics().add(diagnostic);
            }
            QssLanguageServer.getInstance().getClient().publishDiagnostics(diagnostics);
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams didCloseTextDocumentParams) {

    }

    @Override
    public void didSave(DidSaveTextDocumentParams didSaveTextDocumentParams) {

    }
}
