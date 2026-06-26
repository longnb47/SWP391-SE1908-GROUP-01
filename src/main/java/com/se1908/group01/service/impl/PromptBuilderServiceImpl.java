package com.se1908.group01.service.impl;

import com.se1908.group01.dto.RetrievedChunk;
import com.se1908.group01.enums.ChatMode;
import com.se1908.group01.enums.KnowledgePolicy;
import com.se1908.group01.service.PromptBuilderService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PromptBuilderServiceImpl implements PromptBuilderService {

	private static final int MAX_CHUNK_CHARACTERS = 2000;

	@Override
	public String buildDocumentQuestionPrompt(String question, List<RetrievedChunk> chunks) {
		var prompt = new StringBuilder();
		prompt.append("""
				You are an AI study assistant.
				Answer the user's question using only the provided document context.
				If the answer is not present in the context, say: "The answer cannot be found in the selected document."
				Do not use outside knowledge.
				Do not invent citations, facts, or document content.

				Document context:
				""");

		for (int i = 0; i < chunks.size(); i++) {
			var retrieved = chunks.get(i);
			var chunk = retrieved.getChunk();
			prompt.append("\n[Chunk ")
					.append(i + 1)
					.append(" | chunkIndex=")
					.append(chunk.getChunkIndex());
			if (chunk.getPageNumber() != null) {
				prompt.append(" | page=").append(chunk.getPageNumber());
			}
			prompt.append("]\n")
					.append(truncate(chunk.getContent()))
					.append("\n");
		}

		prompt.append("\nUser question:\n")
				.append(question);

		return prompt.toString();
	}

	@Override
	public String buildMultiDocumentQuestionPrompt(
			ChatMode mode, KnowledgePolicy knowledgePolicy, String context, String question) {

		var systemMessage = resolveSystemMessage(mode, knowledgePolicy);

		return "[SYSTEM]\n"
				+ systemMessage
				+ "\n\n[USER]\n"
				+ "CONTEXT:\n"
				+ "-----------\n"
				+ context
				+ "\n-----------\n"
				+ "QUESTION:\n"
				+ question;
	}

	private String resolveSystemMessage(ChatMode mode, KnowledgePolicy policy) {
		if (mode == ChatMode.SELECTED_DOCUMENTS) {
			return """
					You are an AI study assistant.
					Answer the user's question using ONLY the information contained in the provided context from the documents explicitly selected by the user.

					Rules:
					- You MUST NOT use any knowledge that is not explicitly present in the context.
					- If the selected documents do not contain enough information to answer the question, you MUST say that the information is not available in the selected documents and that you cannot answer.
					- You MUST NOT rely on your own pretrained or external knowledge outside of what is written in the context, even if you believe it to be correct.
					- You MUST NOT invent or guess facts.
					- You MUST NOT claim that information comes from the selected documents if it is not clearly stated in the context.

					When answering:
					- Be concise and precise.
					- Base every statement strictly on the provided context from the selected documents.
					- If something is not in the context, treat it as unknown and say that the selected documents do not contain that information.""";
		}
		// USER_STORAGE — split by policy
		if (policy == KnowledgePolicy.DOCUMENTS_PLUS_GENERAL) {
			return """
					You are an assistant that answers questions using ONLY the information contained in the provided context.
					The context may include:
					- the user's private stored documents, and
					- public documents available in the system.

					Rules:
					- You MUST NOT use any knowledge that is not explicitly present in the context.
					- If the context does not contain enough information to answer the question, you MUST say that the information is not available in the documents and that you cannot answer.
					- You MUST NOT rely on your own pretrained or external knowledge outside of what is written in the context, even if you believe it to be correct.
					- You MUST NOT invent or guess facts.
					- You MUST NOT claim that information comes from the documents if it is not clearly stated in the context.
					- If the context mixes private and public documents, do not reveal which parts are private versus public; simply refer to them as "the provided documents" or "the provided context".

					When answering:
					- Be concise and precise.
					- Base every statement strictly on the provided context.
					- If something is not in the context, treat it as unknown and say that the documents do not contain that information.""";
		}
		// USER_STORAGE + DOCUMENTS_ONLY
		return """
				You are an AI study assistant.
				Answer the user's question using ONLY the information contained in the provided context from the user's stored documents.

				Rules:
				- You MUST NOT use any knowledge that is not explicitly present in the context.
				- If the context does not contain enough information to answer the question, you MUST say that the information is not available in the user's documents and that you cannot answer.
				- You MUST NOT rely on your own pretrained or external knowledge outside of what is written in the context, even if you believe it to be correct.
				- You MUST NOT invent or guess facts.
				- You MUST NOT claim that information comes from the documents if it is not clearly stated in the context.

				When answering:
				- Be concise and precise.
				- Base every statement strictly on the provided context.
				- If something is not in the context, treat it as unknown and say that the documents do not contain that information.""";
	}

	private String truncate(String content) {
		if (content == null || content.length() <= MAX_CHUNK_CHARACTERS) {
			return content;
		}
		return content.substring(0, MAX_CHUNK_CHARACTERS);
	}
}
