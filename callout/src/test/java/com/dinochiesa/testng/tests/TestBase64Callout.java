package com.dinochiesa.testng.tests;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;

import mockit.Mock;
import mockit.MockUp;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.message.MessageContext;
import com.apigee.flow.message.Message;

import com.dinochiesa.edgecallouts.Base64;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

public class TestBase64Callout {
    private final static String testDataDir = "src/test/resources/test-data";

    MessageContext msgCtxt;
    //String messageContent;
    InputStream messageContentStream;
    Message message;
    ExecutionContext exeCtxt;

    @BeforeMethod()
    public void beforeMethod() {

        msgCtxt = new MockUp<MessageContext>() {
            private Map variables;
            public void $init() {
                variables = new HashMap();
            }

            @Mock()
            public <T> T getVariable(final String name){
                if (variables == null) {
                    variables = new HashMap();
                }
                return (T) variables.get(name);
            }

            @Mock()
            public boolean setVariable(final String name, final Object value) {
                if (variables == null) {
                    variables = new HashMap();
                }
                variables.put(name, value);
                return true;
            }

            @Mock()
            public boolean removeVariable(final String name) {
                if (variables == null) {
                    variables = new HashMap();
                }
                if (variables.containsKey(name)) {
                    variables.remove(name);
                }
                return true;
            }

            @Mock()
            public Message getMessage() {
                return message;
            }
        }.getMockInstance();

        exeCtxt = new MockUp<ExecutionContext>(){ }.getMockInstance();

        message = new MockUp<Message>(){
            @Mock()
            public InputStream getContentAsStream() {
                // new ByteArrayInputStream(messageContent.getBytes(StandardCharsets.UTF_8));
                return messageContentStream;
            }
        }.getMockInstance();
    }


    @DataProvider(name = "batch1")
    public static Object[][] getDataForBatch1()
        throws IOException, IllegalStateException {

        // @DataProvider requires the output to be a Object[][]. The inner
        // Object[] is the set of params that get passed to the test method.
        // So, if you want to pass just one param to the constructor, then
        // each inner Object[] must have length 1.

        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Path currentRelativePath = Paths.get("");
        // String s = currentRelativePath.toAbsolutePath().toString();
        // System.out.println("Current relative path is: " + s);

        // read in all the *.json files in the test-data directory
        File testDir = new File(testDataDir);
        if (!testDir.exists()) {
            throw new IllegalStateException("no test directory.");
        }
        File[] files = testDir.listFiles();
        if (files.length == 0) {
            throw new IllegalStateException("no tests found.");
        }
        int c=0;
        ArrayList<TestCase> list = new ArrayList<TestCase>();
        for (File file : files) {
            String name = file.getName();
            if (name.endsWith(".json")) {
                TestCase tc = om.readValue(file, TestCase.class);
                tc.setTestName(name.substring(0,name.length()-5));
                list.add(tc);
            }
        }

        // OMG!!  Seriously? Is this the easiest way to generate a 2-d array?
        int n = list.size();
        Object[][] data = new Object[n][];
        for (int i = 0; i < data.length; i++) {
            data[i] = new Object[]{ list.get(i) };
        }
        return data;
    }

    @Test
    public void testDataProviders() throws IOException {
        Assert.assertTrue(getDataForBatch1().length > 0);
    }

    @Test(dataProvider = "batch1")
    public void test2_Configs(TestCase tc) throws Exception {
        if (tc.getDescription()!= null)
            System.out.printf("  %10s - %s\n", tc.getTestName(), tc.getDescription() );
        else
            System.out.printf("  %10s\n", tc.getTestName() );

        Path path = Paths.get(testDataDir, tc.getInput());
        if (!Files.exists(path)) {
            throw new IOException("file("+tc.getInput()+") not found");
        }

        messageContentStream = Files.newInputStream(path);

        Base64 callout = new Base64(tc.getProperties());

        // execute and retrieve output
        ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);

        String s = tc.getExpected().get("success");
        ExecutionResult expectedResult = (s!=null && s.toLowerCase().equals("true")) ?
                                           ExecutionResult.SUCCESS : ExecutionResult.ABORT;
        // check result and output
        if (expectedResult == actualResult) {
            if (expectedResult == ExecutionResult.SUCCESS) {
                String fname = tc.getExpected().get("output");
                path = Paths.get(testDataDir, fname);
                if (!Files.exists(path)) {
                    throw new IOException("expected output file("+fname+") not found");
                }
                byte[] expectedOutputBytes = IOUtils.toByteArray(Files.newInputStream(path));
                boolean stringOutput =
                    (((String)(msgCtxt.getVariable("b64_action"))).equals("encode")) &&
                    (boolean)(msgCtxt.getVariable("b64_wantString"));

                byte[] actualOutputBytes = (stringOutput) ?
                    ((String)msgCtxt.getVariable("b64_result")).getBytes(StandardCharsets.UTF_8) :
                    (byte[])(msgCtxt.getVariable("b64_result"));

                String digest1 = DigestUtils.sha256Hex(actualOutputBytes);
                String digest2 = DigestUtils.sha256Hex(expectedOutputBytes);
                if (!actualOutputBytes.equals(expectedOutputBytes)) {
                    //System.out.printf("  FAIL - content\n");
                    System.err.printf("    digest got: %s\n", digest1);
                    System.err.printf("    expected  : %s\n", digest2);
                    // the following will throw
                    Assert.assertEquals(digest1, digest2, "result not as expected");
                }
            }
            else {
                String expectedError = tc.getExpected().get("error");
                Assert.assertNotNull(expectedError, "broken test: no expected error specified");

                String actualError = msgCtxt.getVariable("b64_error");
                Assert.assertEquals(actualError, expectedError, "error not as expected");
            }
        }
        else {
            String observedError = msgCtxt.getVariable("b64_error");
            System.err.printf("    observed error: %s\n", observedError);

            Assert.assertEquals(actualResult, expectedResult, "result not as expected");
        }
        System.out.println("=========================================================");
    }

}
