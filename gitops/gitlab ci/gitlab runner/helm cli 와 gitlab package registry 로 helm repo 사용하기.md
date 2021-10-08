**목차**

- [1. 요약](#1-요약)
- [2. 차트 생성 및 압축](#2-차트-생성-및-압축)
- [3. 차트 업로드](#3-차트-업로드)
- [4. 차트 repo 추가](#4-차트-repo-추가)
- [5. 추가된 chart 설치](#5-추가된-chart-설치)

**참고**

- [[gitlab docs] Helm charts in the Package Registry](https://docs.gitlab.com/ee/user/packages/helm_repository/)

---

# 1. 요약

- helm cli 와 gitlab 의 package registry 를 사용해서 helm chart 에 대한 repository 를 구축하여 push, pull 해보자.
- 먼저, helm chart 를 생성하고 packaging 한다.
- curl 을 통해, gitlab api 를 활용하여 패키징한 차트를 gitlab package registry 에 push 할 수 있다.
- 이렇게 레지스트리에 올라간 helm chart 또한 curl 을 통해, gitlab api 를 활용하여  helm repo 를 추가하여 helm install 을 사용해 release 를 배포할 수 있다.

# 2. 차트 생성 및 압축

``` bash
helm chart create helloworld
```

``` bash
$ tree helloworld
helloworld
├── charts
├── Chart.yaml
├── templates
│   ├── deployment.yaml
│   ├── _helpers.tpl
│   ├── hpa.yaml
│   ├── ingress.yaml
│   ├── NOTES.txt
│   ├── serviceaccount.yaml
│   ├── service.yaml
│   └── tests
│       └── test-connection.yaml
└── values.yaml

3 directories, 10 files
```

``` bash
$ helm package helloworld
$ ls helloworld-0.1.0.tgz
helloworld-0.1.0.tgz
```

# 3. 차트 업로드

엑세스 토큰 생성

![](/.uploads2/2021-10-08-21-03-54.png)

- 유저 아이콘 클릭 -> user settings -> access tokens
- `api`, `read_api` 권한을 포함한 엑세스 토큰 생성

기본 명령어

``` bash
curl --request POST \
     --form 'chart=@mychart-0.1.0.tgz' \
     --user <username>:<access_token> \
     https://gitlab.example.com/api/v4/projects/<project_id>/packages/helm/api/<channel>/charts
```

e.g.

``` bash
$ curl --request POST \
     --form 'chart=@helloworld-0.1.0.tgz' \
     --user root:diurKmPBLZQzRhTbEp8K \
     https://gitlab.bellship.com/api/v4/projects/2/packages/helm/api/stable/charts

{"message":"201 Created"}
```

업로드 확인

![](/.uploads2/2021-10-08-21-03-20.png)

# 4. 차트 repo 추가

기본 명령어

``` bash
helm repo add --username <username> --password <access_token> project-1 https://gitlab.example.com/api/v4/projects/<project_id>/packages/helm/<channel>
helm install my-release project-1/mychart
```

e.g.

``` bash
helm repo add test \
  --username root \
  --password diurKmPBLZQzRhTbEp8K \
  https://gitlab.bellship.com/api/v4/projects/2/packages/helm/stable
```

repo 추가 확인

``` bash
$ helm repo ls | grep test
test                            https://gitlab.bellship.com/api/v4/projects/2/packages/helm/stable
```

# 5. 추가된 chart 설치

``` bash
$ helm install test-my-release test/helloworld                                                                                     NAME: test-my-release
LAST DEPLOYED: Fri Oct  8 21:14:33 2021                                                                                                                           NAMESPACE: cicd
STATUS: deployed                                                                                                                                                  REVISION: 1
NOTES:                                                                                                                                                            1. Get the application URL by running these commands:
  export POD_NAME=$(kubectl get pods --namespace cicd -l "app.kubernetes.io/name=helloworld,app.kubernetes.io/instance=test-my-release" -o jsonpath="{.items[0].metadata.name}")
  export CONTAINER_PORT=$(kubectl get pod --namespace cicd $POD_NAME -o jsonpath="{.spec.containers[0].ports[0].containerPort}")                                    echo "Visit http://127.0.0.1:8080 to use your application"
  kubectl --namespace cicd port-forward $POD_NAME 8080:$CONTAINER_PORT
```

설치 확인

``` bash
$ k get po -l app.kubernetes.io/instance=test-my-release
NAME                                          READY   STATUS              RESTARTS   AGE
test-my-release-helloworld-589d776d6c-snnvm   0/1     ContainerCreating   0          39s
```