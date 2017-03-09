// Base64.java
//
// This is the source code for a Java callout for Apigee Edge.
// This callout encodes a payload in Base64.
//
// --------------------------------------------
// This code is licensed under the Apache 2.0 license. See the LICENSE
// file that accompanies this source.
//
// ------------------------------------------------------------------

package com.dinochiesa.edgecallouts;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.MessageContext;
import com.apigee.flow.message.Message;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.codec.binary.Base64InputStream;

public class Base64 implements Execution {
    private final static String varprefix= "b64_";
    private Map properties; // read-only

    public Base64 (Map properties) {
        this.properties = properties;
    }
    private static String varName(String s) { return varprefix + s;}
    public ExecutionResult execute (final MessageContext msgCtxt,
                                    final ExecutionContext execContext) {
        try {
            InputStream is = new Base64InputStream(msgCtxt.getMessage().getContentAsStream(),true);
            byte[] bytes = IOUtils.toByteArray(is);
            String encoded = StringUtils.newStringUtf8(bytes);
            msgCtxt.setVariable(varName("result"), encoded);
        }
        catch (Exception e) {
            String error = e.toString();
            msgCtxt.setVariable(varName("exception"), error);
            int ch = error.lastIndexOf(':');
            if (ch >= 0) {
                msgCtxt.setVariable(varName("error"), error.substring(ch+2).trim());
            }
            else {
                msgCtxt.setVariable(varName("error"), error);
            }
            msgCtxt.setVariable(varName("stacktrace"), ExceptionUtils.getStackTrace(e));
            return ExecutionResult.ABORT;
        }
        return ExecutionResult.SUCCESS;
    }
}
