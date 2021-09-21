**목차**

- [1. 요약](#1-요약)
- [2. Getting Started](#2-getting-started)
  - [2.1. 차트 생성](#21-차트-생성)
  - [2.2. templates 폴더 내에 기본 생성된 파일 삭제](#22-templates-폴더-내에-기본-생성된-파일-삭제)
  - [2.3. 첫번째 템플릿 작성](#23-첫번째-템플릿-작성)
  - [2.4. 템플릿 기반 차트 설치](#24-템플릿-기반-차트-설치)
  - [2.5. 릴리즈의 실제 템플릿 받기](#25-릴리즈의-실제-템플릿-받기)
  - [2.6. 간단한 템플릿 지시어 추가](#26-간단한-템플릿-지시어-추가)
  - [2.7. 템플릿 랜더링 테스트](#27-템플릿-랜더링-테스트)
- [3. Built-in Objects](#3-built-in-objects)
  - [3.1. Release 객체](#31-release-객체)

**참고**

- [[Helm docs] Chart Template Guide](https://helm.sh/docs/chart_template_guide/getting_started/)

---

# 1. 요약

> helm docs 의 chart template guide 를 공부하면서 정리하자.

# 2. Getting Started

## 2.1. 차트 생성

``` bash
$ helm create mychart
Creating mychart
```

## 2.2. templates 폴더 내에 기본 생성된 파일 삭제

``` bash
rm -rf mychart/templates/*
```

## 2.3. 첫번째 템플릿 작성

`mychart/templates/configmap.yaml`

``` yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: mychart-configmap
data:
  myvalue: "Hello World"
```

## 2.4. 템플릿 기반 차트 설치

``` bash
$ helm install full-coral ./mychart
NAME: full-coral
LAST DEPLOYED: Tue Nov  1 17:36:01 2016
NAMESPACE: default
STATUS: DEPLOYED
REVISION: 1
TEST SUITE: None
```

## 2.5. 릴리즈의 실제 템플릿 받기

``` bash
$ helm get manifest full-coral

---
# Source: mychart/templates/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: mychart-configmap
data:
  myvalue: "Hello World"
```

## 2.6. 간단한 템플릿 지시어 추가

`mychart/templates/configmap.yaml`

``` yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .Release.Name }}-configmap
data:
  myvalue: "Hello World"
```

- 템플릿 지시어는 `{{` 와 `}}` 로 감싸진다.
- `{{ .Release.Name }}` 와 같이 템플릿에 사용되는 values 를 `namespaced objects` 라고 하며 `.` 으로 구분된다.
- `{{ .Release.Name }}` 에서 `.Release` 의 맨 앞 `.` 은 최상위 namespace 를 말한다.
- `Release` 객체는 helm 의 built-in 객체이다.

## 2.7. 템플릿 랜더링 테스트

``` bash
$ helm install --debug --dry-run goodly-guppy ./mychart
```

# 3. Built-in Objects

- 빌트인 객체의 이름은 항상 대문자로 시작. 이는 Golang 의 네이밍 컨벤션을 따른다.
- 빌트인 객체와 구별하기 위해 local name 을 소문자로 시작해 사용하는 경우도 있다.

## 3.1. Release 객체

- Release : 릴리즈를 나타냄. 아래에 오브젝트들을 갖음.
  - Release.Name : 릴리즈 이름.
  - Release.Namespace : 릴리즈가 설치될 네임스페이스
  - Release.IsUpgrade : 현재 동작이 upgrade 나 rollback 이면 `true`.
  - Release.IsInstall : 현재 동작이 install 이면 `true`.
  - Release.Revision : 이 릴리즈의 revision number. 예를 들어, install 동작이라면 revision number 는 1이 됨. 그 후에, upgrade 나 rollback 마다 1 씩 증가.
- Values : `values.yaml` 파일과 사용자가 제공하는 파일들로부터 values 들이 template 들로 전달되는 것. 기본적으로 `Values` 는 비어있음.
- Chart : `Chart.yaml` 의 내용. 예를 들어, `{{ .Chart.Name }}-{{ .Chart.Version }}` 의 경우, `mychart-0.1.0` 으로 출력됨.
- Files : 차트 안에 파일들에 접근하는 기능. 차트 안에 template 들에 접근하는 것은 안되지만 그 외에 파일들에 접근 가능한 방법.
  - Files.Get : 이름으로 파일을 가져오는 함수.
  - Files.GetBytes : 파일의 내용을 스트링이 아닌 바이트 배열로 가져오는 함수. image 파일들에 유용함.
  - Files.Glob : 주어진 shell glob pattern 에 일치하는 파일의 목록을 반환하는 함수.
  - Files.Lines : 파일을 줄 마다 읽는 함수. 파일의 각 라인별로 반복할 때 유용함.
  - Files.AsSecrets : base64 인코딩된 string 으로 파일의 내용을 반환하는 함수.
  - Files.AsConfig : yaml map 으로 파일의 내용을 반환하는 함수.
- Capabilities : k8s 클러스터가 제공하는 capabilities 의 정보를 제공.
  - Capabilities.APIVersions: api 버전.
  - Capabilities.APIVersions.Has $version : 이 클러스터에서 특정 버전(e.g. `batch/v1`)이나 리소스(e.g. `apps/v1/Deployment`)가 사용가능한지 나타냄.
  - Capabilities.KubeVersion 과 Capabilities.KubeVersion.Version : k8s 버전.
  - Capabilities.KubeVersion.Major : k8s 의 major 버전.
  - Capabilities.KubeVersion.Minor : k8s 의 minor 버전.
  - Capabilities.HelmVersion : helm version 상세내용을 포함하는 객체. `helm version` 명령어의 출력과 같음.
  - Capabilities.HelmVersion.Version : 현재 helm version.
  - Capabilities.HelmVersion.GitCommit : helm git sha1.
  - Capabilities.HelmVersion.GitTreeState : helm git tree 의 상태.
  - Capabilities.HelmVersion.GoVersion : 사용된 Go 컴파일러의 버전.
- Template : 현재 실행되고 있는 템플릿의 정보를 포함.
  - Template.Name : 현재 템플릿의 namespaced file path. (e.g. `mychart/templates/mytemplate.yaml`)
  - Template.BasePath : 현재 차트의 template 경로에 대한 namespaced path. (e.g. `mychart/templates`)