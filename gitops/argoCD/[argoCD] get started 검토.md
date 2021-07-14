**요약**

> Argo CD docs 의 get started 를 검토, 검증해보자.
> - Argo CD 서버와 CLI 설치
> - 서비스 expose
> - 외부 클러스터 등록 (optional)
> - 앱 생성과 Sync

**목차**

- [1. 요구 사항](#1-요구-사항)
- [2. Argo CD 설치](#2-argo-cd-설치)
- [3. Argo CD CLI 설치](#3-argo-cd-cli-설치)
- [4. Argo CD API 서버에 접근 (expose)](#4-argo-cd-api-서버에-접근-expose)
  - [4.1. `LoadBalancer` svc type](#41-loadbalancer-svc-type)
  - [4.2. Ingress](#42-ingress)
  - [4.3. Port Forwarding](#43-port-forwarding)
- [5. CLI 로 로그인](#5-cli-로-로그인)
- [6. 앱을 배포할 클러스터 등록 (optional)](#6-앱을-배포할-클러스터-등록-optional)
- [7. git repo. 로부터 앱 생성](#7-git-repo-로부터-앱-생성)
  - [7.1. CLI 로 앱 생성](#71-cli-로-앱-생성)
  - [7.2. UI 로 앱 생성](#72-ui-로-앱-생성)
- [8. 앱 Sync(배포)](#8-앱-sync배포)
  - [8.1. CLI 로 Sync](#81-cli-로-sync)
  - [8.2. UI 로 Sync](#82-ui-로-sync)

**참고**

- [[Argo CD docs] Getting Started](https://argoproj.github.io/argo-cd/getting_started/)

---

# 1. 요구 사항

- kubectl
- kubeconfig

# 2. Argo CD 설치

리소스 생성

``` bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

생성 확인

``` bash
$ k get all -n argocd
NAME                                      READY   STATUS    RESTARTS   AGE
pod/argocd-application-controller-0       1/1     Running   0          3m10s
pod/argocd-dex-server-7946bfbf79-whlxr    1/1     Running   0          3m11s
pod/argocd-redis-7547547c4f-gznxb         1/1     Running   0          3m11s
pod/argocd-repo-server-6b5cf77fbc-w8xcm   1/1     Running   0          3m11s
pod/argocd-server-86f7f94488-5788v        1/1     Running   0          3m11s

NAME                            TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)                      AGE
service/argocd-dex-server       ClusterIP   10.101.112.228   <none>        5556/TCP,5557/TCP,5558/TCP   3m13s
service/argocd-metrics          ClusterIP   10.111.71.181    <none>        8082/TCP                     3m13s
service/argocd-redis            ClusterIP   10.100.251.97    <none>        6379/TCP                     3m13s
service/argocd-repo-server      ClusterIP   10.101.232.120   <none>        8081/TCP,8084/TCP            3m12s
service/argocd-server           ClusterIP   10.108.222.46    <none>        80/TCP,443/TCP               3m12s
service/argocd-server-metrics   ClusterIP   10.106.56.170    <none>        8083/TCP                     3m12s

NAME                                 READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/argocd-dex-server    1/1     1            1           3m12s
deployment.apps/argocd-redis         1/1     1            1           3m12s
deployment.apps/argocd-repo-server   1/1     1            1           3m12s
deployment.apps/argocd-server        1/1     1            1           3m12s

NAME                                            DESIRED   CURRENT   READY   AGE
replicaset.apps/argocd-dex-server-7946bfbf79    1         1         1       3m12s
replicaset.apps/argocd-redis-7547547c4f         1         1         1       3m12s
replicaset.apps/argocd-repo-server-6b5cf77fbc   1         1         1       3m12s
replicaset.apps/argocd-server-86f7f94488        1         1         1       3m12s

NAME                                             READY   AGE
statefulset.apps/argocd-application-controller   1/1     3m12s
```

# 3. Argo CD CLI 설치

아래 내용은 Linux 기준.

다른 OS 는, [[ArgoCD docs] Installation](https://argoproj.github.io/argo-cd/cli_installation/) 을 참고하자.

최신 버전 설치

``` bash
sudo curl -sSL -o /usr/local/bin/argocd https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64
sudo chmod +x /usr/local/bin/argocd
```

특정 버전 설치

``` bash
VERSION=<TAG> # Select desired TAG from https://github.com/argoproj/argo-cd/releases
sudo curl -sSL -o /usr/local/bin/argocd https://github.com/argoproj/argo-cd/releases/download/$VERSION/argocd-linux-amd64
sudo chmod +x /usr/local/bin/argocd
```

설치 확인

``` bash
$ argocd
argocd controls a Argo CD server

Usage:
  argocd [flags]
  argocd [command]

Available Commands:
  account     Manage account settings
  app         Manage applications
  cert        Manage repository certificates and SSH known hosts entries
  cluster     Manage cluster credentials
  completion  output shell completion code for the specified shell (bash or zsh)
  context     Switch between contexts
  gpg         Manage GPG keys used for signature verification
  help        Help about any command
  login       Log in to Argo CD
  logout      Log out from Argo CD
  proj        Manage projects
  relogin     Refresh an expired authenticate token
  repo        Manage repository connection parameters
  repocreds   Manage repository connection parameters
  version     Print version information

Flags:
      --auth-token string               Authentication token
      --client-crt string               Client certificate file
      --client-crt-key string           Client certificate key file
      --config string                   Path to Argo CD config (default "/home/myai/.argocd/config")
      --grpc-web                        Enables gRPC-web protocol. Useful if Argo CD server is behind proxy which does not support HTTP2.
      --grpc-web-root-path string       Enables gRPC-web protocol. Useful if Argo CD server is behind proxy which does not support HTTP2. Set web root.
  -H, --header strings                  Sets additional header to all requests made by Argo CD CLI. (Can be repeated multiple times to add multiple headers, also supports comma separated headers)
  -h, --help                            help for argocd
      --insecure                        Skip server certificate and domain verification
      --logformat string                Set the logging format. One of: text|json (default "text")
      --loglevel string                 Set the logging level. One of: debug|info|warn|error (default "info")
      --plaintext                       Disable TLS
      --port-forward                    Connect to a random argocd-server port using port forwarding
      --port-forward-namespace string   Namespace name which should be used for port forwarding
      --server string                   Argo CD server address
      --server-crt string               Server certificate file

Use "argocd [command] --help" for more information about a command.
```

# 4. Argo CD API 서버에 접근 (expose)

기본적으로 Argo CD API 서버는 external IP 로 노출되지 않음.
API 서버에 접근하려면, 아래에 노출시킬 방법들 중 선택.

1. `LoadBalancer` svc type
2. Ingress
3. Port Forwarding

## 4.1. `LoadBalancer` svc type

argocd-server 의 svc type 을 `LoadBalancer` 로 변경.

``` bash
kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "LoadBalancer"}}'
```

endpoint 확인

``` bash
$ k get svc argocd-server -n argocd
NAME                    TYPE           CLUSTER-IP       EXTERNAL-IP      PORT(S)                      AGE
argocd-server           LoadBalancer   10.108.222.46    10.0.0.217   80:32160/TCP,443:32007/TCP   62m
```

웹 UI 접근 확인

![](/.uploads/2021-07-12-02-22-34.png)

## 4.2. Ingress

Argo CD 에 대해 Ingress 설정을 하려면 여기 [[Argo CD docs] ingress documentation](https://argoproj.github.io/argo-cd/operator-manual/ingress/) 을 참고하자.

Argo CD 는 Argo CLI 를 위해 gRPC 서버를, UI 를 위해 HTTP/HTTPS 서버를 실행한다.
두 프로토콜 모두 argocd-server svc 객체에 의해 expose 된다.
포트는 아래와 같다.

- 443 : gRPC / HTTPS
- 80 : HTTP (redirects to HTTPS)

이러한 Argo CD 의 svc 를 여러 ingress-controller 를 통해 expose 할 수 있는데, 그 중에서 nginx-ingress 를 통한 방법을 검토하자.

앞서 봤듯이 Argo CD 는 443 포트에 gRPC 와 HTTPS 를 모두 serve 한다. gRPC 나 HTTPS 같은 백엔드 프로토콜에 대한 단일 값만 허용하는 `nginx.ingress.kubernetes.io/backend-protocol` annotation 으로 인해, argocd-service 에 대한 단일 nginx ingress object 와 rule 을 정의하는 것은 쉽지 않다.

Argo CD API 서버를 단일 ingress rule 과 hostname 으로 노출시키려면, `nginx.ingress.kubernetes.io/ssl-passthrough` annotation 을 써서 Argo CD API 서버에 대한 TLS termination 과 TLS connection passthorugh 를 사용해야 한다.

``` yaml
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: argocd-server-ingress
  namespace: argocd
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    nginx.ingress.kubernetes.io/ssl-passthrough: "true"
spec:
  rules:
  - host: argocd.example.com
    http:
      paths:
      - backend:
          serviceName: argocd-server
          servicePort: https
```

위에 rule 은 TLS termination 을 하는데, 사용되는 프로토콜을 감지하고 적절히 응답하는 역할을 한다.
`nginx.ingress.kubernetes.io/ssl-passthrough` annotation 을 사용하려면 `--enable-ssl-passthrough` 플래그를 `nginx-ingress-controller` 에 대한 커멘드라인 인수를 추가해야 한다.

cert-manager 와 Let's Encrypt 를 사용하는 SSL-Passthorugh 의 경우

``` yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: argocd-server-ingress
  namespace: argocd
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    kubernetes.io/ingress.class: nginx
    kubernetes.io/tls-acme: "true"
    nginx.ingress.kubernetes.io/ssl-passthrough: "true"
    # If you encounter a redirect loop or are getting a 307 response code 
    # then you need to force the nginx ingress to connect to the backend using HTTPS.
    #
    nginx.ingress.kubernetes.io/backend-protocol: "HTTPS"
spec:
  rules:
  - host: argocd.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service: 
            name: argocd-server
            port:
              name: https
  tls:
  - hosts:
    - argocd.example.com
    secretName: argocd-secret # do not change, this is provided by Argo CD
```

## 4.3. Port Forwarding

kubectl port-forwading 을 사용하면 svc 노출 없이 API 서버에 접근할 수 있다.

``` bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

# 5. CLI 로 로그인

admin 계정에 대한 초기 패스워드는 자동 생성되며 앞서 argocd 를 설치한 namespace 에 `argocd-initial-admin-secret` 이라는 이름의 secret 으로 `password` 라는 필드 안에 텍스트로 저장되어 있다.

``` bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d && echo
```

초기 패스워드를 통해 초기 로그인을 한 뒤에, 패스워드를 바꾸고 `argocd-initial-admin-secret` 을 지우길 권고된다. 물론 추후에 다시 만들 수도 있다.

cli 로 argocd 에 로그인하기

``` bash
argocd login <ARGOCD_SERVER>
```

e.g.

``` bash
$ argocd login 10.0.0.217
WARNING: server certificate had error: x509: cannot validate certificate for 10.0.0.217 because it doesn't contain any IP SANs. Proceed insecurely (y/n)? y
Username: admin
Password: 
'admin:login' logged in successfully
Context '10.0.0.217' updated
```

비밀번호 변경하기

``` bash
argocd account update-password
```

e.g.

``` bash
$ argocd account update-password
*** Enter current password: 
*** Enter new password: 
*** Confirm new password: 
Password updated
Context '10.0.0.217' updated
```

# 6. 앱을 배포할 클러스터 등록 (optional)

외부 클러스터에 앱을 배포할 때는, argocd 에 해당 cluster 의 credentials 을 등록해야 한다.
argocd 가 구동 중인 내부 클러스터에 앱을 배포할 때는 `https://kubernetes.default.svc` 가 앱의 k8s api server address 로 사용된다.

만약, 외부 클러스터를 등록하려면 [[Argo CD Docs] Register A Cluster To Deploy Apps To (Optional)](https://argoproj.github.io/argo-cd/getting_started/#5-register-a-cluster-to-deploy-apps-to-optional) 을 참고하자.

# 7. git repo. 로부터 앱 생성

argocd 가 동작하는 방법에 대해 검증하기 위해 [[GitHub] guestbook application](https://argoproj.github.io/argo-cd/getting_started/#5-register-a-cluster-to-deploy-apps-to-optional) 을 사용할 것이다.

## 7.1. CLI 로 앱 생성

``` bash
argocd app create guestbook \
 --repo https://github.com/argoproj/argocd-example-apps.git
 --path guestbook \
 --dest-sever https://kubernetes.default.svc \
 --dest-namespace default
```

만약, argocd 를 포트포워딩해서 사용한다면, 모든 명령어에 `--port-forward-namespace argocd` 옵션을 추가하거나 환경 변수를 다음과 같이 만들자.
`export ARGOCD_OPTS='--port-forward-namespace argocd'`

## 7.2. UI 로 앱 생성

argocd 웹 UI 로 접근 -> 로그인 -> `+ NEW APP` 클릭 -> 아래 내용 작성 -> `Create` 클릭

![helm-guestbook](/.uploads/2021-07-12-22-48-41.png)

![guestbook](/.uploads/2021-07-12-22-51-10.png)

# 8. 앱 Sync(배포)

## 8.1. CLI 로 Sync

`guestbook` 앱이 생성되고나면, 상태를 볼 수 있다.

``` bash
argocd app get guestbook
```

e.g.

``` bash
$ argocd app get guestbook
Name:               guestbook
Project:            default
Server:             https://kubernetes.default.svc
Namespace:          default
URL:                https://10.0.0.217/applications/guestbook
Repo:               https://github.com/argoproj/argocd-example-apps.git
Target:             HEAD
Path:               guestbook
SyncWindow:         Sync Allowed
Sync Policy:        <none>
Sync Status:        OutOfSync from HEAD (53e28ff)
Health Status:      Missing

GROUP  KIND        NAMESPACE  NAME          STATUS     HEALTH   HOOK  MESSAGE
       Service     default    guestbook-ui  OutOfSync  Missing        
apps   Deployment  default    guestbook-ui  OutOfSync  Missing   
```

앱이 아직 deploy 되지 않았고 Kubernetes 리소스가 생성되지 않았기 때문에 앱의 상태는 초기에 `OutOfSync` 상태이다. 앱을 Sync(deploy) 하려면 다음을 실행하면 된다.

``` bash
argocd app sync guestbook
```

이 명령어는 repo. 에서 매니페스트를 검색하고 kubectl 적용을 수행한다. 이제 guestbook 앱이 실행 될 것이고 해당 리소스 구성 요소, 로그, 이벤트, 상태를 볼 수 있다.

## 8.2. UI 로 Sync

guestbook 앱의 `SYNC` -> `SYNCHRONIZE` 클릭

![](/.uploads/2021-07-12-23-01-58.png)
![](/.uploads/2021-07-12-23-02-55.png)
![](/.uploads/2021-07-12-23-52-51.png)

CLI 에서 확인

``` bash
myai@mylab-dev-k8s-pjb-1:~$ k get all
NAME                                READY   STATUS    RESTARTS   AGE
pod/guestbook-ui-85985d774c-lqbpb   1/1     Running   0          89s

NAME                   TYPE        CLUSTER-IP    EXTERNAL-IP   PORT(S)   AGE
service/guestbook-ui   ClusterIP   10.97.1.216   <none>        80/TCP    90s
service/kubernetes     ClusterIP   10.96.0.1     <none>        443/TCP   25d

NAME                           READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/guestbook-ui   1/1     1            1           89s

NAME                                      DESIRED   CURRENT   READY   AGE
replicaset.apps/guestbook-ui-85985d774c   1         1         1       89s
myai@mylab-dev-k8s-pjb-1:~$ 
myai@mylab-dev-k8s-pjb-1:~$ 
myai@mylab-dev-k8s-pjb-1:~$ k exec -ti pod/guestbook-ui-85985d774c-lqbpb -- curl 10.97.1.216
<html ng-app="redis">
  <head>
    <title>Guestbook</title>
    <link rel="stylesheet" href="//netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css">
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.2.12/angular.min.js"></script>
    <script src="controllers.js"></script>
    <script src="fancy.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/angular-ui-bootstrap/0.13.0/ui-bootstrap-tpls.js"></script>
  </head>
  <body ng-controller="RedisCtrl">
    <div style="margin-left:20px;">
      <div class="row" style="width: 50%;">
        <div class="col-sm-6">
          <h2 fancy>Fancy Guestbook</h2>
        </div>
        <fieldset class="col-sm-6" style="margin-top:15px">
          <div class="col-sm-8">
            <input ng-model="query" placeholder="Query here" class="form-control" type="text" name="input"><br>
          </div>
          <div class="col-sm-4">
            <button type="button" class="btn btn-primary" ng-click="controller.onSearch()">Search</button>
          </div>
        </fieldset>
      </div>
      <div ng-show="showMain" class="main-ui col-sm-6">
        <form>
        <fieldset>
        <input ng-model="msg" placeholder="Messages" class="form-control" type="text" name="input"><br>
        <button type="button" class="btn btn-primary" ng-click="controller.onRedis()">Submit</button>
        </fieldset>
        </form>
        <div>
          <div ng-repeat="msg in messages track by $index">
            {{msg}}
          </div>
        </div>
      </div>
      <div ng-hide="showMain" class="search-results row">
        HIHIHIHI
      </div>
    </div>
  </body>
</html>
```

UI 에서 확인

- UI 에서 확인하기 위해서 NodePort 를 사용했다. 여기서 배포된 매니페스트를 아래와 같이 argoCD 에서 수정했다.

![](/.uploads/2021-07-12-23-54-03.png)

- 아래와 같이 배포한 웹에 접근할 수 있다.

![](/.uploads/2021-07-12-23-55-51.png)