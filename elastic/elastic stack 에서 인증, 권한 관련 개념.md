<!-- TOC -->
- [elastic stack 에서 인증, 권한 관련 개념](#elastic-stack-에서-인증-권한-관련-개념)
  - [Native user authentication](#native-user-authentication)
    - [Native user authentication 이란?](#native-user-authentication-이란)
  - [native realm 설정 방법](#native-realm-설정-방법)
  - [Built-in user](#built-in-user)
    - [Built-in user 란?](#built-in-user-란)
    - [Built-in user 작동 방법](#built-in-user-작동-방법)
    - [Elastic bootstrap password](#elastic-bootstrap-password)
  - [Kibana 에서 인증](#kibana-에서-인증)
  - [참고](#참고)
<!-- /TOC -->

# elastic stack 에서 인증, 권한 관련 개념

## Native user authentication

### Native user authentication 이란?

- 유저를 인증하고 관리하는 가장 쉬운 방법은 `native realm` 이다.
- REST API 나 Kibana 를 통해 계정이나 role 을 추가, 변경, 삭제할 수 있다.
- 다른 realm 이 설정되지 않는다면, 기본적으로 native realm 이 available 하다.
- 만약, 다른 realm 을 `elasticsearch.yml` 에 설정했을 때, native realm 을 쓰고 싶다면, 이것도 realm chain 에 추가해줘야 한다.

## native realm 설정 방법

- `elasticsearch.yml` 파일 안에 `xpack.security.auth.realms.native` 네임스페이스에서 native realm 을 설정할 수 있다.
- 여기서 realm 을 설정할 때는, realm 간에 우선순위를 두기 위해 `order` 도 작성하자.
- e.g.

    ``` yaml
    xpack:
    security:
        authc:
        realms:
            native:
            native1:
                order: 0
    ```

## Built-in user

### Built-in user 란?

- built-in user 는 패스워드 가 설정될 때 까지 인증될 수 없음.
- `elastic` 유저는 build-in superuser 로써 built-in 유저의 패스워드를 설정할 수 있음.
- built-in user 는 특정한 목적을 위해 사용되며 범용적으로 사용하면 안 된다.
- built-in user = [elastic, kibana_system, logstash_system, beats_system, apm_system, remote_monitoring_user]

### Built-in user 작동 방법

- built-in user 는 ES 의 `.security` index 에 저장됨.
- 특정 built-in user 가 disabled 되거나 패스워드 변경이 되면 이 변화를 클러스터 안에 있는 각 노드에 자동으로 반영함.

### Elastic bootstrap password

- ES 를 설치하면, `elastic` 유저는 디폴트 설정에 따라 bootstrap password 를 갖고있다. 이는 임시 비밀번호이며 built-in user 패스워드를 설정하는 데 사용된다. 이 키는 랜덤으로 배정된다. 필요에 따라, `bootstrap.password` 를 keystore 네임스페이스에 따로 설정하여 변경 가능하다.

## Kibana 에서 인증

- 여러 가지 auth provider 를 우선순위로 적용 가능.
- 각 provider 는 이름이 unique 해야 함.
- 유저에게 `login instruction` 을 제공하고 싶다면, `xpack.security.loginHelp` 세팅을 해야 함.
- `showInSelector` 를 `false` 로 세팅하면, 특정한 provider 를 UI 에 표시하지 않게 할 수 있음.

## 참고

- [Elastic docs 'Native user authentication'](https://www.elastic.co/guide/en/elasticsearch/reference/7.10/native-realm.html)
- [Blog 'Elasticsearch - Enable user authentication'](https://techexpert.tips/elasticsearch/elasticsearch-enable-user-authentication/)
