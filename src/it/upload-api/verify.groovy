/**
 * Copyright © 2019 dataliquid GmbH | www.dataliquid.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
File logFile = new File( basedir, "build.log" );

assert logFile.isFile() : "build log not exists"
assert logFile.text.contains("[INFO] API openapi/openapi-spec-v1.yaml uploaded successfully.") : "API upload failure"
assert logFile.text.contains("[INFO] API openapi/openapi-spec-v2.yaml uploaded successfully.") : "API upload failure"
assert logFile.text.contains("[WARNING] API openapi/openapi-spec-v3.yaml upload failed. HTTP Response: 401 - Unauthorized") : "HTTP 401 expected"
assert logFile.text.contains("[WARNING] API openapi/openapi-spec-v4.yaml upload failed. HTTP Response: 422 - Unprocessable Entity") : "HTTP 422 expected"