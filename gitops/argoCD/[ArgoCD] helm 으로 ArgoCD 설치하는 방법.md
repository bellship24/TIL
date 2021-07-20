**목차**

- [1. 전제](#1-전제)
  - [사용한 argoCD 헬름 차트에 대한 설명](#사용한-argocd-헬름-차트에-대한-설명)
- [repo 추가 다운로드](#repo-추가-다운로드)
- [2. 설치](#2-설치)
- [3. 업데이트](#3-업데이트)
- [4. 삭제](#4-삭제)

**참고**

- [[argocd GitHub] argocd's helm chart](https://github.com/argoproj/argo-helm/tree/master/charts/argo-cd)

---

**요약**

> argoCD 를 helm 으로 설치해보자.

# 1. 전제

## 사용한 argoCD 헬름 차트에 대한 설명

- [차트 URL](https://github.com/argoproj/argo-helm/tree/master/charts/argo-cd)
- 이 차트는 official 이 아닌 community maintained 되는 차트이다.
- 기본 설정으로 설치하면 argoCD release 와 같이 설치된다.
- 현재로써 이 차트는 argoCD HA 버전을 지원하지 않는다.

# repo 추가 다운로드

``` bash
$ helm repo add argo https://argoproj.github.io/argo-helm
"argo" has been added to your repositories
```

# 2. 설치

``` bash
helm upgrade --install argocd argo/argo-cd \
  -n cicd --create-namespace \
  --timeout 600s \
  --version 3.10.0 \
  -f override-values.yaml
```

# 3. 업데이트

# 4. 삭제