**목차**

- [1. 요약](#1-요약)
- [2. 현상](#2-현상)
- [3. 분석](#3-분석)
- [4. 해결](#4-해결)

**참고**

---

# 1. 요약

> kubernetes executor 를 사용하는 gitlab runner 가 Job 을 수행할 때, 제일 먼저 해당 CI git repo. 를 clone 한다. 그런데 이 때, gitlab url 에 대해 unreachable failed 에러가  발생할 수 있다. 이 경우, 많은 원인들이 있을 텐데 그 중에서도 Job 을 수행하는 Pod 가 gitlab URL 을 resolve 못하는 경우를 트러블슈팅 해보자. 내 원인은 gitlab 도메인이 사설 IP 에 대해 사설로 도메인 설정이 됐기 때문이다. 해결 방법은 gitlab runner 의 config.toml 안에 `hostaliases` 를 설정하여 Job 을 수행하는 Pod 가 gitlab 도메인 URL 을 resolve 할 수 있게 하는 것이다.

# 2. 현상

- kubernetes executor 사용하는 gitlab runner 의 job 수행 시에 아래와 같이 에러 발생.

``` bash
[0KRunning with gitlab-runner 13.12.0 (7a6612da)
[0;m[0K  on gitlab-runner-gitlab-runner-58b7b4d486-qpzht 3KymoL1X
[0;msection_start:1627214996:prepare_executor
[0K[0K[36;1mPreparing the "kubernetes" executor[0;m
[0;m[0KUsing Kubernetes namespace: cicd
[0;m[0KUsing Kubernetes executor with image docker:19.03.13 ...
[0;msection_end:1627214996:prepare_executor
[0Ksection_start:1627214996:prepare_script
[0K[0K[36;1mPreparing environment[0;m
[0;m[0;33mWARNING: Pulling GitLab Runner helper image from Docker Hub. Helper image is migrating to registry.gitlab.com, for more information see https://docs.gitlab.com/runner/configuration/advanced-configuration.html#migrate-helper-image-to-registrygitlabcom
[0;mWaiting for pod cicd/runner-3kymol1x-project-2-concurrent-04rkhp to be running, status is Pending
Waiting for pod cicd/runner-3kymol1x-project-2-concurrent-04rkhp to be running, status is Pending
	ContainersNotReady: "containers with unready status: [build helper svc-0]"
	ContainersNotReady: "containers with unready status: [build helper svc-0]"
Running on runner-3kymol1x-project-2-concurrent-04rkhp via gitlab-runner-gitlab-runner-58b7b4d486-qpzht...
section_end:1627215002:prepare_script
[0Ksection_start:1627215002:get_sources
[0K[0K[36;1mGetting source from Git repository[0;m
[0;m[32;1mFetching changes with git depth set to 50...[0;m
Initialized empty Git repository in /builds/3KymoL1X/0/root/mycicd/.git/
[32;1mCreated fresh repository.[0;m
fatal: unable to access 'https://gitlab.mylab.com/root/mycicd.git/': Failed to connect to gitlab.mylab.com port 443: Host is unreachable
section_end:1627215005:get_sources
[0Ksection_start:1627215005:cleanup_file_variables
[0K[0K[36;1mCleaning up file based variables[0;m
[0;msection_end:1627215005:cleanup_file_variables
[0K[31;1mERROR: Job failed: command terminated with exit code 1
[0;m
```

- 마지막 즈음에 보면 `fatal: unable to access [gitlab CI repo.] : Failed to connect to [gitlab url] port 443: Host is unreachable` 에러가 발생했음을 확인할 수 있다.

# 3. 분석

- 에러 내용으로 보아 gitlab URL 에 대해 접근이 불가능해보인다.
- 도메인네임 resolve 를 못하거나 방화벽 문제 혹은 SSL 인증을 못한 것으로 보인다.
- 방화벽의 경우, 파드에서 아웃바운드에 대한 networkPolicy 를 딱히 설정하진 않았다.
- 인증서 문제의 경우, 가능성은 있지만 인증서 관련 에러 로그로 보이지는 않는다.
- 그 중에서도 도메인네임 resolve 가 가장 큰 원인으로 보였고 이 원인이 맞았다.

# 4. 해결

- 내 gitlab 은 사설 IP 에 대해 도메인네임을 설정했다. 그러므로 별도 dns 설정이 없다면, job 을 실행하는 pod 에서 gitlab URL 도메인을 resolve 할 수 없다. 그래서 job 을 실행하는 pod 에서 /etc/hosts 에 도메인네임 설정 등을 하여 gitlab URL 을 resolve 할 수 있게 설정해주면 된다.
- 이런 설정은 gitlab runner 를 올릴 때 `host_aliases` 를 통해 `config.toml` 에 아래와 같이 명시할 수 있다.

``` toml
runners:
  config: |
    [[runners]]
      ...
      [runners.kubernetes]
        [[runners.kubernetes.host_aliases]]
          ip = "10.0.0.232"
          hostnames = ["gitlab.mylab.com", "minio.mylab.com", "registry.mylab.com"]
```

- 좀 더 자세히 보면, 위에 내용은 helm 으로 gitlab-runner 를 올릴 때에 override-values.yaml 이다. 즉, 해당 yaml 을 아래와 같이 runner 에 대한 helm 릴리즈에 적용하자.

``` bash
$ helm upgrade --install gitlab-runner ./gitlab-runner   -n cicd --create-namespace   -f runner-override-values.yaml
Release "gitlab-runner" has been upgraded. Happy Helming!
NAME: gitlab-runner                               
LAST DEPLOYED: Sun Jul 25 12:25:20 2021           
NAMESPACE: cicd 
STATUS: deployed
REVISION: 8     
TEST SUITE: None                               
NOTES:                                                                                                                                                                                                   
Your GitLab Runner should now be registered against the GitLab instance reachable at: "https://gitlab.mylab.com"
```

- 이제, 다시 CI 를 실행시키면 초기에 git repo. 를 잘 clone 하는 것을 확인할 수 있다.

``` bash
[0KRunning with gitlab-runner 13.12.0 (7a6612da)
[0;m[0K  on gitlab-runner-gitlab-runner-6f669d9859-8h6fb 64pTYNWR
[0;msection_start:1627216007:prepare_executor
[0K[0K[36;1mPreparing the "kubernetes" executor[0;m
[0;m[0KUsing Kubernetes namespace: cicd
[0;m[0KUsing Kubernetes executor with image docker:19.03.13 ...
[0;msection_end:1627216007:prepare_executor
[0Ksection_start:1627216007:prepare_script
[0K[0K[36;1mPreparing environment[0;m
[0;m[0;33mWARNING: Pulling GitLab Runner helper image from Docker Hub. Helper image is migrating to registry.gitlab.com, for more information see https://docs.gitlab.com/runner/configuration/advanced-configuration.html#migrate-helper-image-to-registrygitlabcom
[0;mWaiting for pod cicd/runner-64ptynwr-project-2-concurrent-055bf2 to be running, status is Pending
Waiting for pod cicd/runner-64ptynwr-project-2-concurrent-055bf2 to be running, status is Pending
	ContainersNotReady: "containers with unready status: [build helper svc-0]"
	ContainersNotReady: "containers with unready status: [build helper svc-0]"
Running on runner-64ptynwr-project-2-concurrent-055bf2 via gitlab-runner-gitlab-runner-6f669d9859-8h6fb...
section_end:1627216014:prepare_script
[0Ksection_start:1627216014:get_sources
[0K[0K[36;1mGetting source from Git repository[0;m
[0;m[32;1mFetching changes with git depth set to 50...[0;m
Initialized empty Git repository in /builds/64pTYNWR/0/root/mycicd/.git/
[32;1mCreated fresh repository.[0;m
[32;1mChecking out 6c63a28b as master...[0;m

[32;1mSkipping Git submodules setup[0;m
section_end:1627216024:get_sources
```