// retainLineLength.js
// ------------------------------------------------------------------

// We need to store the queryparam in a separate context variable
// so that it will be available in the response flow.

context.setVariable('base64_desired_linelength', context.getVariable('request.queryparam.linelength'));
