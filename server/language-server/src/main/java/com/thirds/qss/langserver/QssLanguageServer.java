package com.thirds.qss.langserver;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.*;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class QssLanguageServer implements LanguageServer, LanguageClientAware {
    public static URI rootUri;
    private final TextDocumentService textDocumentService;
    private final WorkspaceService workspaceService;
    private LanguageClient client;
    private int errorCode = 1;

    public QssLanguageServer() {
        this.textDocumentService = new QssTextDocumentService();
        this.workspaceService = new QssWorkspaceService();
    }

    /**
     * Returns a URI relative to the root of the workspace. For example, if uri was "file:///mnt/c/code/test",
     * the and the workspace was "file:///mnt/c/code", the return value would be "test".
     *
     * Basically just syntactic sugar for <code>rootUri.relativize(URI.create(...))</code>.
     */
    public static URI relativize(String uri) {
        return rootUri.relativize(URI.create(uri));
    }

    public static Path getRootDir() {
        return Paths.get(rootUri.getPath());
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams initializeParams) {
        rootUri = URI.create(initializeParams.getRootUri());

        // Initialize the InitializeResult for this LS.
        final InitializeResult initializeResult = new InitializeResult(new ServerCapabilities());

        // Set the capabilities of the LS to inform the client.
        initializeResult.getCapabilities().setTextDocumentSync(TextDocumentSyncKind.Full);
        CompletionOptions completionOptions = new CompletionOptions();
        initializeResult.getCapabilities().setCompletionProvider(completionOptions);

        QssLogger.logger.atConfig().log("Initialising QSS language server in %s with capabilities: %s", rootUri, initializeResult.getCapabilities());

        return CompletableFuture.supplyAsync(()->initializeResult);
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        // If shutdown request comes from client, set the error code to 0.
        errorCode = 0;
        return null;
    }

    @Override
    public void exit() {
        // Kill the LS on exit request from client.
        System.exit(errorCode);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        // Return the endpoint for language features.
        return this.textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        // Return the endpoint for workspace functionality.
        return this.workspaceService;
    }

    @Override
    public void connect(LanguageClient languageClient) {
        // Get the client which started this LS.
        this.client = languageClient;
    }
}
