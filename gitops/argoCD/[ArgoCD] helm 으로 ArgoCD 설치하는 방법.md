**목차**

- [1. 사용한 argoCD 헬름 차트 설명](#1-사용한-argocd-헬름-차트-설명)
- [2. repo 추가 다운로드](#2-repo-추가-다운로드)
- [3. override-values.yaml 작성](#3-override-valuesyaml-작성)
- [4. 설치](#4-설치)
- [5. UI 접근 및 비밀번호 세팅](#5-ui-접근-및-비밀번호-세팅)
- [6. 트러블슈팅](#6-트러블슈팅)
  - [6.1. argocd 도메인 접근 불가 [해결]](#61-argocd-도메인-접근-불가-해결)

**참고**

- [[argocd GitHub] argocd's helm chart](https://github.com/argoproj/argo-helm/tree/master/charts/argo-cd)

---

**요약**

> k8s 의 지속적 배포 툴 ArgoCD 를 helm 으로 설치해보자. self-signed 인증서를 위한 cert-manager 와 nginx-ingress 를 사용했다.

# 1. 사용한 argoCD 헬름 차트 설명

- [ArgoCD Chart URL](https://github.com/argoproj/argo-helm/tree/master/charts/argo-cd)
- 이 차트는 official 이 아닌 community maintained 되는 차트이다.
- 기본 설정으로 설치하면 argoCD release 와 같이 설치된다.
- 현재로써 이 차트는 argoCD HA 버전을 지원하지 않는다.

# 2. repo 추가 다운로드

``` bash
$ helm repo add argo https://argoproj.github.io/argo-helm
"argo" has been added to your repositories
```

# 3. override-values.yaml 작성

``` bash
cat <<EOF > override-values.yaml
server:
  ingress:
    enabled: true
    annotations:
      certmanager.k8s.io/issuer: gitlab-issuer
      kubernetes.io/ingress.class: nginx
      kubernetes.io/ingress.provider: nginx
      kubernetes.io/tls-acme: "true"
      nginx.ingress.kubernetes.io/proxy-body-size: "0"
      nginx.ingress.kubernetes.io/proxy-buffering: "off"
      nginx.ingress.kubernetes.io/proxy-read-timeout: "900"
      nginx.ingress.kubernetes.io/proxy-request-buffering: "off"
      nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
      nginx.ingress.kubernetes.io/ssl-passthrough: "true"
      nginx.ingress.kubernetes.io/backend-protocol: "HTTPS"
    labels:
      app: argocd
    hosts:
    - argocd.mylab.com
    tls:
    - hosts:
      - argocd.mylab.com
      secretName: mylab-gitlab
    https: true
EOF
```

- 작성한 annotation 에 대한 상세한 검증이 필요하다. 예를 들면, nginx-ingress 관련된 설정들 검증이 필요하다.
- 그 중에서도, 이러한 annotation 없이 nginx-ingress 와 tls 설정을 사용하게되면, helm 뿐만이 아니라 기존에 argocd 의 설정이 필요하다. [여기 링크](https://github.com/argoproj/argo-cd/blob/master/docs/operator-manual/ingress.md#ssl-passthrough-with-cert-manager-and-lets-encrypt) 를 참고하여 해결했으며 이는 cert-manager 를 쓸 경우, redirect loop 를 벗어나기 위해 SSL-Passthrough 하기 위함이다.

# 4. 설치

``` bash
$ helm upgrade --install argocd argo/argo-cd \
  -n cicd --create-namespace \
  --timeout 600s \
  --version 3.10.0 \
  -f override-values.yaml

Release "argocd" does not exist. Installing it now.
NAME: argocd
LAST DEPLOYED: Wed Jul 21 16:53:44 2021
NAMESPACE: cicd
STATUS: deployed
REVISION: 1
TEST SUITE: None
NOTES:
In order to access the server UI you have the following options:

1. kubectl port-forward service/argocd-server -n cicd 8080:443

    and then open the browser on http://localhost:8080 and accept the certificate
2. enable ingress in the values file `server.ingress.enabled` and either
      - Add the annotation for ssl passthrough: https://github.com/argoproj/argo-cd/blob/master/docs/operator-manual/ingress.md#option-1-ssl-passthrough
      - Add the `--insecure` flag to `server.extraArgs` in the values file and terminate SSL at your ingress: https://github.com/argoproj/argo-cd/blob/master/docs/operator-manual/ingress.md#option-2-multiple-ingress-objects-and-hosts

After reaching the UI the first time you can login with username: admin and the random password generated during the installation. You can find the password by running:

kubectl -n cicd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
(You should delete the initial secret afterwards as suggested by the Getting Started Guide: https://github.com/argoproj/argo-cd/blob/master/docs/getting_started.md#4-login-using-the-cli)
```

- argocd helm chart 버전은 3.10.0 이고 argocd 의 버전은 2.0.4 이다.

# 5. UI 접근 및 비밀번호 세팅

`/etc/hosts` 에 해당 ingress 에 대한 hostalias 설정부터 할 것

웹페이지 접근 확인

![](/.uploads/2021-07-22-02-36-45.png)

초기 비밀번호 확인

``` bash
$ kubectl -n cicd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d && echo
jRtmMUbobnOFtEll
```

- 초기 계정명은 admin 이다.
- 초기 패스워드를 통해 초기 로그인을 한 뒤에, 패스워드를 바꾸고 argocd-initial-admin-secret 을 지우길 권고된다. 물론 추후에 다시 만들 수도 있다.

로그인 확인

![](/.uploads/2021-07-22-02-38-37.png)

argocd CLI 설치

``` bash
sudo curl -sSL -o /usr/local/bin/argocd https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64
sudo chmod +x /usr/local/bin/argocd
```

argocd CLI 로그인

``` bash
$ argocd login argocd.mylab.com
WARNING: server certificate had error: x509: certificate relies on legacy Common Name field, use SANs or temporarily enable Common Name matching with GODEBUG=x509ignoreCN=0. Proceed insecurely (y/n)? y
WARN[0001] Failed to invoke grpc call. Use flag --grpc-web in grpc calls. To avoid this warning message, use flag --grpc-web. 
Username: admin
Password: 
'admin:login' logged in successfully
Context 'argocd.mylab.com' updated
```

argocd CLI 로 비밀번호 변경

``` bash
$ argocd account update-password
WARN[0000] Failed to invoke grpc call. Use flag --grpc-web in grpc calls. To avoid this warning message, use flag --grpc-web. 
*** Enter current password: 
*** Enter new password: 
*** Confirm new password: 
Password updated
Context 'argocd.mylab.com' updated
```

웹 UI 에서 변경한 비밀번호로 로그인 -> 정상적으로 됨

initial pass SECRET 삭제

``` bash
kubectl -n cicd delete secret argocd-initial-admin-secret
```

# 6. 트러블슈팅

## 6.1. argocd 도메인 접근 불가 [해결]

- ingress 고려한 helm 배포 완료
- pc 에서 웹브라우저로 도메인 호출은 되나 결국 접근이 안 되고 에러 발생
- 해당 네임스페이스에서 alpine 파드 띄워서 SVC 로 curl 해보면 정상 접근 됨
- 노드 로컬에서 도메인으로 curl 하면 Temporary Redirect 만 뜬다.
- 이 문제는 308 response 로그가 계속 뜬다.
- [여기 링크](https://github.com/argoproj/argo-cd/blob/master/docs/operator-manual/ingress.md#ssl-passthrough-with-cert-manager-and-lets-encrypt) 를 참고해서 argocd 에서 nginx-ingress 를 사용하기 위한 annotation 설정을 추가해줘서 됐다.