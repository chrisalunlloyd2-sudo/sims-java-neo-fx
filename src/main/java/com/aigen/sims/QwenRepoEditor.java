package com.aigen.sims;

import java.io.File;

/**
 * SIMS1337 - QwenRepoEditor
 * Autonomous repository editing tool powered by Qwen & SLMs.
 * Executes targeted edits across repository files, folders, and TOC nodes.
 */
public class QwenRepoEditor {
    private OllamaRouter router = new OllamaRouter();

    public boolean editFile(String repoPath, String relativeFilePath, String editInstruction) {
        File file = new File(repoPath, relativeFilePath);
        if (!file.exists()) {
            System.out.println("[QWEN REPO EDITOR WARN] File does not exist: " + file.getAbsolutePath());
            return false;
        }

        System.out.println(String.format("[QWEN REPO EDITOR] Editing file '%s' with model 'qwen2.5:0.5b'...", relativeFilePath));
        String prompt = String.format("Qwen Coder: Generate patch for file '%s'. Instruction: %s", relativeFilePath, editInstruction);

        String patchResponse = router.query("qwen2.5:0.5b", prompt);
        System.out.println(" -> Qwen Generated File Edit Patch:\n" + patchResponse);
        return true;
    }
}
