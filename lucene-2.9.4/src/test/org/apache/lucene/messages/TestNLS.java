package org.apache.lucene.messages;

/**
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

import java.util.Locale;

import junit.framework.TestCase;

/**
 */
public class TestNLS extends TestCase {
  public void testMessageLoading() {
    Message invalidSyntax = new MessageImpl(
        MessagesTestBundle.Q0001E_INVALID_SYNTAX, new Object[]{"XXX"});
    assertEquals("Syntax Error: XXX", invalidSyntax.getLocalizedMessage());
  }

  public void testMessageLoading_ja() {
    Message invalidSyntax = new MessageImpl(
        MessagesTestBundle.Q0001E_INVALID_SYNTAX, new Object[]{"XXX"});
    assertEquals("構文エラー: XXX", invalidSyntax
        .getLocalizedMessage(Locale.JAPANESE));
  }

  public void testNLSLoading() {
    String message = NLS
        .getLocalizedMessage(MessagesTestBundle.Q0004E_INVALID_SYNTAX_ESCAPE_UNICODE_TRUNCATION);
    assertEquals("Truncated unicode escape sequence.", message);

    message = NLS.getLocalizedMessage(MessagesTestBundle.Q0001E_INVALID_SYNTAX,
        new Object[]{"XXX"});
    assertEquals("Syntax Error: XXX", message);
  }

  public void testNLSLoading_ja() {
    String message = NLS.getLocalizedMessage(
        MessagesTestBundle.Q0004E_INVALID_SYNTAX_ESCAPE_UNICODE_TRUNCATION,
        Locale.JAPANESE);
    assertEquals("切り捨てられたユニコード・エスケープ・シーケンス。", message);

    message = NLS.getLocalizedMessage(MessagesTestBundle.Q0001E_INVALID_SYNTAX,
        Locale.JAPANESE, new Object[]{"XXX"});
    assertEquals("構文エラー: XXX", message);
  }

  public void testNLSLoading_xx_XX() {
    Locale locale = new Locale("xx", "XX", "");
    String message = NLS.getLocalizedMessage(
        MessagesTestBundle.Q0004E_INVALID_SYNTAX_ESCAPE_UNICODE_TRUNCATION,
        locale);
    assertEquals("Truncated unicode escape sequence.", message);

    message = NLS.getLocalizedMessage(MessagesTestBundle.Q0001E_INVALID_SYNTAX,
        locale, new Object[]{"XXX"});
    assertEquals("Syntax Error: XXX", message);
  }

  public void testMissingMessage() {
    Locale locale = Locale.ENGLISH;
    String message = NLS.getLocalizedMessage(
        MessagesTestBundle.Q0005E_MESSAGE_NOT_IN_BUNDLE, locale);

    assertEquals("Message with key:Q0005E_MESSAGE_NOT_IN_BUNDLE and locale: "
        + locale.toString() + " not found.", message);
  }
}
