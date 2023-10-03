node ('maven_runner_java11') {
  stage('SCM') {
    checkout scm
  }
  stage('SonarQube Analysis') {
    def mvn = tool 'Default Maven';
    withSonarQubeEnv() {
      sh "${mvn}/bin/mvn clean verify sonar:sonar -Dsonar.projectKey=jewel-dashboardmanager -Dsonar.projectName=jewel-dashboardmanager"
    }
  }
  stage('Quality Gate') {
    timeout (time: 5, unit: 'MINUTES') {
       waitForQualityGate abortPipeline: true
       echo "code is good"
    }
  }
}
node('maven_runner_java11') {
	stage('backend_checkout') {
		dir('jewel-dashboardmanager') {
			checkout([$class: 'GitSCM', branches: [[name: '*/beta']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], \
				userRemoteConfigs: [[credentialsId: 'admingithub', url: 'git@github.com:Gemini-Solutions/jewel-dashboardmanager.git']]])
		}
	}
	stage('Maven_Build') {
		container('maven-runner-11') {
			dir('jewel-dashboardmanager') {
				sh 'rm -rf target'
				sh 'mvn package'
				dir('target'){
					sh 'chmod +x DashBoardManager*.jar'
				}
			}
		}
	}
}

node('image_builder_trivy') {
	try {
		stage('Build_image') {
			dir('jewel-dashboardmanager') {
				container('docker-image-builder-trivy') {
					withCredentials([usernamePassword(credentialsId: 'docker_registry', passwordVariable: 'docker_pass', usernameVariable: 'docker_user')]) {
						sh 'docker image build -f DockerFile-beta -t registry-np.geminisolutions.com/gemeco/jewel_dashboard_manager_beta:1.0-$BUILD_NUMBER -t registry-np.geminisolutions.com/gemeco/jewel_dashboard_manager_beta .'
  						sh 'trivy image -f json --timeout 30m --offline-scan registry-np.geminisolutions.com/gemeco/jewel_dashboard_manager_beta:1.0-$BUILD_NUMBER > trivy-report.json'
	      archiveArtifacts artifacts: 'trivy-report.json', onlyIfSuccessful: true
						sh '''docker login -u $docker_user -p $docker_pass https://registry-np.geminisolutions.com'''
						sh 'docker push registry-np.geminisolutions.com/gemeco/jewel_dashboard_manager_beta:1.0-$BUILD_NUMBER'
						sh 'docker push registry-np.geminisolutions.com/gemeco/jewel_dashboard_manager_beta'
					}
				}
			}
		}
    stage('Deployment_stage') {
               dir ('jewel-dashboardmanager') {
                   container('docker-image-builder-trivy') {
                   kubeconfig(credentialsId: 'KubeConfigCred') {
                   sh '/usr/local/bin/kubectl apply -f Deployment-beta.yaml -n dev'
                   sh '/usr/local/bin/kubectl rollout restart Deployment jeweldashboardmanager -n dev'
                   }
                   }
               }
           }

	} finally {
		//sh 'echo current_image="registry-np.geminisolutions.com/gemeco/gemecosystem_backend:1.0-$BUILD_NUMBER" > build.properties'
		//archiveArtifacts artifacts: 'build.properties', onlyIfSuccessful: true
	}
}