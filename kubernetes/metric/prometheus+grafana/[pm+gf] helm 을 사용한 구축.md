**목차**

- [1. 전제](#1-전제)
- [2. helm 설치](#2-helm-설치)
- [3. helm chart repo 추가](#3-helm-chart-repo-추가)
- [4. prometheus 배포](#4-prometheus-배포)
- [5. grafana 배포](#5-grafana-배포)

---

# 1. 전제

- helm v3+
- storageClass
- nginx-ingress-controller

# 2. helm 설치

``` bash

```

# 3. helm chart repo 추가

``` bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add kube-state-metrics https://kubernetes.github.io/kube-state-metrics
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

$ helm repo list
NAME                            URL
prometheus-community            https://prometheus-community.github.io/helm-charts
kube-state-metrics              https://kubernetes.github.io/kube-state-metrics
grafana                         https://grafana.github.io/helm-charts
```

# 4. prometheus 배포

chart 다운로드

``` bash
helm fetch --untar prometheus-community/prometheus
cd prometheus
```

override-values.yaml 작성

``` bash
cat << EOF > override-values.yaml
server:
  ingress:
    enabled: true
    annotations:
      # certmanager.k8s.io/issuer: gitlab-issuer
      kubernetes.io/ingress.class: nginx
      kubernetes.io/ingress.provider: nginx
      # kubernetes.io/tls-acme: "true"
      # nginx.ingress.kubernetes.io/proxy-body-size: "0"
      # nginx.ingress.kubernetes.io/proxy-buffering: "off"
      # nginx.ingress.kubernetes.io/proxy-read-timeout: "900"
      # nginx.ingress.kubernetes.io/proxy-request-buffering: "off"
      # nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
      # nginx.ingress.kubernetes.io/ssl-passthrough: "true"
      # nginx.ingress.kubernetes.io/backend-protocol: "HTTPS"
    hosts:
    - prometheus.ailab.com
    tls:
    - secretName:
      hosts:
      - prometheus.ailab.com

## Prometheus data retention period (default if not specified is 15 days)
##
# retention: "15d"
EOF
```

release 배포

``` bash
helm upgrade --install \
prometheus \
prometheus-community/prometheus \
-n monitoring --create-namespace \
-f override-values.yaml
```

배포된 컴포넌트 확인

``` bash
$ k get all -n monitoring
NAME                                                 READY   STATUS    RESTARTS   AGE
pod/prometheus-alertmanager-9c55db574-4nw2x          2/2     Running   0          4h14m
pod/prometheus-kube-state-metrics-76f66976cb-gncwp   1/1     Running   0          4h14m
pod/prometheus-node-exporter-pqjdl                   1/1     Running   0          4h14m
pod/prometheus-node-exporter-skglc                   1/1     Running   0          4h14m
pod/prometheus-node-exporter-wjh99                   1/1     Running   0          4h14m
pod/prometheus-pushgateway-598d657c9-rgqqk           1/1     Running   0          4h14m
pod/prometheus-server-b6ddb8f5c-cttqg                2/2     Running   0          4h14m

NAME                                    TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)    AGE
service/prometheus-alertmanager         ClusterIP   10.104.107.49    <none>        80/TCP     4h14m
service/prometheus-kube-state-metrics   ClusterIP   10.102.132.237   <none>        8080/TCP   4h14m
service/prometheus-node-exporter        ClusterIP   None             <none>        9100/TCP   4h14m
service/prometheus-pushgateway          ClusterIP   10.104.186.240   <none>        9091/TCP   4h14m
service/prometheus-server               ClusterIP   10.108.208.15    <none>        80/TCP     4h14m

NAME                                      DESIRED   CURRENT   READY   UP-TO-DATE   AVAILABLE   NODE SELECTOR   AGE
daemonset.apps/prometheus-node-exporter   3         3         3       3            3           <none>          4h14m

NAME                                            READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/prometheus-alertmanager         1/1     1            1           4h14m
deployment.apps/prometheus-kube-state-metrics   1/1     1            1           4h14m
deployment.apps/prometheus-pushgateway          1/1     1            1           4h14m
deployment.apps/prometheus-server               1/1     1            1           4h14m

NAME                                                       DESIRED   CURRENT   READY   AGE
replicaset.apps/prometheus-alertmanager-9c55db574          1         1         1       4h14m
replicaset.apps/prometheus-kube-state-metrics-76f66976cb   1         1         1       4h14m
replicaset.apps/prometheus-pushgateway-598d657c9           1         1         1       4h14m
replicaset.apps/prometheus-server-b6ddb8f5c                1         1         1       4h14m

$ k get ing -n monitoring
NAME                CLASS    HOSTS                  ADDRESS      PORTS     AGE
prometheus-server   <none>   prometheus.ailab.com   10.0.0.232   80, 443   8m42s
```

Web UI 접근 확인

![](/.uploads/2021-08-13-23-47-55.png)

여러가지 표현식을 확인

![](/.uploads/2021-08-13-23-49-06.png)

kube_namespace_created 로 네임스페이스 생성 내역을 테이블로 보기

![](/.uploads/2021-08-13-23-50-50.png)

kube_namespace_created 로 네임스페이스 생성 내역을 그래프로 보기

![](/.uploads/2021-08-14-01-26-13.png)

# 5. grafana 배포

chart 다운로드

``` bash
helm fetch --untar grafana/grafana
cd grafana
```

override-values.yaml 작성

``` bash
cat << EOF > override-values.yaml
ingress:
  enabled: true
  annotations:
    # certmanager.k8s.io/issuer: gitlab-issuer
    kubernetes.io/ingress.class: nginx
    kubernetes.io/ingress.provider: nginx
    # kubernetes.io/tls-acme: "true"
    # nginx.ingress.kubernetes.io/proxy-body-size: "0"
    # nginx.ingress.kubernetes.io/proxy-buffering: "off"
    # nginx.ingress.kubernetes.io/proxy-read-timeout: "900"
    # nginx.ingress.kubernetes.io/proxy-request-buffering: "off"
    # nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
    # nginx.ingress.kubernetes.io/ssl-passthrough: "true"
    # nginx.ingress.kubernetes.io/backend-protocol: "HTTPS"
  hosts:
  - grafana.ailab.com
#   tls:
#   - secretName:
#     hosts:
#     - grafana.ailab.com
EOF
```

release 배포

``` bash
helm upgrade --install \
grafana \
grafana/grafana \
-n monitoring --create-namespace \
-f override-values.yaml
```

---

배포된 컴포넌트 확인

``` bash
$ kubens monitoring


$ k get all -l=app.kubernetes.io/instance=grafana
NAME                          READY   STATUS    RESTARTS   AGE
pod/grafana-7f8458bcb-bxlcf   1/1     Running   0          50m

NAME              TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)   AGE
service/grafana   ClusterIP   10.109.135.193   <none>        80/TCP    50m

NAME                      READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/grafana   1/1     1            1           50m

NAME                                DESIRED   CURRENT   READY   AGE
replicaset.apps/grafana-7f8458bcb   1         1         1       50m


$ k get ing -l=app.kubernetes.io/instance=grafana
NAME      CLASS    HOSTS               ADDRESS          PORTS   AGE
grafana   <none>   grafana.ailab.com   10.0.0.232   80      52m
```

초기 비밀번호 확인

``` bash
$ kubectl get secret --namespace monitoring grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo
dkZRUXMBQf3z9vhBus1W6e8yXUdf8d3gMaEQtxhV
```

Web UI 접근 확인

![](/.uploads/2021-08-14-03-01-43.png)
![](/.uploads/2021-08-14-03-01-55.png)

패스워드 변경하기