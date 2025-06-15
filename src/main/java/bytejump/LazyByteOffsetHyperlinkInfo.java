package bytejump;

import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

class LazyByteOffsetHyperlinkInfo implements HyperlinkInfo {
    private final Project project;
    private final String fileName;
    private final int byteOffset;

    LazyByteOffsetHyperlinkInfo(Project project, String fileName, int byteOffset) {
        this.project = project;
        this.fileName = fileName;
        this.byteOffset = byteOffset;
    }

    @Override
    public void navigate(@NotNull Project project) {
        VirtualFile file = resolveFile(fileName);
        if (file == null) {
            throw new RuntimeException("File not found: " + fileName);
        }
        byte[] bytes;
        try {
            bytes = new String(file.contentsToByteArray(), file.getCharset()).getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var lineIndex = 0;
        var lineOffset = 0;
        var skip = 0;
        var delta = 0;
        var byteOffset = this.byteOffset;
        for (byte b : bytes) {
            if (byteOffset-- == 0) {
                break;
            } else if (skip > 0) {
                if (--skip == 0) {
                    lineOffset += delta;
                }
            } else if (b == '\n') {
                lineIndex++;
                lineOffset = 0;
            } else {
                int unsigned = b & 0xFF;
                if (unsigned < 0x80) {
                    lineOffset++;
                } else {
                    if ((unsigned & 0xE0) == 0xC0) {
                        delta = 1;
                        skip = 1;
                    } else if ((unsigned & 0xF0) == 0xE0) {
                        delta = 2;
                        skip = 2;
                    } else if ((unsigned & 0xF8) == 0xF0) {
                        delta = 2;
                        skip = 3;
                    }
                }
            }
        }

        new OpenFileDescriptor(project, file, lineIndex, lineOffset).navigate(true);
    }


    private @Nullable VirtualFile resolveFile(String text) {
        VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(text);
        if (vf != null) return vf;
        String projRoot = project.getBasePath();
        if (projRoot != null) {
            vf = LocalFileSystem.getInstance().findFileByPath(projRoot + "/" + text);
            if (vf != null) return vf;
        }
        if (projRoot != null) {
            return LocalFileSystem.getInstance().findFileByPath(projRoot + "/" + text);
        }
        return null;
    }
}