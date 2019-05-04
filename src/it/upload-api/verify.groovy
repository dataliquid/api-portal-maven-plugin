File logFile = new File( basedir, "build.log" );

assert logFile.isFile() : "build log not exists"
assert logFile.text.contains("[INFO] API openapi/openapi-spec-v1.yaml uploaded successfully.") : "API upload failure"
assert logFile.text.contains("[INFO] API openapi/openapi-spec-v2.yaml uploaded successfully.") : "API upload failure"
assert logFile.text.contains("[WARNING] API openapi/openapi-spec-v3.yaml upload failed. HTTP Response: 401 - Unauthorized") : "HTTP 401 expected"
assert logFile.text.contains("[WARNING] API openapi/openapi-spec-v4.yaml upload failed. HTTP Response: 422 - Unprocessable Entity") : "HTTP 422 expected"