**목차**

- [1. 요약](#1-요약)
- [2. 전제](#2-전제)
- [3. GitHub Repo. 생성 및 clone](#3-github-repo-생성-및-clone)
- [4. helm chart 생성 및 배포 테스트](#4-helm-chart-생성-및-배포-테스트)
- [5. chart 패키징](#5-chart-패키징)
- [6. index.yaml 생성](#6-indexyaml-생성)
- [7. remote repo. 에 push](#7-remote-repo-에-push)
  - [7.1. Personal access tokens 생성](#71-personal-access-tokens-생성)
  - [7.2. push 하기](#72-push-하기)
  - [7.3. github repo. 확인](#73-github-repo-확인)
- [8. github pages 활성화](#8-github-pages-활성화)
- [9. helm 에 repo 추가](#9-helm-에-repo-추가)
- [10. 추가된 helm repo 로 chart 배포 및 테스트](#10-추가된-helm-repo-로-chart-배포-및-테스트)

---

# 1. 요약

- GitHub 을 Helm Chart Repository 로 사용하는 방법을 검토하자.
- 먼저, github 에 새로운 repo. 를 생성하고 github pages 를 활성화한다. github pages URL 이 helm repo 의 URL 로 사용된다. git push 를 위해 personal access token 도 준비하자. 그 다음, chart 를 만들어 package 로 빌드하고 index.yaml 까지 만들어서 git push 를 한다. helm repo add 를 하여 업데이트된 git repo. 를 추가하고 helm install 로 사용할 수 있게된다.

# 2. 전제

- helm v3+
- k8s 클러스터

# 3. GitHub Repo. 생성 및 clone

- 제일 먼저, Helm Chart repo. 로 사용할 GitHub repo. 를 새로 생성한다.

![](/.uploads2/2021-10-01-16-57-44.png)

- 생성한 github repo. 를 helm 명령어를 수행할 서버에서 clone 한다.

``` bash
git clone https://github.com/bellship24/test-helm-repo.git
```

# 4. helm chart 생성 및 배포 테스트

`test-helm-repo` chart 생성

``` bash
$ cd test-helm-repo
$ mkdir charts
$ helm create charts/test-helm-repo
Creating charts/test-helm-repo
```

chart 의 기존 manifests 삭제 및 수정

``` bash
rm -rf charts/test-helm-repo/templates/*
cat <<EOF > charts/test-helm-repo/templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-nginx
spec:
  selector:
    matchLabels:
      run: my-nginx
  replicas: 2
  template:
    metadata:
      labels:
        run: my-nginx
    spec:
      containers:
      - name: my-nginx
        image: nginx:latest
        # imagePullPolicy: Never #IfNotPresent #Always
        ports:
        - containerPort: 80
EOF

cat <<EOF > charts/test-helm-repo/templates/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: my-nginx
  labels:
    run: my-nginx
spec:
  type: NodePort
  ports:
  - port: 80
    protocol: TCP
  selector:
    run: my-nginx
EOF
```

배포 테스트

``` bash
$ helm install test-helm-repo charts/test-helm-repo
NAME: test-helm-repo-1633076208
LAST DEPLOYED: Fri Oct  1 17:16:49 2021
NAMESPACE: cicd
STATUS: deployed
REVISION: 1
TEST SUITE: None
```

- `--dry-run` 옵션을 줘서 실제 배포가 아닌 확인만 해도 된다.

리소스 확인 (옵션)

``` bash
$ k get po -l=run=my-nginx
NAME                       READY   STATUS    RESTARTS   AGE
my-nginx-67f6c85b8-d4xnt   1/1     Running   0          117s
my-nginx-67f6c85b8-hx2xz   1/1     Running   0          117s

$ k get svc -l=run=my-nginx
NAME       TYPE       CLUSTER-IP       EXTERNAL-IP   PORT(S)        AGE
my-nginx   NodePort   10.101.225.229   <none>        80:31256/TCP   2m1s

$ curl localhost:31256
<!DOCTYPE html>
<html>
<head>
<title>Welcome to nginx!</title>
<style>
html { color-scheme: light dark; }
body { width: 35em; margin: 0 auto;
font-family: Tahoma, Verdana, Arial, sans-serif; }
</style>
</head>
<body>
<h1>Welcome to nginx!</h1>
<p>If you see this page, the nginx web server is successfully installed and
working. Further configuration is required.</p>

<p>For online documentation and support please refer to
<a href="http://nginx.org/">nginx.org</a>.<br/>
Commercial support is available at
<a href="http://nginx.com/">nginx.com</a>.</p>

<p><em>Thank you for using nginx.</em></p>
</body>
</html>
```

리소스 삭제

``` bash
helm uninstall test-helm-repo
```

# 5. chart 패키징

``` bash
$ helm package charts/test-helm-repo
Successfully packaged chart and saved it to: test-helm-repo/test-helm-repo-0.1.0.tgz

$ ls
charts  README.md  test-helm-repo-0.1.0.tgz
```

# 6. index.yaml 생성

``` bash
$ helm repo index .
$ ls
charts  index.yaml  README.md  test-helm-repo-0.1.0.tgz

$ cat index.yaml
apiVersion: v1
entries:
  test-helm-repo:
  - apiVersion: v2
    appVersion: 1.16.0
    created: "2021-10-01T17:27:39.962244384+09:00"
    description: A Helm chart for Kubernetes
    digest: 11fa446da11213804bbe26543eb20c7bc558e631338823ca2a1755e3ecaabdd9
    name: test-helm-repo
    type: application
    urls:
    - test-helm-repo-0.1.0.tgz
    version: 0.1.0
generated: "2021-10-01T17:27:39.961224806+09:00"
```

# 7. remote repo. 에 push

## 7.1. Personal access tokens 생성

![](/.uploads2/2021-10-01-17-34-30.png)

- `github.com` 접근 -> `유저 아이콘 클릭` -> `Settings` 클릭 -> `Developer settings` 클릭 -> `Personal access tokens` 클릭 -> `Generate new token` 클릭 -> `Note(토큰 이름)` 입력 -> `Expiration` 설정 -> `Select scopes` 에서 `repo` 활성화 체크 -> `Generate token` 클릭 -> 생성된 토큰을 잘 복사해놓기

## 7.2. push 하기

``` bash
git config user.name "<이름>"
git config user.email "<이메일>@<도메인>"
git add .
git commit -am "Add charts pkg and index.yaml"
git push
```

- git push 할 때, 유저네임을 잘 입력하고 패스워드에는 생성한 Personal access token 을 붙여넣어준다.

## 7.3. github repo. 확인

![](/.uploads2/2021-10-01-17-37-24.png)

- 업데이트가 잘 된 것을 확인할 수 있다.

# 8. github pages 활성화

- 해당 repo. 의 `Settings` 클릭 -> `Pages` 클릭 -> `Source` 에서 `main` 브랜치를 선택하고 `Save` 클릭

![](/.uploads2/2021-10-01-17-39-41.png)

- 이제, 주어진 경로로 helm repo URL 를 쓸 수 있다.

# 9. helm 에 repo 추가

``` bash
$ helm repo add test-helm-repo https://bellship24.github.io/test-helm-repo/
"test-helm-repo" has been added to your repositories
```

# 10. 추가된 helm repo 로 chart 배포 및 테스트

설치 가능한 chart 확인

``` bash
$ helm search repo test-helm-repo
NAME                         	CHART VERSION	APP VERSION	DESCRIPTION
test-helm-repo/test-helm-repo	0.1.0        	1.16.0     	A Helm chart for Kubernetes
```

chart 설치

``` bash
$ helm install test-helm-repo test-helm-repo/test-helm-repo
NAME: test-helm-repo
LAST DEPLOYED: Fri Oct  1 17:42:26 2021
NAMESPACE: cicd
STATUS: deployed
REVISION: 1
TEST SUITE: None
```

리소스 확인

``` bash
$ k get po -l=run=my-nginx
NAME                       READY   STATUS    RESTARTS   AGE
my-nginx-67f6c85b8-9vz9k   1/1     Running   0          31s
my-nginx-67f6c85b8-njrnr   1/1     Running   0          31s


$ k get svc -l=run=my-nginx
NAME       TYPE       CLUSTER-IP      EXTERNAL-IP   PORT(S)        AGE
my-nginx   NodePort   10.107.229.56   <none>        80:31162/TCP   34s


$ curl localhost:31162
<!DOCTYPE html>
<html>
<head>
<title>Welcome to nginx!</title>
<style>
html { color-scheme: light dark; }
body { width: 35em; margin: 0 auto;
font-family: Tahoma, Verdana, Arial, sans-serif; }
</style>
</head>
<body>
<h1>Welcome to nginx!</h1>
<p>If you see this page, the nginx web server is successfully installed and
working. Further configuration is required.</p>

<p>For online documentation and support please refer to
<a href="http://nginx.org/">nginx.org</a>.<br/>
Commercial support is available at
<a href="http://nginx.com/">nginx.com</a>.</p>

<p><em>Thank you for using nginx.</em></p>
</body>
</html>
``

리소스 삭제

``` bash
$ helm uninstall test-helm-repo
release "test-helm-repo" uninstalled
```