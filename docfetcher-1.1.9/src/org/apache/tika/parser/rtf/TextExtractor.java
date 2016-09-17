/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tika.parser.rtf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.sax.XHTMLContentHandler;
import org.apache.tika.utils.CharsetUtils;
import org.xml.sax.SAXException;

/* Tokenizes and performs a "shallow" parse of the RTF
 * document, just enough to properly decode the text.
 *
 * TODO: we should cutover to a "real" tokenizer (eg JFlex);
 * it should give better perf, by replacing the excessive
 * "else if" string compares with FSA traversal. */

public final class TextExtractor {

    // Hold pending bytes (encoded in the current charset)
    // for text output:
    private byte[] pendingBytes = new byte[16];
    private int pendingByteCount;
    private ByteBuffer pendingByteBuffer = ByteBuffer.wrap(pendingBytes);
      
    // Holds pending chars for text output
    private char[] pendingChars = new char[10];
    private int pendingCharCount;

    // Holds chars for a still-being-tokenized control word
    private byte[] pendingControl = new byte[10];
    private int pendingControlCount;

    // Used when we decode bytes -> chars using CharsetDecoder:
    private final char[] outputArray = new char[128];
    private final CharBuffer outputBuffer = CharBuffer.wrap(outputArray);

    // Reused when possible:
    private CharsetDecoder decoder;
    private String lastCharset;

    private String globalCharset = "windows-1252";
    private int globalDefaultFont = -1;
    private int curFontID = -1;

    // Holds the font table from this RTF doc, mapping
    // the font number (from \fN control word) to the
    // corresponding charset:
    private final Map<Integer,String> fontToCharset = new HashMap<Integer,String>();

    // Group stack: when we open a new group, we push
    // the previous group state onto the stack; when we
    // close the group, we restore it
    private final LinkedList<GroupState> groupStates = new LinkedList<GroupState>();

    // Current group state; in theory this initial
    // GroupState is unused because the RTF doc should
    // immediately open the top group (start with {):
    private GroupState groupState = new GroupState();

    private boolean inHeader = true;
    private int fontTableState;
    private int fontTableDepth;

    // Non null if we are processing metadata (title,
    // keywords, etc.) inside the info group:
    private String nextMetaData;
    private boolean inParagraph;

    private final StringBuilder headerBuffer = new StringBuilder();

    // Used to process the sub-groups inside the upr
    // group:
    private int uprState = -1;

    private final XHTMLContentHandler out;
    private final Metadata metadata;

    // How many next ansi chars we should skip; this
    // is 0 except when we are still in the "ansi
    // shadow" after seeing a unicode escape, at which
    // point it's set to the last ucN skip we had seen:
    int ansiSkip = 0;

