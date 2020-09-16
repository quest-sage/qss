import * as assert from 'assert';
import { before } from 'mocha';
import { getDocUri, activate } from '../helper';

// You can import and use all API from the 'vscode' module
// as well as import your extension to test it
import * as vscode from 'vscode';
// import * as myExtension from '../extension';

suite('Should do completion', () => {
	const docUri = getDocUri('completion.qss');
  
	test('Completes JS/TS in txt file', async () => {
	  await testCompletion(docUri, new vscode.Position(0, 0), {
		items: [
		  { label: 'sayHello()', kind: vscode.CompletionItemKind.Snippet },
		]
	  });
	});
  });

async function testCompletion(
	docUri: vscode.Uri,
	position: vscode.Position,
	expectedCompletionList: vscode.CompletionList
  ) {
	await activate(docUri);

	// Executing the command `vscode.executeCompletionItemProvider` to simulate triggering completion
	const actualCompletionList = (await vscode.commands.executeCommand(
	  'vscode.executeCompletionItemProvider',
	  docUri,
	  position
	)) as vscode.CompletionList;

	assert.ok(actualCompletionList.items.length >= expectedCompletionList.items.length);
	expectedCompletionList.items.forEach((expectedItem, i) => {
	  const actualItem = actualCompletionList.items[i];
	  assert.strictEqual(actualItem.label, expectedItem.label);
	  assert.strictEqual(actualItem.kind, expectedItem.kind);
	});
  }
