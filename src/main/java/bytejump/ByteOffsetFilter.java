package bytejump;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.LazyFileHyperlinkInfo;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ByteOffsetFilter implements Filter {
    private static final Pattern PATTERN =
            Pattern.compile("(?<path>[^\\s\\[\\]]+)\\[(?<offset>\\d+)]");

    private final Project project;

    public ByteOffsetFilter(Project project) {
        this.project = project;
    }

    @Override
    public @Nullable Result applyFilter(@NotNull String line, int entireLength) {
        Matcher m = PATTERN.matcher(line);
        List<ResultItem> items = new java.util.ArrayList<>();

        while (m.find()) {
            String pathText = m.group("path");
            int byteOffset = Integer.parseInt(m.group("offset"));
            int highlightStart = entireLength - line.length() + m.start();
            int highlightEnd = entireLength - line.length() + m.end();

            HyperlinkInfo link = new LazyByteOffsetHyperlinkInfo(project, pathText, byteOffset);
            items.add(new ResultItem(highlightStart, highlightEnd, link));
        }

        return items.isEmpty() ? null : new Result(items);
    }
}
