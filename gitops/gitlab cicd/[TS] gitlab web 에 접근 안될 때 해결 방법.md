**목차**

- [1. 현상](#1-현상)
- [2. 분석](#2-분석)
- [3. 해결](#3-해결)
  - [3.1. 80,443 이 열려있는 내부 IP 찾기](#31-80443-이-열려있는-내부-ip-찾기)
  - [3.2. metlabLB 의 VIP range 설정 변경](#32-metlablb-의-vip-range-설정-변경)
    - [3.2.1. helm 으로 metallb 설치](#321-helm-으로-metallb-설치)
  - [3.3. LoadBalancer 타입 SVC 테스트](#33-loadbalancer-타입-svc-테스트)
  - [4. ingress-nginx-controller 의 external IP 를 변경한 VIP 로 수정](#4-ingress-nginx-controller-의-external-ip-를-변경한-vip-로-수정)
  - [5. gitlab helm release 의 override-values.yaml 을 upgrade](#5-gitlab-helm-release-의-override-valuesyaml-을-upgrade)

**요약**

> 잘 접근되던 k8s 위에 구축한 gitlab 웹 페이지에 접근이 안 된다. docker 재시작 이후에 발생한 이슈로 k8s 구성과 관련 있을 것 같다. 이와 관련된 트러블슈팅을 정리해보았다. 결국 metallb 와 nginx-ingress 를 재배포하고 gitlab helm values 를 일부 수정하여 upgrade 함으로써 해결했다.

**참고**

---

# 1. 현상

기존에 잘 접근되던 k8s 위에 helm 으로 구축한 gitlab 웹 페이지에 접근이 안 된다.

![](/.uploads/2021-07-15-00-52-41.png)

# 2. 분석

배포한 gitlab helm release 의 리소스 확인

``` bash
 k get all -n cicd
NAME                                              READY   STATUS      RESTARTS   AGE
pod/gitlab-gitaly-0                               1/1     Running     4          26d
pod/gitlab-gitlab-exporter-6f645c49c-jb4r9        1/1     Running     1          26d
pod/gitlab-gitlab-shell-58cd975b57-drlxq          1/1     Running     2          26d
pod/gitlab-gitlab-shell-58cd975b57-pnqjb          1/1     Running     1          26d
pod/gitlab-migrations-4-v8m22                     0/1     Completed   0          25d
pod/gitlab-minio-5557bb8cfd-vbp7r                 1/1     Running     5          26d
pod/gitlab-minio-create-buckets-4-c6gpc           0/1     Completed   0          25d
pod/gitlab-postgresql-0                           2/2     Running     2          26d
pod/gitlab-prometheus-server-6444c7bd76-gs7k6     2/2     Running     4          26d
pod/gitlab-redis-master-0                         2/2     Running     4          26d
pod/gitlab-registry-6b454b5668-2w24z              1/1     Running     3          26d
pod/gitlab-registry-6b454b5668-hlcc9              1/1     Running     1          26d
pod/gitlab-runner-gitlab-runner-f9c57b49f-zm4dq   1/1     Running     3          15d
pod/gitlab-sidekiq-all-in-1-v1-85fffc9595-rrl8z   1/1     Running     1          26d
pod/gitlab-task-runner-556dc767c7-dccsl           1/1     Running     1          26d
pod/gitlab-webservice-default-bb66f6b6d-bdztv     2/2     Running     2          26d
pod/gitlab-webservice-default-bb66f6b6d-phjtk     2/2     Running     3          26d

NAME                                 TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)             AGE
service/gitlab-gitaly                ClusterIP   None             <none>        8075/TCP            26d
service/gitlab-gitlab-exporter       ClusterIP   10.107.205.187   <none>        9168/TCP            26d
service/gitlab-gitlab-shell          ClusterIP   10.109.176.181   <none>        22/TCP              26d
service/gitlab-minio-svc             ClusterIP   10.105.5.86      <none>        9000/TCP            26d
service/gitlab-postgresql            ClusterIP   10.100.173.37    <none>        5432/TCP            26d
service/gitlab-postgresql-headless   ClusterIP   None             <none>        5432/TCP            26d
service/gitlab-postgresql-metrics    ClusterIP   10.111.196.4     <none>        9187/TCP            26d
service/gitlab-prometheus-server     ClusterIP   10.111.209.226   <none>        80/TCP              26d
service/gitlab-redis-headless        ClusterIP   None             <none>        6379/TCP            26d
service/gitlab-redis-master          ClusterIP   10.98.192.27     <none>        6379/TCP            26d
service/gitlab-redis-metrics         ClusterIP   10.100.8.78      <none>        9121/TCP            26d
service/gitlab-registry              ClusterIP   10.108.238.232   <none>        5000/TCP            26d
service/gitlab-webservice-default    ClusterIP   10.96.79.150     <none>        8080/TCP,8181/TCP   26d

NAME                                          READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/gitlab-gitlab-exporter        1/1     1            1           26d
deployment.apps/gitlab-gitlab-shell           2/2     2            2           26d
deployment.apps/gitlab-minio                  1/1     1            1           26d
deployment.apps/gitlab-prometheus-server      1/1     1            1           26d
deployment.apps/gitlab-registry               2/2     2            2           26d
deployment.apps/gitlab-runner-gitlab-runner   1/1     1            1           24d
deployment.apps/gitlab-sidekiq-all-in-1-v1    1/1     1            1           26d
deployment.apps/gitlab-task-runner            1/1     1            1           26d
deployment.apps/gitlab-webservice-default     2/2     2            2           26d

NAME                                                     DESIRED   CURRENT   READY   AGE
replicaset.apps/gitlab-gitlab-exporter-6f645c49c         1         1         1       26d
replicaset.apps/gitlab-gitlab-shell-58cd975b57           2         2         2       26d
replicaset.apps/gitlab-minio-5557bb8cfd                  1         1         1       26d
replicaset.apps/gitlab-prometheus-server-6444c7bd76      1         1         1       26d
replicaset.apps/gitlab-registry-6b454b5668               2         2         2       26d
replicaset.apps/gitlab-runner-gitlab-runner-568954c478   0         0         0       24d
replicaset.apps/gitlab-runner-gitlab-runner-6854dbc547   0         0         0       24d
replicaset.apps/gitlab-runner-gitlab-runner-7fcb546446   0         0         0       23d
replicaset.apps/gitlab-runner-gitlab-runner-85c667868b   0         0         0       15d
replicaset.apps/gitlab-runner-gitlab-runner-86b9898cb4   0         0         0       24d
replicaset.apps/gitlab-runner-gitlab-runner-f9c57b49f    1         1         1       15d
replicaset.apps/gitlab-sidekiq-all-in-1-v1-85fffc9595    1         1         1       26d
replicaset.apps/gitlab-task-runner-556dc767c7            1         1         1       26d
replicaset.apps/gitlab-webservice-default-bb66f6b6d      2         2         2       26d

NAME                                   READY   AGE
statefulset.apps/gitlab-gitaly         1/1     26d
statefulset.apps/gitlab-postgresql     1/1     26d
statefulset.apps/gitlab-redis-master   1/1     26d

NAME                                                             REFERENCE                               TARGETS          MINPODS   MAXPODS   REPLICAS   AGE
horizontalpodautoscaler.autoscaling/gitlab-gitlab-shell          Deployment/gitlab-gitlab-shell          <unknown>/100m   2         10        2          26d
horizontalpodautoscaler.autoscaling/gitlab-registry              Deployment/gitlab-registry              <unknown>/75%    2         10        2          26d
horizontalpodautoscaler.autoscaling/gitlab-sidekiq-all-in-1-v1   Deployment/gitlab-sidekiq-all-in-1-v1   <unknown>/350m   1         10        1          26d
horizontalpodautoscaler.autoscaling/gitlab-webservice-default    Deployment/gitlab-webservice-default    <unknown>/1      2         10        2          26d

NAME                                      COMPLETIONS   DURATION   AGE
job.batch/gitlab-issuer-4                 0/1           25d        25d
job.batch/gitlab-migrations-4             1/1           60s        25d
job.batch/gitlab-minio-create-buckets-4   1/1           2s         25d

$ k get ing -n cicd
NAME                        CLASS    HOSTS                ADDRESS          PORTS     AGE
gitlab-minio                <none>   minio.ailab.com      10.0.0.216   80, 443   26d
gitlab-registry             <none>   registry.ailab.com   10.0.0.216   80, 443   26d
gitlab-webservice-default   <none>   gitlab.ailab.com     10.0.0.216   80, 443   26d
```

- 기본적으로 모든 파드들이 Running 중이다.

pc 에서 접근이 안됐던 것으로 사내망 방화벽 문제일 가능성이 있다. 그러므로 노드 local 에서 접근을 시도해보자.

``` bash
### /etc/hosts 추가할 것을 주의
$ nc -v gitlab.ailab.com 443
Connection to gitlab.ailab.com 443 port [tcp/https] succeeded!

$ curl -k https://gitlab.ailab.com && echo
<html><body>You are being <a href="https://gitlab.ailab.com/users/sign_in">redirected</a>.</body></html>
```

- 노드 local 에서는 접근이 잘 된다.

이제, 앞서 점검과 동일하게 pc 환경에서 gitlab 엔드포인트로 네트워크 점검을 해보자.

``` bash
$ nc -v gitlab.ailab.com 443    
nc: connectx to gitlab.ailab.com port 443 (tcp) failed: Operation timed out

$ curl -k https://gitlab.ailab.com    
curl: (7) Failed to connect to gitlab.ailab.com port 443: Operation timed out
```

- 접근이 안 되고 방화벽에 막힌 것으로 보인다.

# 3. 해결

분석에 따라 방화벽 문제가 유력하다. 아무래도 사내 VPN 이슈가 생긴 것 같다. 아무튼 문제를 해결해야 한다. 사내 VPN 정책은 결재도 해야하고 언제 해결될지 모른다. 빠른 이슈해결을 위해 ingress endpoint 를 방화벽 오픈되어 있을 가능성이 높은 IP 로 변경해보자. 하지만, 내 환경은 온프레미스이다. 그래서 고려해야할 사항들이 꽤 많다. 우선, 로드밸런서용으로 nginx-ingress controller 를 사용한다. 로드밸런서는 metalLB 로 구축했다.

즉, 작업 순서는 대략 아래와 같다.

1. 80,443 이 열려있는 내부 IP 찾기.
2. metlabLB 의 VIP range 설정 변경.
3. ingress-nginx-controller 의 external IP 를 변경한 VIP 로 수정.
4. gitlab helm release 의 override-values.yaml 을 upgrade.

## 3.1. 80,443 이 열려있는 내부 IP 찾기

10.0.0.[232, 249]

## 3.2. metlabLB 의 VIP range 설정 변경

### 3.2.1. helm 으로 metallb 설치

metallb 의 helm repo 추가

``` bash
helm repo add metallb https://metallb.github.io/metallb
```

helm chart 다운로드

``` bash
helm fetch metallb/metallb
tar -xf metallb-0.10.2.tgz
cd metallb
```

`values.yaml` 수정

``` bash
configInline: 
  address-pools:
  - name: default
    protocol: layer2
    addresses:
    - 10.0.0.232/32
    - 10.0.0.249/32
```

release 배포

``` bash
helm upgrade --install metallb metallb/metallb -f values.yaml -n metallb --create-namespace
```

리소스 배포 확인

``` bash
$ k get all -n metallb
NAME                                      READY   STATUS    RESTARTS   AGE
pod/metallb-controller-748756655f-n7hxn   1/1     Running   0          35s
pod/metallb-speaker-cz47m                 1/1     Running   0          35s
pod/metallb-speaker-qjfjr                 1/1     Running   0          35s
pod/metallb-speaker-zgmj4                 1/1     Running   0          35s
pod/metallb-speaker-zvvhf                 1/1     Running   0          35s

NAME                             DESIRED   CURRENT   READY   UP-TO-DATE   AVAILABLE   NODE SELECTOR            AGE
daemonset.apps/metallb-speaker   4         4         4       4            4           kubernetes.io/os=linux   35s

NAME                                 READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/metallb-controller   1/1     1            1           35s

NAME                                            DESIRED   CURRENT   READY   AGE
replicaset.apps/metallb-controller-748756655f   1         1         1       35s
```

## 3.3. LoadBalancer 타입 SVC 테스트

``` bash
$ cat <<EOF > test-lb-nginx.yaml
apiVersion: v1
kind: Pod
metadata:
  name: test-lb-nginx-pod
  labels:
    app: test-lb-nginx
spec:
  containers:
  - name: nginx
    image: nginx
---
apiVersion: v1
kind: Service
metadata:
  name: test-lb-nginx-svc
  labels:
    app: test-lb-nginx
spec:
  selector:
    app: test-lb-nginx 
  ports:
    - port: 80
      targetPort: 80
  type: LoadBalancer
EOF


$ k create -f test-lb-nginx.yaml
pod/test-lb-nginx-pod created
service/test-lb-nginx-svc created

$ k get svc -l app=test-lb-nginx
NAME                TYPE           CLUSTER-IP     EXTERNAL-IP      PORT(S)        AGE
test-lb-nginx-svc   LoadBalancer   10.106.13.28   10.0.0.232   80:30104/TCP   19s

### 마스터 m1 노드에서 테스트
$ curl 10.0.0.232
<!DOCTYPE html>
<html>
<head>
<title>Welcome to nginx!</title>
<style>
    body {
        width: 35em;
        margin: 0 auto;
        font-family: Tahoma, Verdana, Arial, sans-serif;
    }
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

pc 에서도 로드밸런서로 접근 확인

![](/.uploads/2021-07-15-22-35-44.png)

테스트한 리소스 삭제

``` bash
$ k delete -f test-lb-nginx.yaml
pod "test-lb-nginx-pod" deleted
service "test-lb-nginx-svc" deleted
```

## 4. ingress-nginx-controller 의 external IP 를 변경한 VIP 로 수정

helm 으로 ingress-nginx 릴리즈 생성

``` bash
helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx  -n ingress-nginx --create-namespace
```

`ingress-nginx-controller` SVC 의 external IP 확인

``` bash
$ k get svc -n ingress-nginx
NAME                                 TYPE           CLUSTER-IP      EXTERNAL-IP      PORT(S)                      AGE
ingress-nginx-controller             LoadBalancer   10.96.199.168   10.0.0.232   80:31124/TCP,443:32373/TCP   29s
ingress-nginx-controller-admission   ClusterIP      10.103.55.94    <none>           443/TCP                      29s
```

- `10.0.0.232` 로써 정상적으로 metallb 로드밸런서 IP 로 할당 됐다.

## 5. gitlab helm release 의 override-values.yaml 을 upgrade

`override-values.yaml` 수정

``` yaml
global:
  edition: ce
  hosts:
    domain: ailab.com
    https: true
    externalIP: 10.0.0.232
  ingress:
    congifureCertmanager: false
    class: "nginx"
    enabled: true
    tls:
      secretName: ailab-gitlab
      enabled: true
certmanager:
  install: false
certmanager-issuer:
  email: bellship24@gmail.com
nginx-ingress:
  enabled: false
gitlab-runner:
  install: false
```

helm 으로 gitlab 설치

``` bash
helm upgrade --install gitlab gitlab/gitlab \
  -n cicd --create-namespace \
  --timeout 600s \
  --version 4.12.4 \
  -f override-values.yaml
```

gitlab 관련하여 할당된 ingress 확인

``` bash
$ k get ingress -n cicd
NAME                        CLASS    HOSTS                ADDRESS          PORTS     AGE
gitlab-minio                <none>   minio.ailab.com      10.0.0.232   80, 443   65s
gitlab-registry             <none>   registry.ailab.com   10.0.0.232   80, 443   65s
gitlab-webservice-default   <none>   gitlab.ailab.com     10.0.0.232   80, 443   65s
```

pc 에서 `/etc/hosts` 수정

``` bash
# gitlab
10.0.0.232 ailab.com gitlab.ailab.com minio.ailab.com registry.ailab.com
```

- 해당 IP 는 사내망이기 때문에 pc 에 /etc/hosts 를 수정했다.

기존 gitlab UI 에 접근 확인

![](/.uploads/2021-07-16-02-28-12.png)

- helm upgrade 를 한 것이기 때문에 기존 데이터들도 그대로 있는 것을 확인할 수 있다.