**요약**

> CI/CD 를 검증하기 위해 ArgoCD 에서 연동할 helm repo. 를 직접 만들어보자.

**목차**

**참고**

[[blog] helm chart 생성 기본](https://happycloud-lee.tistory.com/5)
[[blog] github 을 helm chart repo 로 활용하는 방법](https://uzihoon.com/post/b1765d60-5ed9-11ea-9252-e1204d96fe61)
[[blog] Hosting a Helm repository on GitLab Pages](https://tobiasmaier.info/posts/2018/03/13/hosting-helm-repo-on-gitlab-pages.html)

---

chart 기본 구조 생성

``` bash
$ helm create hello-helm
$ tree hello-helm
hello-helm
├── charts
├── Chart.yaml
├── templates
│   ├── deployment.yaml
│   ├── _helpers.tpl
│   ├── hpa.yaml
│   ├── ingress.yaml
│   ├── NOTES.txt
│   ├── serviceaccount.yaml
│   ├── service.yaml
│   └── tests
│       └── test-connection.yaml
└── values.yaml

3 directories, 10 files
```
