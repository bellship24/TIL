# 전제

- helm v2+
- storageClass
- nginx-ingress-controller

# helm 설치

``` bash

```

# helm chart repo 추가

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

# prometheus 의 override-values.yaml 작성

chart 다운로드

``` bash
helm fetch --untar prometheus-community/prometheus
cd prometheus
```

override-values.yaml 작성

``` bash
cat << EOF > override-values.yaml
server:
  enabled: true

ingress:
  enabled: true
  annotations:
    kubernetes.io/ingress.class: nginx
    hosts:
    - prometheus.ailab.com

persistentVolume:
  enabeld: true
  accessModes:
  - ReadWriteOnce
  mountPath: /data
  size: 8Gi
replicaCount: 1

## Prometheus data retention period (default if not specified is 15 days)
##
retention: "10d"
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