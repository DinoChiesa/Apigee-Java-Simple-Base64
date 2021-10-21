// Base64.java
//
// Copyright (c) 2018-2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package com.google.apigee.callouts;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.MessageContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;

public class Base64Callout extends CalloutBase implements Execution {
  private enum Base64Action {
    Encode,
    Decode
  }

  public Base64Callout(Map properties) {
    super(properties);
  }

  public String getVarnamePrefix() {
    return "b64_";
  }

  private Base64Action getAction(MessageContext msgCtxt) throws Exception {
    String action = getSimpleRequiredProperty("action", msgCtxt);
    if (action == null) throw new IllegalStateException("action value is missing");
    action = action.toLowerCase();
    if (action.equals("decode")) return Base64Action.Decode;
    if (action.equals("encode")) return Base64Action.Encode;
    throw new IllegalStateException("action value is unknown: (" + action + ")");
  }

  private boolean getMime(MessageContext msgCtxt) throws Exception {
    String encoding = getSimpleOptionalProperty("encoding", msgCtxt);
    if (encoding == null) return false;
    if (encoding.toLowerCase().equals("mime")) return true;
    return false;
  }

  private boolean getUrlEncoding(MessageContext msgCtxt) throws Exception {
    String encoding = getSimpleOptionalProperty("encoding", msgCtxt);
    if (encoding == null) return false;
    if (encoding.toLowerCase().equals("url")) return true;
    return false;
  }

  Base64.Encoder getEncoder(MessageContext msgCtxt) {
    String encoding = getSimpleOptionalProperty("encoding", msgCtxt);
    if (encoding == null) return Base64.getEncoder();
    encoding = encoding.toLowerCase();
    if (encoding.equals("mime")) return Base64.getMimeEncoder();
    if (encoding.equals("url")) return Base64.getUrlEncoder().withoutPadding();
    return Base64.getEncoder();
  }

  Base64.Decoder getDecoder(MessageContext msgCtxt) {
    String encoding = getSimpleOptionalProperty("encoding", msgCtxt);
    if (encoding == null) return Base64.getDecoder();
    encoding = encoding.toLowerCase();
    if (encoding.equals("mime")) return Base64.getMimeDecoder();
    if (encoding.equals("url")) return Base64.getUrlDecoder();
    return Base64.getDecoder();
  }

  byte[] readFully(InputStream in) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int nRead;
    byte[] data = new byte[16384];
    while ((nRead = in.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, nRead);
    }
    return buffer.toByteArray();
  }

  public ExecutionResult execute(final MessageContext msgCtxt, final ExecutionContext execContext) {
    try {
      Base64Action action = getAction(msgCtxt);
      msgCtxt.setVariable(varName("action"), action.name().toLowerCase());
      byte[] content = readFully(msgCtxt.getMessage().getContentAsStream());
      if (action == Base64Action.Encode) {
        byte[] encoded = getEncoder(msgCtxt).encode(content);
        ByteArrayInputStream bais = new ByteArrayInputStream(encoded);
        msgCtxt.getMessage().setContent(bais);
      } else {
        byte[] decoded = getDecoder(msgCtxt).decode(content);
        ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
        msgCtxt.getMessage().setContent(bais);
      }
      return ExecutionResult.SUCCESS;
    } catch (Exception e) {
      if (getDebug()) {
        System.out.println(getStackTrace(e));
      }
      setExceptionVariables(e, msgCtxt);
      msgCtxt.setVariable(varName("stacktrace"), getStackTrace(e));
      return ExecutionResult.ABORT;
    }
  }
}
