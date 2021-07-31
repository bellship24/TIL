**목차**

- [1. 요약](#1-요약)
- [2. docker 의 포트 포워딩 한계와 k8s 클러스터 노드들에서 파드로 접근하는 방법](#2-docker-의-포트-포워딩-한계와-k8s-클러스터-노드들에서-파드로-접근하는-방법)
- [3. Service 가 기본적으로 파드를 노출시키는 방법 : Selector 와 ClusterIP](#3-service-가-기본적으로-파드를-노출시키는-방법--selector-와-clusterip)
- [4. core-dns 를 통해 Pod 에서 Service 로 통신하는 방법](#4-core-dns-를-통해-pod-에서-service-로-통신하는-방법)

---

# 1. 요약

> 리눅스 네트워크를 잘 모르는 내게는 k8s 의 네트워크가 굉장히 복잡하고 특이하다. 간혹 그 내부를 deep-dive 하고 싶어진다. 그래서 그때그때 궁금한 내용을 찾아보고 정리해보려한다.

# 2. docker 의 포트 포워딩 한계와 k8s 클러스터 노드들에서 파드로 접근하는 방법

참고 URL: [[k8s docs] 서비스와 애플리케이션 연결하기](https://kubernetes.io/ko/docs/concepts/services-networking/connect-applications-service/#컨테이너-연결을-위한-쿠버네티스-모델)

도커의 경우, 호스트-프라이빗 네트워킹을 사용하여 컨테이너의 노드 간 통신이 굉장히 어려운 문제가 있음.

- 기본적으로 도커는 호스트-프라이빗 네트워킹을 사용하기에 컨테이너는 동일한 머신에 있는 경우에만 다른 컨테이너와 통신 할 수 있다. 도커 컨테이너가 노드를 통해 통신하려면 머신 포트에 IP 주소가 할당되어야 컨테이너에 전달되거나 프록시된다. 이것은 컨테이너가 사용하는 포트를 매우 신중하게 조정하거나 포트를 동적으로 할당해야 한다는 의미이다.
- 컨테이너를 제공하는 여러 개발자 또는 팀에서 포트를 조정하는 것은 규모면에서 매우 어려우며, 사용자가 제어할 수 없는 클러스터 수준의 문제에 노출된다.

하지만 쿠버네티스의 경우, 모든 파드에게 자체 클러스터-프라이빗 IP 주소를 제공하므로 NAT 없이 노드와 파드 간에, 파드와 파드 간에 서로 통신할 수 있다.

- 쿠버네티스는 파드가 배치된 호스트와는 무관하게 다른 파드와 통신할 수 있다고 가정한다. 쿠버네티스는 모든 파드에게 자체 클러스터-프라이빗 IP 주소를 제공하기 때문에 파드간에 명시적으로 링크를 만들거나 컨테이너 포트를 호스트 포트에 매핑 할 필요가 없다. 이것은 파드 내의 컨테이너는 모두 로컬호스트에서 서로의 포트에 도달할 수 있으며 클러스터의 모든 파드는 NAT 없이 서로를 볼 수 있다는 의미이다.

containerPort 를 사용하여 파드에 할당된 `[파드 IP]:[containerPort]` 로 호출 테스트해보자.

- 사실 containerPort 는 이미 이미지를 빌드할 때, EXPOSE 로 명시 됐을 수도 있다. 이 경우, containerPort 를 명시하지 않아도 파드는 EXPOSE 된 포트를 listen 하게 된다. 그럼에도 굳이 containerPort 를 명시하는 이유는 관리 차원의 목적이 있기 때문이다.

nginx deployment 배포

``` bash
$ cat <<EOF | kubectl apply -f -
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
        image: nginx
        ports:
        - containerPort: 80
EOF

deployment.apps/my-nginx created
```

배포된 nginx deployment 의 파드 조회

``` bash
$ kubectl get pods -l run=my-nginx -o wide
NAME                        READY   STATUS    RESTARTS   AGE    IP           NODE                  NOMINATED NODE   READINESS GATES
my-nginx-5b56ccd65f-7tljf   1/1     Running   0          107s   10.46.0.8    dtlab-dev-k8s-pjb-3   <none>           <none>
my-nginx-5b56ccd65f-r7mhj   1/1     Running   0          107s   10.32.0.12   dtlab-dev-k8s-pjb-2   <none>           <none>
```

파드 IP 만 조회

``` bash
$ k get po -l run=my-nginx -o yaml | grep podIP
    podIP: 10.46.0.8
    podIPs:
    podIP: 10.32.0.12
    podIPs:
```

클러스터 내에 노드들에서 `[파드 IP]:[containerPort]` 로 curl 테스트

``` bash
$ curl 10.46.0.8:80
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

# 3. Service 가 기본적으로 파드를 노출시키는 방법 : Selector 와 ClusterIP

참고 URL: [[k8s docs] 서비스와 애플리케이션 연결하기](https://kubernetes.io/ko/docs/concepts/services-networking/connect-applications-service/#서비스-생성하기)

Service 가 Pod 를 노출하는 방법은 기본적으로 Selector 와 ClusterIP 를 사용한다.

- Service(SVC) 가 생성되면 기본적으로 ClusterIP 라는 고유한 IP 가 할당되고 이 주소는 SVC 의 생명주기에 따라 SVC 가 활성화 되어있는 동안에는 변경되지 않는다.
- Pod 는 서비스와 통신하도록 구성할 수 있고 SVC 와의 통신은 멤버 중 일부 파드에 자동적으로 로드밸런싱된다.
- SVC 는 Selector 를 통해 지원받을 파드 그룹을 지속적으로 찾고 이 파드들의 고유한 내부 IP 를 Endpoints 를 노출시킨다.
- 이렇게 생성된 ClusterIP 는 클러스터 모든 노드에서 접근 가능하지만 이는 가상의 IP 이므로 오직 클러스터 내부에서만 접근 가능하다.

서비스 생성하기

``` bash
$ cat <<EOF | k create -f -
apiVersion: v1
kind: Service
metadata:
  name: my-nginx
  labels:
    run: my-nginx
spec:
  ports:
  - port: 80
    protocol: TCP
  selector:
    run: my-nginx
EOF

service/my-nginx created
```

배포한 서비스의 ClusterIP 와 Endpoints 조회

``` bash
$ k describe svc my-nginx

Name:              my-nginx
Namespace:         default
Labels:            run=my-nginx
Annotations:       <none>
Selector:          run=my-nginx
Type:              ClusterIP
IP Family Policy:  SingleStack
IP Families:       IPv4
IP:                10.98.205.88
IPs:               10.98.205.88
Port:              <unset>  80/TCP
TargetPort:        80/TCP
Endpoints:         10.32.0.12:80,10.46.0.8:80
Session Affinity:  None
Events:            <none>
```

- ClusterIP 는 10.98.205.88
- Endpoints 는 10.32.0.12:80, 10.46.0.8:80 로써 Selector 에 따라 해당하는 파드의 IP:containerPort 로 설정된 것을 알 수 있다.
- Port 와 TargetPort 모두 80 인 것을 확인할 수 있다.

# 4. core-dns 를 통해 Pod 에서 Service 로 통신하는 방법

- 참고 URL: [[k8s docs] 서비스와 애플리케이션 연결하기](https://kubernetes.io/ko/docs/concepts/services-networking/connect-applications-service/#dns)

- k8s 는 DNS 클러스터 애드온 서비스를 제공하며 dns 이름을 서비스에 자동으로 할당한다.

core-dns 실행 확인

``` bash
$ k get po -n kube-system -l k8s-app=kube-dns
NAME                       READY   STATUS    RESTARTS   AGE
coredns-558bd4d5db-2zwm6   1/1     Running   1          43d
coredns-558bd4d5db-66h9g   1/1     Running   1          43d

$ k get svc -n kube-system kube-dns
NAME       TYPE        CLUSTER-IP   EXTERNAL-IP   PORT(S)                  AGE
kube-dns   ClusterIP   10.96.0.10   <none>        53/UDP,53/TCP,9153/TCP   43d
```

- core-dns 의 Pod 는 deployment 로 관리되며 replica 는 2 개로써 kube-system 네임스페이스에 배포된다.
- core-dns 의 SVC 이름은 kube-dns 이다.
- 이 SVC 는 기본적으로 `10.96.0.10` 이라는 ClusterIP 를 갖는다. 포트는 DNS 이므로 53 을 사용한다.

파드 하나를 생성해서 kube-dns SVC 를 해석해보자.

``` bash
$ kubectl run curl --image=radial/busyboxplus:curl -ti --rm
[ root@curl:/ ]$ nslookup kube-dns.kube-system.svc.cluster.local
Server:    10.96.0.10
Address 1: 10.96.0.10 kube-dns.kube-system.svc.cluster.local

Name:      kube-dns.kube-system.svc.cluster.local
Address 1: 10.96.0.10 kube-dns.kube-system.svc.cluster.local
```

- radial/busyboxplus:curl 이미지를 사용해서 nslookup 을 통해 kube-dns 서비스명을 도메인 해석해보았다.
- 실행한 curl 파드는 default 네임스페이스에 있고 kube-dns 서비스는 kube-system 네임스페이스에 있으므로 curl 파드에서 kube-dns 서비스를 해석할 때는 네임스페이스가 달라 도메인을 아래와 같이 작성해줘야 해석가능하다.
  - `[service name].[namespace]`
  - `[service name].[namespace].svc`
  - `[service name].[namespace].svc.cluster.local`
- kube-dns 를 nslookup 해보면 dns 의 IP 가 10.96.0.10 으로 kube-dns 의 ClusterIP 인 것을 확인할 수 있다. 도메인 서버의 도메인도 `kube-dns.kube-system.svc.cluster.local` 인 것을 확인할 수 있다.
