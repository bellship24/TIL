**목차**

- [1. 요약](#1-요약)
- [2. 전제](#2-전제)
- [3. helm 으로 metallb 설치](#3-helm-으로-metallb-설치)
- [4. LoadBalancer 타입 SVC 테스트](#4-loadbalancer-타입-svc-테스트)

**참고**

- [[MetalLB Docs] Installation With Helm](https://metallb.universe.tf/installation/)

---

# 1. 요약

helm 으로 metallb 를 설치하고 LoadBalancer 타입의 SVC 가 잘 작동하는 지 확인해보자.

# 2. 전제

- k8s 클러스터
- lb 로 사용할 IP
- helm v3+

# 3. helm 으로 metallb 설치

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

- lb 로 쓰일 ip 주소가 특정한 ip 하나라고 해도 서브넷을 32 로 줘야한다.

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

metallb controller 로그 확인

``` bash
$ k logs -n metallb metallb-controller-748756655f-n7hxn
{"branch":"HEAD","caller":"main.go:138","commit":"v0.10.2","goversion":"gc / go1.16.5 / amd64","msg":"MetalLB controller starting version 0.10.2 (commit v0.10.2, branch HEAD)","ts":"2021-07-15T13:25:28.58
2601917Z","version":"0.10.2"}          
{"caller":"k8s.go:364","msg":"secret succesfully created","op":"CreateMlSecret","ts":"2021-07-15T13:25:28.628442397Z"}                                                                                      
{"caller":"main.go:100","configmap":"metallb/metallb","event":"startUpdate","msg":"start of config update","ts":"2021-07-15T13:25:28.728927393Z"}                                                           
{"caller":"main.go:113","configmap":"metallb/metallb","event":"endUpdate","msg":"end of config update","ts":"2021-07-15T13:25:28.729071795Z"}                                                               
{"caller":"k8s.go:545","configmap":"metallb/metallb","event":"configLoaded","msg":"config (re)loaded","ts":"2021-07-15T13:25:28.729098015Z"}                                                                
{"caller":"main.go:49","event":"startUpdate","msg":"start of service update","service":"cicd/gitlab-postgresql-metrics","ts":"2021-07-15T13:25:28.729226719Z"}
{"caller":"service.go:33","event":"clearAssignment","msg":"not a LoadBalancer","reason":"notLoadBalancer","service":"cicd/gitlab-postgresql-metrics","ts":"2021-07-15T13:25:28.729272781Z"}                 
{"caller":"main.go:75","event":"noChange","msg":"service converged, no change","service":"cicd/gitlab-postgresql-metrics","ts":"2021-07-15T13:25:28.729491353Z"}                                            
{"caller":"main.go:76","event":"endUpdate","msg":"end of service update","service":"cicd/gitlab-postgresql-metrics","ts":"2021-07-15T13:25:28.729516469Z"}                                                  
{"caller":"main.go:49","event":"startUpdate","msg":"start of service update","service":"default/guestbook-ui","ts":"2021-07-15T13:25:28.72954087Z"} 
...
```

- 컨트롤러가 정상적으로 시작됐고 여러 SVC 들에 대한 로드밸런싱 정책을 업데이트하는 것을 확인할 수 있다.

# 4. LoadBalancer 타입 SVC 테스트

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
