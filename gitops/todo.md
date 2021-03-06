# GitLab CI

- [ ] .gitlab-ci.yml 에 대한 어느 한국 블로그의 설명글
  - [[blog kr] .gitlab-ci.yml 에 대하여](https://assu10.github.io/dev/2020/10/09/gitlab-runner-3/)
- [ ] gitlab cicd 기본 개념 교육 영상
  - [[youtube] Gitlab CI Pipeline, Artifacts and Environments](https://www.youtube.com/watch?v=PCKDICEe10s)
  - [[youtube] Gitlab CI pipeline tutorial for beginners](https://www.youtube.com/watch?v=Jav4vbUrqII)
- [ ] gitlab repo. 에 gitlab 의 container registry 를 연동 및 활용 방법 검토
  - [[blog eng] Getting [meta] with GitLab CI/CD: Building build images](https://about.gitlab.com/blog/2019/08/28/building-build-images/)
- [ ] gitlab runner k8s executor 를 사용하면 dind 로 docker build 를 하는데 FROM 으로 base image 를 가져올 때 너무 오래 걸리고 매번 수행한다면 docker layer 의 cache 를 사용해야 한다. aws s3 등이 디폴트이지만 s3 compatible 인 minio 를 대신해서 쓸 수 있다. 연동 방법을 검증해보자.
  - [[다나와 기술 블로그] GitLab CI/CD cache with Kubernetes](https://danawalab.github.io/gitlab/2020/04/14/GitLab-CI-CD-cache-with-Kubernetes.html)
  - [[blog eng] Gitlab Docker Layer Caching for Kubernetes Executor](https://dev.to/liptanbiswas/gitlab-docker-layer-caching-for-kubernetes-executor-39ch)
- [ ] job artifacts 개념 검토 및 정리
  - [[gitlab docs] job artifacts](https://docs.gitlab.com/ee/ci/pipelines/job_artifacts.html)

# Argo CD

- [ ] project 란?
  - [[argocd docs] Project](https://argoproj.github.io/argo-cd/user-guide/projects/#projects)
