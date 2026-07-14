package com.se1908.group01.entity;

/** Lifecycle states returned to the UI while document ingestion runs asynchronously. */
public enum DocumentStatus {
	UPLOADED,
	PARSING,
	INDEXING,
	READY,
	FAILED
}