    // The RTF doc has a "font table" that assigns ords
    // (f0, f1, f2, etc.) to fonts and charsets, using the
    // \fcharsetN control word.  This mapping maps from the
    // N to corresponding Java charset:
    private static final Map<Integer, String> FCHARSET_MAP = new HashMap<Integer, String>();
    static {
        FCHARSET_MAP.put(0, "windows-1252"); // ANSI
        // charset 1 is Default
        // charset 2 is Symbol

        FCHARSET_MAP.put(77, "MacRoman"); // Mac Roman
        FCHARSET_MAP.put(78, "Shift_JIS"); // Mac Shift Jis
        FCHARSET_MAP.put(79, "ms949"); // Mac Hangul
        FCHARSET_MAP.put(80, "GB2312"); // Mac GB2312
        FCHARSET_MAP.put(81, "Big5"); // Mac Big5
        FCHARSET_MAP.put(82, "johab"); // Mac Johab (old)
        FCHARSET_MAP.put(83, "MacHebrew"); // Mac Hebrew
        FCHARSET_MAP.put(84, "MacArabic"); // Mac Arabic
        FCHARSET_MAP.put(85, "MacGreek"); // Mac Greek
        FCHARSET_MAP.put(86, "MacTurkish"); // Mac Turkish
        FCHARSET_MAP.put(87, "MacThai"); // Mac Thai
        FCHARSET_MAP.put(88, "cp1250"); // Mac East Europe
        FCHARSET_MAP.put(89, "cp1251"); // Mac Russian

        FCHARSET_MAP.put(128, "MS932"); // Shift JIS
        FCHARSET_MAP.put(129, "ms949"); // Hangul
        FCHARSET_MAP.put(130, "ms1361"); // Johab
        FCHARSET_MAP.put(134, "ms936"); // GB2312
        FCHARSET_MAP.put(136, "ms950"); // Big5
        FCHARSET_MAP.put(161, "cp1253"); // Greek
        FCHARSET_MAP.put(162, "cp1254"); // Turkish
        FCHARSET_MAP.put(163, "cp1258"); // Vietnamese
        FCHARSET_MAP.put(177, "cp1255"); // Hebrew
        FCHARSET_MAP.put(178, "cp1256"); // Arabic
        // FCHARSET_MAP.put( 179, "" ); // Arabic Traditional
        // FCHARSET_MAP.put( 180, "" ); // Arabic user
        // FCHARSET_MAP.put( 181, "" ); // Hebrew user
        FCHARSET_MAP.put(186, "cp1257"); // Baltic

        FCHARSET_MAP.put(204, "cp1251"); // Russian
        FCHARSET_MAP.put(222, "ms874"); // Thai
        FCHARSET_MAP.put(238, "cp1250"); // Eastern European
        FCHARSET_MAP.put(254, "cp437"); // PC 437
        FCHARSET_MAP.put(255, "cp850"); // OEM
    }

    // The RTF may specify the \ansicpgN charset in the
    // header; this maps the N to the corresponding Java
    // character set:

    private static final Map<Integer, String> ANSICPG_MAP = new HashMap<Integer, String>();
    static {
        ANSICPG_MAP.put(437, "CP437");   // US IBM
        ANSICPG_MAP.put(708, "ISO-8859-6");   // Arabic (ASMO 708)
      
        ANSICPG_MAP.put(709, "windows-709");  // Arabic (ASMO 449+, BCON V4)
        ANSICPG_MAP.put(710, "windows-710");  // Arabic (transparent Arabic)
        ANSICPG_MAP.put(710, "windows-711");  // Arabic (Nafitha Enhanced)
        ANSICPG_MAP.put(710, "windows-720");  // Arabic (transparent ASMO)
        ANSICPG_MAP.put(819, "CP819");  // Windows 3.1 (US & Western Europe)
        ANSICPG_MAP.put(819, "CP819");  // Windows 3.1 (US & Western Europe)

        ANSICPG_MAP.put(819, "CP819");  // Windows 3.1 (US & Western Europe)
        ANSICPG_MAP.put(850, "CP850");  // IBM Multilingual
        ANSICPG_MAP.put(852, "CP852");  // Eastern European
        ANSICPG_MAP.put(860, "CP860");  // Portuguese
        ANSICPG_MAP.put(862, "CP862");  // Hebrew
        ANSICPG_MAP.put(863, "CP863");  // French Canadian
        ANSICPG_MAP.put(864, "CP864");  // Arabic
        ANSICPG_MAP.put(865, "CP865");  // Norwegian
        ANSICPG_MAP.put(866, "CP866");  // Soviet Union
        ANSICPG_MAP.put(874, "MS874");  // Thai
        ANSICPG_MAP.put(932, "MS932");  // Japanese
        ANSICPG_MAP.put(936, "MS936");  // Simplified Chinese
        ANSICPG_MAP.put(949, "CP949");  // Korean
        ANSICPG_MAP.put(950, "CP950");  // Traditional Chinese
        ANSICPG_MAP.put(1250, "CP1250");  // Eastern European
        ANSICPG_MAP.put(1251, "CP1251");  // Cyrillic
        ANSICPG_MAP.put(1252, "CP1252");  // Western European
        ANSICPG_MAP.put(1253, "CP1253");  // Greek
        ANSICPG_MAP.put(1254, "CP1254");  // Turkish
        ANSICPG_MAP.put(1255, "CP1255");  // Hebrew
        ANSICPG_MAP.put(1256, "CP1256");  // Arabic
        ANSICPG_MAP.put(1257, "CP1257");  // Baltic
        ANSICPG_MAP.put(1258, "CP1258");  // Vietnamese
        ANSICPG_MAP.put(1361, "x-Johab");  // Johab
        ANSICPG_MAP.put(10000, "MacRoman");  // Mac Roman
        ANSICPG_MAP.put(10001, "Shift_JIS");  // Mac Japan
        ANSICPG_MAP.put(10004, "MacArabic");  // Mac Arabic
        ANSICPG_MAP.put(10005, "MacHebrew");  // Mac Hebrew
        ANSICPG_MAP.put(10006, "MacGreek");  // Mac Hebrew
        ANSICPG_MAP.put(10007, "MacCyrillic");  // Mac Cyrillic
        ANSICPG_MAP.put(10029, "x-MacCentralEurope");  // MAC Latin2
        ANSICPG_MAP.put(10081, "MacTurkish");  // Mac Turkish
        ANSICPG_MAP.put(57002, "x-ISCII91");   // Devanagari

        // TODO: in theory these other charsets are simple
        // shifts off of Devanagari, so we could impl that
        // here:
        ANSICPG_MAP.put(57003, "windows-57003");   // Bengali
        ANSICPG_MAP.put(57004, "windows-57004");   // Tamil
        ANSICPG_MAP.put(57005, "windows-57005");   // Telugu
        ANSICPG_MAP.put(57006, "windows-57006");   // Assamese
        ANSICPG_MAP.put(57007, "windows-57007");   // Oriya
        ANSICPG_MAP.put(57008, "windows-57008");   // Kannada
        ANSICPG_MAP.put(57009, "windows-57009");   // Malayalam
        ANSICPG_MAP.put(57010, "windows-57010");   // Gujariti
        ANSICPG_MAP.put(57011, "windows-57011");   // Punjabi
    }

