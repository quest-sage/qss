package com.thirds.qss.langserver;

import com.thirds.qss.QssLogger;
import com.thirds.qss.compiler.*;
import com.thirds.qss.compiler.Compiler;
import com.thirds.qss.compiler.tree.Script;
import com.thirds.qss.compiler.tree.SymbolMap;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class QssTextDocumentService implements TextDocumentService {
    private Compiler compiler;

    public void initialise(Path rootDir) {
        compiler = new Compiler(rootDir);
    }

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
        ScriptPath scriptPath = pathFromUri(textDocumentPositionParams.getTextDocument().getUri());
        SymbolMap symbolMap = compiler.getSymbolMap(scriptPath);
        if (symbolMap == null) {
            return CompletableFuture.completedFuture(null);
        }

        Optional<Symbol> selected = symbolMap.getSelected(from(textDocumentPositionParams.getPosition()));
        Optional<String> optionalDocs = selected.flatMap(Symbol::getTargetDocumentation);
        if (optionalDocs.isPresent()) {
            Hover hover = new Hover();
            String docs = optionalDocs.get();
            MarkupContent markupContents = new MarkupContent();
            markupContents.setKind("markdown");
            markupContents.setValue(docs);
            hover.setContents(markupContents);
            return CompletableFuture.completedFuture(hover);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(TextDocumentPositionParams textDocumentPositionParams) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends Location>> definition(TextDocumentPositionParams textDocumentPositionParams) {
        ScriptPath scriptPath = pathFromUri(textDocumentPositionParams.getTextDocument().getUri());
        SymbolMap symbolMap = compiler.getSymbolMap(scriptPath);
        if (symbolMap == null)
            return CompletableFuture.completedFuture(List.of());

        Optional<Symbol> selected = symbolMap.getSelected(from(textDocumentPositionParams.getPosition()));
        ArrayList<Location> locations = new ArrayList<>();
        selected.flatMap(Symbol::getTargetLocation).ifPresent(location -> locations.add(from(location)));
        return CompletableFuture.completedFuture(locations);
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

    private ScriptPath pathFromUri(String uri) {
        return new ScriptPath(Paths.get(QssLanguageServer.relativize(uri).getPath()));
    }

    private URI uriOf(ScriptPath filePath) {
        return QssLanguageServer.getRootDir().resolve(filePath.toPath()).toUri();
    }

    private Position from(com.thirds.qss.compiler.Position position) {
        return new Position(position.line, position.character);
    }

    private com.thirds.qss.compiler.Position from(Position position) {
        return new com.thirds.qss.compiler.Position(position.getLine(), position.getCharacter());
    }

    private Range from(com.thirds.qss.compiler.Range range) {
        return new Range(from(range.start), from(range.end));
    }

    private Location from(com.thirds.qss.compiler.Location location) {
        return new Location(
                location.getFilePath().toPath().toUri().toString(),
                from(location.getRange())
        );
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams didOpenTextDocumentParams) {
        try {
            compileAndPublishDiagnostics(didOpenTextDocumentParams.getTextDocument().getUri(), didOpenTextDocumentParams.getTextDocument().getText());
        } catch (Exception e) {
            QssLogger.logger.atSevere().withCause(e).log("Uncaught exception");
        }
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        try {
            for (TextDocumentContentChangeEvent change : params.getContentChanges()) {
                if (change.getRange() != null || change.getRangeLength() != null) {
                    QssLogger.logger.atSevere().log("Incremental file change not supported: %s", params);
                }

                compileAndPublishDiagnostics(params.getTextDocument().getUri(), change.getText());
            }
        } catch (Exception e) {
            QssLogger.logger.atSevere().withCause(e).log("Uncaught exception");
        }
    }

    private void compileAndPublishDiagnostics(String textDocumentUri, String fileContents) {
        URI uri = QssLanguageServer.relativize(textDocumentUri);

        // Don't parse or validate any files not in the bundle root of the currently compiled bundle.
        if (QssLanguageServer.getInstance().rootUri.relativize(URI.create(textDocumentUri)).equals(URI.create(textDocumentUri))) {
            // The given URI was not a child of the root URI.
            return;
        }

        QssLogger.logger.atInfo().log("Compiling %s", uri);
        ScriptPath filePath = new ScriptPath(Paths.get(uri.getPath()));
        compiler.overwriteCachedFileContent(filePath, fileContents);
        Messenger<Script> result = compiler.compile(filePath);
        QssLogger.logger.atInfo().log("Compile result: %s %s", compiler, result);

        PublishDiagnosticsParams diagnostics = new PublishDiagnosticsParams();
        diagnostics.setUri(textDocumentUri);
        for (Message message : result.getMessages()) {
            Diagnostic diagnostic = new Diagnostic();
            diagnostic.setRange(from(message.range));
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
            ArrayList<DiagnosticRelatedInformation> infos = new ArrayList<>();
            for (Message.MessageRelatedInformation info : message.infos) {
                infos.add(new DiagnosticRelatedInformation(
                        new Location(uriOf(info.location.getFilePath()).toString(), from(info.location.getRange())),
                        info.message
                ));
            }
            diagnostic.setRelatedInformation(infos);
            diagnostics.getDiagnostics().add(diagnostic);
        }
        QssLanguageServer.getInstance().getClient().publishDiagnostics(diagnostics);
    }

    @Override
    public void didClose(DidCloseTextDocumentParams didCloseTextDocumentParams) {

    }

    @Override
    public void didSave(DidSaveTextDocumentParams didSaveTextDocumentParams) {

    }
}
