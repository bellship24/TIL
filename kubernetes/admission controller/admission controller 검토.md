**목차**

- [1. 요약](#1-요약)
- [2. 개념 정리](#2-개념-정리)
  - [2.1. Admission Controller 란?](#21-admission-controller-란)
  - [2.2. Admission Controller 절차](#22-admission-controller-절차)
  - [2.3. Admission Controller Plugin 과 동작 원리](#23-admission-controller-plugin-과-동작-원리)
  - [2.4. 자체 플러그인을 직접 구현하는 방법](#24-자체-플러그인을-직접-구현하는-방법)

**참고**

- [[blog] admission controller 이해 및 python 을 mutate webhook 작성 예제](https://m.blog.naver.com/PostView.naver?isHttpsRedirect=true&blogId=alice_k106&logNo=221546328906)

---

# 1. 요약

> `참고` 에 첨부한 링크의 블로그 글에 자세히 나온 admission controller 를 읽으며 자체적으로 필기하고 공부한 내용이다.
> 즉, 본 글에서는 admission controller 의 개념, 절차, 동작원리를 간단히 이해하고 python flask 를 이용해 mutator 서버를 만들어 admission control 을 구현해본다.

# 2. 개념 정리

## 2.1. Admission Controller 란?

- k8s 의 API 를 호출했을 때, 해당 요청의 내용을 변경(mutate)하거나 검증(validate)하는 플러그인의 집합
- 즉, Admission Controller 는 mutate 단계와 validate 단계가 있다

## 2.2. Admission Controller 절차

![](/.uploads/2021-08-19-23-25-47.png)

- api 로 들어오는 모든 요청은 최종적으로 etcd 에 저장되며 그 전에 Auth 를 거침
- JWT 또는 인증서 등을 통해 클라이언트 인증을 한 뒤 API 요청이 RBAC 권한과 매칭되는지 검사
- 인증이 완료되면, admission controller 의 mutate 과정과 validate 과정을 거쳐 etcd 에 요청 데이터를 저장
- 그 뒤에는 컨트롤러나 스케줄러 등이 etcd 의 데이터를 감지해 적절한 작업을 수행

## 2.3. Admission Controller Plugin 과 동작 원리

- admission controller 에는 많은 종류의 내장 plugin 이 존재하며 자체 plugin 도 직접 구현 가능
- k8s 에는 기본적으로 여러 개의 plugin 이 기본적으로 적용되어 있음. 예를 들면, 컨테이너의 root privileged 권한 부여에 따른 보얀 취약점을 막기 위해 PodSecurityPolicy 플러그인이 적용되어 있음

내장 plugin 조회

```bash
$ docker run -it --rm k8s.gcr.io/kube-apiserver:v1.14.1 kube-apiserver -h | grep enable-admission-plugins
...
      --enable-admission-plugins strings       
admission plugins that should be enabled in addition to default enabled ones 
(NamespaceLifecycle, LimitRanger, ServiceAccount, TaintNodesByCondition, Priority, 
DefaultTolerationSeconds, DefaultStorageClass, PersistentVolumeClaimResize, 
MutatingAdmissionWebhook, ValidatingAdmissionWebhook, ResourceQuota).
...
Colored by Color Scripter

# 기본적으로 적용된 NodeRestriction 플러그인 확인
$ ps aux | grep admission-plugin
root      2879  5.5  8.7 542956 352380 ?   Ssl  08:03   0:14 kube-apiserver ... --enable-admission-plugins=NodeRestriction ...
```

내장 plugin 사용 방법

- kuber-apiserver 의 `—enabled-admission-plugins=[추가할 플러그인명]` 옵션으로 사용 가능

내장 플러그인 예제

- PodPreset : 파드 템플릿에 특정 설정을 자동으로 추가

## 2.4. 자체 플러그인을 직접 구현하는 방법

앞서 봤던 Admission Controller 절차를 통해 자체 플러그인 구현 방법 이해하기

![](/.uploads/2021-08-19-23-26-26.png)

- **자체 플러그인은 MutatingAdmissionWebhook 과 ValidatingAdmissionWebhook 의 내장 플러그인을 사용하여 구현**
- 빨간 박스 부분이 직접 소스코드로 구현해야 하는 Webhook 부분

mutator 의 Webhook 동작 원리

![](/.uploads/2021-08-19-23-26-38.png)

- validator 의 원리도 mutator 와 비슷하다.
- `AdmissionView` 라고하는 객체는 클라이언트가 API 서버로 요청한 매니페스트 데이터(e.g. Pod 템플릿)를 담고 있음
- 여기서 AdmissionView 객체를 검사하고 수정하는 Mutator 의 비즈니스 로직이 담긴 서버를 소스코드로 구현해야 함
- 이 Mutator 서버는 flask, go, tomcat 등 어떤 언어/프레임워크를 사용해도 상관 없으며 REST POST 요청을 받아들일 수만 있으면 됨
- Mutator 서버를 파드로 생성한 뒤 해당 파드의 엔드포인트를 MutateWebhook 으로서 쿠버네티스에 등록한다
- 그러면, 앞으로 mutating admission 단계에서는 앞서 배포한 해당 mutator 서버가 포함될 것이고 이 서버의 엔드포인트(k8s 의 SVC 오브젝트)로 POST 요청이 전송된다 (Webhook)
- 클라이언트로부터 API 요청이 도착하면 API 서버는 Mutating admission 단계에서 여러 Mutator 에 요청을 보낸다
- 여기에는 직접 구현한 mutator 서버에 AdmissionView 데이터를 전송하는 일종의 Webhook 요청 과정도 포함됨. 즉, 직접 구현한 mutator 서버는 앞서 정의한 비즈니스 로직에 따라 AdmissionView 데이터를 원하는대로 적절히 가공해 API 서버에 다시 반환하게 된다