    public TextExtractor(XHTMLContentHandler out, Metadata metadata) {
        this.metadata = metadata;
        this.out = out;
    }

    private static boolean isHexChar(char ch) {
        return (ch >= '0' && ch <= '9') ||
            (ch >= 'a' && ch <= 'f') ||
            (ch >= 'A' && ch <= 'F');
    }

    private static boolean isAlpha(char ch) {
        return (ch >= 'a' && ch <= 'z') ||
            (ch >= 'A' && ch <= 'Z');
    }

    private static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    private static int hexValue(char ch) {
        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        } else if (ch >= 'a' && ch <= 'z') {
            return 10 + (ch - 'a');
        } else {
            assert ch >= 'A' && ch <= 'Z';
            return 10 + (ch - 'A');
        }
    }

    // Push pending bytes or pending chars:
    private void pushText() throws IOException, SAXException, TikaException {
        if (pendingByteCount != 0) {
            assert pendingCharCount == 0;
            pushBytes();
        } else {
            pushChars();
        }
    }

    // Buffers the byte (unit in the current charset) for
    // output:
    private void addOutputByte(byte b) throws IOException, SAXException, TikaException {

        if (pendingCharCount != 0) {
            pushChars();
        }

        // Save the byte in pending buffer:
        if (pendingByteCount == pendingBytes.length) {
            // Gradual but exponential growth:
            final byte[] newArray = new byte[(int) (pendingBytes.length*1.25)];
            System.arraycopy(pendingBytes, 0, newArray, 0, pendingBytes.length);
            pendingBytes = newArray;
            pendingByteBuffer = ByteBuffer.wrap(pendingBytes);
        }
        pendingBytes[pendingByteCount++] = b;
    }

    // Buffers a byte as part of a control word:
    private void addControl(byte b) {
        assert isAlpha((char) b);
        // Save the byte in pending buffer:
        if (pendingControlCount == pendingControl.length) {
            // Gradual but exponential growth:
            final byte[] newArray = new byte[(int) (pendingControl.length*1.25)];
            System.arraycopy(pendingControl, 0, newArray, 0, pendingControl.length);
            pendingControl = newArray;
        }
        pendingControl[pendingControlCount++] = b;
    }

    // Buffers a UTF16 code unit for output
    private void addOutputChar(char ch) throws IOException, SAXException, TikaException {
        if (pendingByteCount != 0) {
            pushBytes();
        }

        if (inHeader) {
            headerBuffer.append(ch);
        } else {
            if (pendingCharCount == pendingChars.length) {
                // Gradual but exponential growth:
                final char[] newArray = new char[(int) (pendingChars.length*1.25)];
                System.arraycopy(pendingChars, 0, newArray, 0, pendingChars.length);
                pendingChars = newArray;
            }
            pendingChars[pendingCharCount++] = ch;
        }
    }

    // Shallow parses the entire doc, writing output to
    // this.out and this.metadata
    public void extract(InputStream in) throws IOException, SAXException, TikaException {
        out.startDocument();

        int state = 0;
        int pushBack = -2;
        boolean negParam = false;
        char hex1 = 0;
        long param = 0;

        while (true) {
            final int b;
            if (pushBack != -2) {
                b = pushBack;
                pushBack = -2;
            } else {
                b = in.read();
            }
            if (b == -1) {
                break;
            }

            // NOTE: this is always a 8bit clean byte (ie
            // < 128), but we use a char for
            // convenience in the testing below:
            final char ch = (char) b;

            switch (state) {

            case 0:
                if (ch == '\\') {
                    state = 1;
                } else if (ch == '{') {
                    pushText();
                    processGroupStart();
                } else if (ch == '}') {
                    pushText();
                    processGroupEnd();
                } else if (ch != '\r' && ch != '\n' && (!groupState.ignore || nextMetaData != null)) {
                    // Linefeed and carriage return are not
                    // significant
                    if (ansiSkip != 0) {
                        ansiSkip--;
                    } else {
                        addOutputByte((byte) ch);
                    }
                }
                break;

            // saw \
            case 1:
                if (ch == '\'') {
                    // escaped hex char
                    state = 2;
                } else if (isAlpha(ch)) {
                    // control word
                    //pushText();
                    addControl((byte) ch);
                    state = 4;
                } else if (ch == '{' || ch == '}' || ch == '\\' || ch == '\r' || ch == '\n') {
                    // escaped char
                    addOutputByte((byte) ch);
                    state = 0;
                } else {
                    // control symbol, eg \* or \~
                    //pushText();
                    processControlSymbol(ch);
                    state = 0;
                }
                break;

            // saw \'
            case 2:
                if (isHexChar(ch)) {
                    hex1 = ch;
                    state = 3;
                } else {
                    // DOC ERROR (malformed hex escape): ignore
                    state = 0;
                }
                break;

            // saw \'x
            case 3:
                if (isHexChar(ch)) {
                    if (ansiSkip != 0) {
                        // Skip this ansi char since we are
                        // still in the shadow of a unicode
                        // escape:
                        ansiSkip--;
                    } else {
                        // Unescape:
                        addOutputByte((byte) (16*hexValue(hex1) + hexValue(ch)));
                    }
                    state = 0;
                } else {
                    // TODO: log a warning here, somehow?
                    // DOC ERROR (malformed hex escape):
                    // ignore
                    state = 0;
                }
                break;

            // inside control word
            case 4:
                if (isAlpha(ch)) {
                    // still in control word
                    addControl((byte) ch);
                } else if (ch == '-') {
                    // end of control word, start of negative parameter
                    negParam = true;
                    param = 0;
                    state = 5;
                } else if (isDigit(ch)) {
                    // end of control word, start of positive parameter
                    negParam = false;
                    param = (ch - '0');
                    state = 5;
                } else if (ch == ' ') {
                    // space is consumed as part of the
                    // control word, but is not added to the
                    // control word
                    processControlWord();
                    pendingControlCount = 0;
                    state = 0;
                } else {
                    processControlWord();
                    pendingControlCount = 0;
                    // eps transition back to start state
                    pushBack = ch;
                    state = 0;
                }
                break;

            // inside control word's numeric param
            case 5:
                if (isDigit(ch)) {
                    param = (10*param) + (ch - '0');
                } else {
                    if (negParam) {
                        param = -param;
                    }
                    processControlWord(param);
                    pendingControlCount = 0;
                    if (ch != ' ') {
                        // space is consumed as part of the
                        // control word
                        pushBack = ch;
                    }
                    state = 0;
                }
                break;
              
            default:
                throw new RuntimeException("invalid state");
            }
        }

        endParagraph(false);
        out.endDocument();
    }

    private void lazyStartParagraph() throws IOException, SAXException, TikaException {
        if (!inParagraph) {
            // Ensure </i></b> order
            if (groupState.italic) {
                end("i");
            }
            if (groupState.bold) {
                end("b");
            }
            out.startElement("p");
            // Ensure <b><i> order
            if (groupState.bold) {
                start("b");
            }
            if (groupState.italic) {
                start("i");
            }
            inParagraph = true;
        }
    }

    private void endParagraph(boolean preserveStyles) throws IOException, SAXException, TikaException {
        pushText();
        if (inParagraph) {
            if (groupState.italic) {
                end("i");
                groupState.italic = preserveStyles;
            }
            if (groupState.bold) {
                end("b");
                groupState.bold = preserveStyles;
            }
            out.endElement("p");
            if (preserveStyles && (groupState.bold || groupState.italic)) {
                start("p");
                if (groupState.bold) {
                    start("b");
                }
                if (groupState.italic) {
                    start("i");
                }
                inParagraph = true;
            } else {
                inParagraph = false;
            }
        }
    }

    // Push pending UTF16 units to out ContentHandler
    private void pushChars() throws IOException, SAXException, TikaException {
        if (pendingCharCount != 0) {
            lazyStartParagraph();
            out.characters(pendingChars, 0, pendingCharCount);
            pendingCharCount = 0;
        }
    }

    // Decodes the buffered bytes in pendingBytes
    // into UTF16 code units, and sends the characters
    // to the out ContentHandler, if we are in the body,
    // else appends the characters to the headerBuffer
    private void pushBytes() throws IOException, SAXException, TikaException {
        if (pendingByteCount > 0 && (!groupState.ignore || nextMetaData != null)) {

            final CharsetDecoder decoder = getDecoder();
            pendingByteBuffer.limit(pendingByteCount);
            assert pendingByteBuffer.position() == 0;
            assert outputBuffer.position() == 0;

            while (true) {
                // We pass true for endOfInput because, when
                // we are called, we should have seen a
                // complete sequence of characters for this
                // charset:
                final CoderResult result = decoder.decode(pendingByteBuffer, outputBuffer, true);

                final int pos = outputBuffer.position();
                if (pos > 0) {
                    if (inHeader) {
                        headerBuffer.append(outputArray, 0, pos);
                    } else {
                        lazyStartParagraph();
                        out.characters(outputArray, 0, pos);
                    }
                    outputBuffer.position(0);
                }

                if (result == CoderResult.UNDERFLOW) {
                    break;
                }
            }

            while (true) {
                final CoderResult result = decoder.flush(outputBuffer);

                final int pos = outputBuffer.position();
                if (pos > 0) {
                    if (inHeader) {
                        headerBuffer.append(outputArray, 0, pos);
                    } else {
                        lazyStartParagraph();
                        out.characters(outputArray, 0, pos);
                    }
                    outputBuffer.position(0);
                }

                if (result == CoderResult.UNDERFLOW) {
                    break;
                }
            }

            // Reset for next decode
            decoder.reset();
            pendingByteBuffer.position(0);
        }

        pendingByteCount = 0;
    }

    // NOTE: s must be ascii alpha only
    private boolean equals(String s) {
        if (pendingControlCount != s.length()) {
            return false;
        }
        for(int idx=0;idx<pendingControlCount;idx++) {
            assert isAlpha(s.charAt(idx));
            if (((byte) s.charAt(idx)) != pendingControl[idx]) {
                return false;
            }
        }
        return true;
    }

    private void processControlSymbol(char ch) throws IOException, SAXException, TikaException {
        switch(ch) {
        case '~':
            // Non-breaking space -> unicode NON-BREAKING SPACE
            addOutputChar('\u00a0');
            break;
        case '*':
            // Ignorable destination (control words defined
            // after the 1987 RTF spec).  Note that
            // sometimes we un-ignore within this group, eg
            // when handling upr escape.
            groupState.ignore = true;
            break;
        case '-':
            // Optional hyphen -> unicode SOFT HYPHEN
            addOutputChar('\u00ad');
            break;
        case '_':
            // Non-breaking hyphen -> unicode NON-BREAKING HYPHEN
            addOutputChar('\u2011');
            break;
        default:
            break;
        }
    }

    private CharsetDecoder getDecoder() throws TikaException {
        final String charset = getCharset();
          
        // Common case: charset is same as last time, so
        // just reuse it:
        if (lastCharset == null || !charset.equals(lastCharset)) {
            decoder = CharsetUtils.forName(charset).newDecoder();
            if (decoder == null) {
                throw new TikaException("cannot find decoder for charset=" + charset);
            }
            decoder.onMalformedInput(CodingErrorAction.REPLACE);
            decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
            lastCharset = charset;
        }

        return decoder;
    }

    // Return current charset in-use
    private String getCharset() throws TikaException {
        // If a specific font (fN) was set, use its charset
        if (groupState.fontCharset != null) {
            return groupState.fontCharset;
        }

        // Else, if global default font (defN) was set, use
        // that
        if (globalDefaultFont != -1 && !inHeader) {
            final String cs = fontToCharset.get(globalDefaultFont);
            if (cs != null) {
                return cs;
            }
        }

        // Else, use the global charset
        if (globalCharset == null) {
            throw new TikaException("unable to determine charset");
        }

        return globalCharset;
    }

    // Handle control word that takes a parameter:
    // Param is long because spec says max value is 1+ Integer.MAX_VALUE!
    private void processControlWord(long param) throws IOException, SAXException, TikaException {

        // TODO: afN?  (associated font number)

        // TODO: do these alter text output...?
        /*
            } else if (equals("stshfdbch")) {
                // font to be used by default in
                // style sheet for East Asian chars
                // arg N is font table entry
            } else if (equals("stshfloch")) {
                // font to be used by default in
                // style sheet for ASCII chars
                // arg N is font table entry
            } else if (equals("stshfhich")) {
                // font to be used by default in
                // style sheet for High Ansi chars
                // arg N is font table entry
            } else if (equals("stshfbi")) {
                // style sheet for Complex Scripts (BIDI) chars
                // arg N is font table entry
                */

        // TODO: inefficient that we check equals N times;
        // we'd get better perf w/ real lexer (eg
        // JFlex), which uses single-pass FSM to do cmp:
        if (inHeader) {
            if (equals("ansicpg")) {
                // ANSI codepage
                final String cs = ANSICPG_MAP.get((int) param);
                if (cs != null) {
                    globalCharset = cs;
                }
            } else if (equals("deff")) {
                // Default font
                globalDefaultFont = (int) param;
            }

            if (fontTableState == 1) {
                // Still inside font table -- record the
                // mappings of fN to the fcharset:
                if (groupState.depth < fontTableDepth) {
                    fontTableState = 2;
                } else {
                    if (equals("f")) {
                        // Start new font definition
                        curFontID = (int) param;
                    } else if (equals("fcharset")) {
                        final String cs = FCHARSET_MAP.get((int) param);
                        if (cs != null) {
                            fontToCharset.put(curFontID, cs);
                        }
                    }
                }
            }
        } else {
            // In document
            if (equals("b")) {
                // b0
//                assert param == 0;
                if (groupState.bold) {
                    pushText();
                    if (groupState.italic) {
                        end("i");
                    }
                    end("b");
                    if (groupState.italic) {
                        start("i");
                    }
                    groupState.bold = false;
                }
            } else if (equals("i")) {
                // i0
//                assert param == 0;
                if (groupState.italic) {
                    pushText();
                    end("i");
                    groupState.italic = false;
                }
            } else if (equals("f")) {
                // Change current font
                final String fontCharset = fontToCharset.get((int) param);
                if (fontCharset != null) {
                    groupState.fontCharset = fontCharset;
                } else {
                    // DOC ERROR: font change referenced a
                    // non-table'd font number
                    // TODO: log a warning?  Throw an exc?
                    groupState.fontCharset = null;
                }
            }
        }

        // Process unicode escape.  This can appear in doc
        // or in header, since the metadata (info) fields
        // in the header can be unicode escaped as well:
        if (pendingControl[0] == 'u') {
            if (pendingControlCount == 1) {
                // Unicode escape
                if (!groupState.ignore) {
                    final char utf16CodeUnit = (char) (((int) param) & 0xffff);
                    addOutputChar(utf16CodeUnit);
                }

                // After seeing a unicode escape we must
                // skip the next ucSkip ansi chars (the
                // "unicode shadow")
                ansiSkip = groupState.ucSkip;
            } else if (pendingControlCount == 2 && pendingControl[1] == 'c') {
                // Change unicode shadow length
                groupState.ucSkip = (int) param;
            }
        }
    }

    private void end(String tag) throws IOException, SAXException, TikaException {
        out.endElement(tag);
    }

    private void start(String tag) throws IOException, SAXException, TikaException {
        out.startElement(tag);
    }

    // Handle non-parameter control word:
    private void processControlWord() throws IOException, SAXException, TikaException {
        if (inHeader) {
            if (equals("ansi")) {
                globalCharset = "cp1252";
            } else if (equals("pca")) {
                globalCharset = "cp850";
            } else if (equals("pc")) {
                globalCharset = "cp437";
            } else if (equals("mac")) {
                globalCharset = "MacRoman";
            }

            if (equals("colortbl") || equals("stylesheet") || equals("fonttbl")) {
                groupState.ignore = true;
            }

            if (uprState == -1) {
                // TODO: we can also parse \creatim, \revtim,
                // \printim, \version, \nofpages, \nofwords,
                // \nofchars, etc.
                if (equals("author")) {
                    nextMetaData = Metadata.AUTHOR;
                } else if (equals("title")) {
                    nextMetaData = Metadata.TITLE;
                } else if (equals("subject")) {
                    nextMetaData = Metadata.SUBJECT;
                } else if (equals("keywords")) {
                    nextMetaData = Metadata.KEYWORDS;
                } else if (equals("category")) {
                    nextMetaData = Metadata.CATEGORY;
                } else if (equals("comment")) {
                    nextMetaData = Metadata.COMMENT;
                } else if (equals("company")) {
                    nextMetaData = Metadata.COMPANY;
                } else if (equals("manager")) {
                    nextMetaData = Metadata.MANAGER;
                } else if (equals("template")) {
                    nextMetaData = Metadata.TEMPLATE;
                }
            }

            if (fontTableState == 0) {
                // Didn't see font table yet
                if (equals("fonttbl")) {
                    fontTableState = 1;
                    fontTableDepth = groupState.depth;
                }
            } else if (fontTableState == 1) {
                // Inside font table
                if (groupState.depth < fontTableDepth) {
                    fontTableState = 2;
                }
            }

            if (!groupState.ignore && (equals("par") || equals("pard") || equals("sect") || equals("sectd") || equals("plain") || equals("ltrch") || equals("rtlch"))) {
                inHeader = false;
            }
        } else {
            if (equals("b")) {
                if (!groupState.bold) {
                    pushText();
                    lazyStartParagraph();
                    if (groupState.italic) {
                        // Make sure nesting is always <b><i>
                        end("i");
                    }
                    groupState.bold = true;
                    start("b");
                    if (groupState.italic) {
                        start("i");
                    }
                }
            } else if (equals("i")) {
                if (!groupState.italic) {
                    pushText();
                    lazyStartParagraph();
                    groupState.italic = true;
                    start("i");
                }
            }
        }

        if (equals("pard")) {
            // Reset styles
            pushText();
            if (groupState.italic) {
                end("i");
                groupState.italic = false;
            }
            if (groupState.bold) {
                end("b");
                groupState.bold = false;
            }
        } else if (equals("par")) {
            endParagraph(true);
        } else if (equals("shptxt")) {
            pushText();
            // Text inside a shape
            groupState.ignore = false;
        } else if (equals("atnid")) {
            pushText();
            // Annotation ID
            groupState.ignore = false;
        } else if (equals("atnauthor")) {
            pushText();
            // Annotation author
            groupState.ignore = false;
        } else if (equals("annotation")) {
            pushText();
            // Annotation
            groupState.ignore = false;
        } else if (equals("cell")) {
            // TODO: we should produce a table output here?
            //addOutputChar(' ');
            endParagraph(true);
        } else if (equals("pict")) {
            pushText();
            // TODO: create img tag?  but can that support
            // embedded image data?
            groupState.ignore = true;
        } else if (equals("line")) {
            addOutputChar('\n');
        } else if (equals("column")) {
            addOutputChar(' ');
        } else if (equals("page")) {
            addOutputChar('\n');
        } else if (equals("softline")) {
            addOutputChar('\n');
        } else if (equals("softcolumn")) {
            addOutputChar(' ');
        } else if (equals("softpage")) {
            addOutputChar('\n');
        } else if (equals("tab")) {
            addOutputChar('\t');
        } else if (equals("upr")) {
            uprState = 0;
        } else if (equals("ud") && uprState == 1) {
            uprState = -1;
            // 2nd group inside the upr destination, which
            // contains the unicode encoding of the text, so
            // we want to keep that:
            groupState.ignore = false;
        } else if (equals("bullet")) {
            // unicode BULLET
            addOutputChar('\u2022');
        } else if (equals("endash")) {
            // unicode EN DASH
            addOutputChar('\u2013');
        } else if (equals("emdash")) {
            // unicode EM DASH
            addOutputChar('\u2014');
        } else if (equals("enspace")) {
            // unicode EN SPACE
            addOutputChar('\u2002');
        } else if (equals("qmspace")) {
            // quarter em space -> unicode FOUR-PER-EM SPACE
            addOutputChar('\u2005');
        } else if (equals("emspace")) {
            // unicode EM SPACE
            addOutputChar('\u2003');
        } else if (equals("lquote")) {
            // unicode LEFT SINGLE QUOTATION MARK
            addOutputChar('\u2018');
        } else if (equals("rquote")) {
            // unicode RIGHT SINGLE QUOTATION MARK
            addOutputChar('\u2019');
        } else if (equals("ldblquote")) {
            // unicode LEFT DOUBLE QUOTATION MARK
            addOutputChar('\u201C');
        } else if (equals("rdblquote")) {
            // unicode RIGHT DOUBLE QUOTATION MARK
            addOutputChar('\u201D');
        }
    }

    // Push new GroupState
    private void processGroupStart() throws IOException {
        ansiSkip = 0;
        // Push current groupState onto the stack
        groupStates.add(groupState);

        // Make new GroupState
        groupState = new GroupState(groupState);
        assert groupStates.size() == groupState.depth: "size=" + groupStates.size() + " depth=" + groupState.depth;

        if (uprState == 0) {
            uprState = 1;
            groupState.ignore = true;
        }
    }

    // Pop current GroupState
    private void processGroupEnd() throws IOException, SAXException, TikaException {

        if (inHeader) {
            if (nextMetaData != null) {
                metadata.add(nextMetaData, headerBuffer.toString());
                nextMetaData = null;
            }
            headerBuffer.setLength(0);
        }

        assert groupState.depth > 0;
        ansiSkip = 0;

        // Restore group state:
        final GroupState outerGroupState = groupStates.removeLast();

        // Close italic, if outer does not have italic or
        // bold changed:
        if (groupState.italic) {
            if (!outerGroupState.italic ||
                groupState.bold != outerGroupState.bold) {
                end("i");
                groupState.italic = false;
            }
        }

        // Close bold
        if (groupState.bold && !outerGroupState.bold) {
            end("b");
        }

        // Open bold
        if (!groupState.bold && outerGroupState.bold) {
            start("b");
        }

        // Open italic
        if (!groupState.italic && outerGroupState.italic) {
            start("i");
        }
        groupState = outerGroupState;
        assert groupStates.size() == groupState.depth;
    }
}
