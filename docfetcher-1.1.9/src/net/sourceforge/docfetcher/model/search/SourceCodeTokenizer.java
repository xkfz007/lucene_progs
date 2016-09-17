package net.sourceforge.docfetcher.model.search;

import java.io.Reader;
import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.util.Version;

/**
 *
 */
public final class SourceCodeTokenizer extends CharTokenizer {

	/**
	 * Construct a new SourceCodeTokenizer. * @param matchVersion Lucene version
	 * to match See {@link <a href="#version">above</a>}
	 *
	 * @param in
	 *          the input to split up into tokens
	 */
	public SourceCodeTokenizer(Version matchVersion, Reader in) {
		super(matchVersion, in);
	}

	/** Collects only characters which do not satisfy
	 * {@link Character#isWhitespace(int)}.
	 *  or ='.' or ='=' ...
	 */
	@Override
	protected boolean isTokenChar(int c) {
		return !(Character.isWhitespace(c) || (c=='.') || (c=='=') || (c=='"') 
				|| (c=='<') || (c=='>') || (c=='(') || (c==')')
				|| (c=='[') || (c==']') || (c=='/') || (c=='\\')
				|| (c=='{') || (c=='}') || (c=='+') || (c=='*'));
	}
}
