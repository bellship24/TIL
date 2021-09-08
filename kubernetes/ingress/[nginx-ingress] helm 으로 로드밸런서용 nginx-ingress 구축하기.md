**목차**

- [1. 요약](#1-요약)
- [2. 로드밸런서 타입 nginx-ingress 작동 방식 예제](#2-로드밸런서-타입-nginx-ingress-작동-방식-예제)
- [3. helm 으로 LoadBalancer 타입 nginx-ingress 설치](#3-helm-으로-loadbalancer-타입-nginx-ingress-설치)
- [4. nginx-ingress 테스트](#4-nginx-ingress-테스트)

**참고**

- [ingress-nginx docs : installing guide](https://kubernetes.github.io/ingress-nginx/deploy/#using-helm)

---

# 1. 요약

> **로드밸런서 타입 nginx-ingress 의 작동 방식을 알아본다.**
> **로드밸런서 타입 nginx-ingress 를 helm 으로 설치한다.**
> **로드밸런서 타입 nginx-ingress 에 대해 nginx 로 ingress 테스트를 해본다.**

# 2. 로드밸런서 타입 nginx-ingress 작동 방식 예제

![](/.uploads/2021-06-18-16-21-58.png)

# 3. helm 으로 LoadBalancer 타입 nginx-ingress 설치

전제

- helm v3

helm 레포 추가 및 설치

``` bash
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update
helm repo ls

helm upgrade --install \
  ingress-nginx ingress-nginx/ingress-nginx \
  -n ingress-nginx --create-namespace
```

설치된 nginx-ingress 확인

``` bash
### 컴포넌트 확인 
$ k get all -n ingress-nginx
NAME                                           READY   STATUS    RESTARTS   AGE
pod/ingress-nginx-controller-7d98fb5bd-xhl68   1/1     Running   0          70m

NAME                                         TYPE           CLUSTER-IP       EXTERNAL-IP      PORT(S)                      AGE
service/ingress-nginx-controller             LoadBalancer   10.111.135.236   10.0.0.216   80:31084/TCP,443:32177/TCP   70m
service/ingress-nginx-controller-admission   ClusterIP      10.97.216.58     <none>           443/TCP                      70m

NAME                                       READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/ingress-nginx-controller   1/1     1            1           70m

NAME                                                 DESIRED   CURRENT   READY   AGE
replicaset.apps/ingress-nginx-controller-7d98fb5bd   1         1         1       70m

### 버전 확인
$ POD_NAME=$(kubectl get pods -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx -o jsonpath='{.items[0].metadata.name}')
$ kubectl exec -it $POD_NAME -n ingress-nginx -- /nginx-ingress-controller --version

-------------------------------------------------------------------------------
NGINX Ingress controller
  Release:       v0.47.0
  Build:         7201e37633485d1f14dbe9cd7b22dd380df00a07
  Repository:    https://github.com/kubernetes/ingress-nginx
  nginx version: nginx/1.20.1

-------------------------------------------------------------------------------
```

# 4. nginx-ingress 테스트

deployment, service, ingress 생성

``` bash
cat <<EOF | kubectl apply -f -
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
---
apiVersion: v1
kind: Service
metadata:
  name: my-nginx
  labels:
    run: my-nginx
spec:
  type: ClusterIP
  ports:
  - port: 80
    protocol: TCP
  selector:
    run: my-nginx
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: my-nginx
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    kubernetes.io/ingress.class: nginx
    kubernetes.io/ingress.provider: nginx
spec:
  rules:
  - host: "my.lab.com"
    http:
      paths:
      - pathType: Prefix
        path: /
        backend:
          service:
            name: my-nginx
            port:
              number: 80
EOF
```

endpoint 확인

``` bash
k get ingress
```

ingress 통한 접근 테스트

``` bash
### /etc/hosts 수정
$ sudo bash -c 'cat <<EOF >> /etc/hosts
10.0.0.216 my.lab.com
'

### curl 테스트
$ curl my.lab.com/hostname
Welcome! http-go-568f649bb-fh9mt
```

테스트 리소스 삭제

``` bash
k delete deployment/my-nginx service/my-nginx ingress/my-nginx
```