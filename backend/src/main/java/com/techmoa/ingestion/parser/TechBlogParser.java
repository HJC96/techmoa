package com.techmoa.ingestion.parser;

import java.util.List;

public interface TechBlogParser {

    boolean supports(ParserType parserType);

    List<ParsedPost> fetch(SourceProfile sourceProfile);
}
