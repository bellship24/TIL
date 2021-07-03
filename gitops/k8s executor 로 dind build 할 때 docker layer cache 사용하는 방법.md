**요약**

- gitlab runner k8s executor 를 사용하면 dind 로 docker build 를 하는데 FROM 으로 base image 를 가져올 때 너무 오래 걸리고 매번 수행한다면 docker layer 의 cache 를 사용해야 한다. aws s3 등이 디폴트이지만 s3 compatible 인 minio 를 대신해서 쓸 수 있다. minio 연동 방법을 검토 및 검증해보자.

**목차**

- [1. 해결 방법](#1-해결-방법)

**참조**

# 1. 해결 방법

1. 매번 파드에 대해 docker dind 를 service 로 생성하지 않고 하나의 docker dind 컨테이너를 만들어 사용