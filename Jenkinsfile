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
pipeline {
 agent {
  node {
   label 'master'
  }
 }
 tools {
  maven 'maven-3'
 }
 stages {
  stage("Prepare WS") {
   steps {
    cleanWs()
    checkout scm
   }
  }
  stage("Build") {
   parallel {
    stage('Build') {
     when {
      not {
       branch "master"
      }
     }
     steps {
      sh "mvn -T 2C compile install -DskipTests"
     }
    }
    stage('Tests') {
     steps {
      sh "mvn -T 2C test-compile test -DfailIfNoTests=false"
     }
     post {
      always {
       step([$class: 'JUnitResultArchiver', allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml'])
      }
     }
    }
   }
  }
  stage("Deploy") {
   steps {
    sh "mvn deploy -DskipTests"
   }
  }
 }
}