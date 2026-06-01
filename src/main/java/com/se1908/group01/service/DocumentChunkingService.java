package com.se1908.group01.service;

import com.se1908.group01.dto.ChunkData;
import com.se1908.group01.dto.TextSegment;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DocumentChunkingService {

	private static final int MAX_CHARS = 1200;
	private static final int OVERLAP_CHARS = 200;

	public List<ChunkData> chunk(List<TextSegment> segments) {
		List<ChunkData> chunks = new ArrayList<>();
		int chunkIndex = 0;

		for (TextSegment segment : segments) {
			if (segment == null || !StringUtils.hasText(segment.getText())) {
				continue;
			}
			var text = segment.getText().trim();
			var pageNumber = segment.getPageNumber();

			for (String piece : splitWithOverlap(text, MAX_CHARS, OVERLAP_CHARS)) {
				if (StringUtils.hasText(piece)) {
					chunks.add(new ChunkData(chunkIndex++, pageNumber, piece));
				}
			}
		}

		return chunks;
	}

	private static List<String> splitWithOverlap(String text, int maxChars, int overlapChars) {
		List<String> out = new ArrayList<>();
		int start = 0;
		int len = text.length();
		while (start < len) {
			int end = Math.min(len, start + maxChars);

			// Prefer breaking on newline boundary when possible.
			int breakAt = lastBreak(text, start, end);
			if (breakAt > start + 200) {
				end = breakAt;
			}

			String chunk = text.substring(start, end).trim();
			if (!chunk.isEmpty()) {
				out.add(chunk);
			}

			if (end >= len) {
				break;
			}
			start = Math.max(0, end - overlapChars);
		}
		return out;
	}

	private static int lastBreak(String text, int start, int end) {
		int idx = text.lastIndexOf('\n', end - 1);
		if (idx >= start) {
			return idx;
		}
		idx = text.lastIndexOf(". ", end - 1);
		if (idx >= start) {
			return idx + 1;
		}
		return -1;
	}
}

