package com.dinochiesa.testng.tests;

import java.util.HashMap;

public class TestCase {

    private String _testName;
    private String _description;
    private String  _input; // filename
    private HashMap<String,String>  _properties; // JSON hash
    private HashMap<String,String>  _expected; // JSON hash

    // getters
    public String getTestName() { return _testName; }
    public String getDescription() { return _description; }
    public String getInput() { return _input; }
    public HashMap<String,String> getProperties() { return _properties; }
    public HashMap<String,String> getExpected() { return _expected; }

    // setters
    public void setTestName(String n) { _testName = n; }
    public void setDescription(String d) { _description = d; }
    public void setInput(String f) { _input = f; }
    public void setExpected(HashMap<String,String> hash) { _expected = hash; }
    public void setProperties(HashMap<String,String> hash) { _properties = hash; }
}
