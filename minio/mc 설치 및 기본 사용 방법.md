**목차**

- [1. 요약](#1-요약)
- [2. 전제](#2-전제)
- [3. mc 란?](#3-mc-란)
- [4. mc 기본 명령어](#4-mc-기본-명령어)
- [5. mc 다운로드](#5-mc-다운로드)
  - [5.1. Linux 기반 mc 다운로드](#51-linux-기반-mc-다운로드)
  - [5.2. docker 기반 mc 다운로드](#52-docker-기반-mc-다운로드)
  - [5.3. macOS 기반 mc 다운로드](#53-macos-기반-mc-다운로드)
- [6. mc 에 minio 서버 등록](#6-mc-에-minio-서버-등록)
- [7. 등록한 minio 서버 테스트](#7-등록한-minio-서버-테스트)
  - [7.1. 업로드 테스트](#71-업로드-테스트)
  - [7.2. 다운로드 테스트](#72-다운로드-테스트)
  - [7.3. 다운로드 URL 생성 및 테스트](#73-다운로드-url-생성-및-테스트)
  - [7.4. 업로드 URL 생성 및 테스트](#74-업로드-url-생성-및-테스트)
  - [7.5. share 목록 확인](#75-share-목록-확인)

**참고**

- [[MinIO docs] MinIO Client Complete Guide](https://docs.min.io/docs/minio-client-complete-guide.html)

---

# 1. 요약

- mc 는 minio client 로써 minio 서버와 상호작용할 수 있다. 예를 들면, 데이터 다운로드, 업로드나 토큰없이 권한을 부여하는 share 를 생성할 수 있다.
- 이런 mc 명령어는 linux 기본 명령어와 유사하다.
- mc 의 자세한 사용 방법은 `mc --help` 옵션에 자세히 나와있다.

# 2. 전제

- minio 서버

# 3. mc 란?

- MinIO Client(mc) 는 ls, cat, cp, mirror, diff 등과 같은 UNIX 명령어 기능을 제공한다.
- mc 는 파일시스템 과 Amazon S3 compatible 클라우드 스토리지 서비스(AWS Signature v2 및 v4) 를 지원한다.
- mc 명령어는 -h 옵션을 통해 자세한 설명과 많은 사용 예제들을 참고하면 쉽게 사용할 수 있다.

# 4. mc 기본 명령어

``` text
alias       set, remove and list aliases in configuration file
ls          list buckets and objects
mb          make a bucket
rb          remove a bucket
cp          copy objects
mirror      synchronize object(s) to a remote site
cat         display object contents
head        display first 'n' lines of an object
pipe        stream STDIN to an object
share       generate URL for temporary access to an object
find        search for objects
sql         run sql queries on objects
stat        show object metadata
mv          move objects
tree        list buckets and objects in a tree format
du          summarize disk usage recursively
retention   set retention for object(s) and bucket(s)
legalhold   set legal hold for object(s)
diff        list differences in object name, size, and date between two buckets
rm          remove objects
version     manage bucket versioning
ilm         manage bucket lifecycle
encrypt     manage bucket encryption config
event       manage object notifications
watch       listen for object notification events
undo        undo PUT/DELETE operations
policy      manage anonymous access to buckets and objects
tag         manage tags for bucket(s) and object(s)
replicate   configure server side bucket replication
admin       manage MinIO servers
update      update mc to latest release
```

# 5. mc 다운로드

- 참고 링크 [[MinIO docs] MinIO Client Complete Guide](https://docs.min.io/docs/minio-client-complete-guide.html) 내용을 참고하여 각 환경에 맞는 mc 를 다운로드할 수 있다.

## 5.1. Linux 기반 mc 다운로드

``` bash
wget https://dl.min.io/client/mc/release/linux-amd64/mc
sudo mv mc /usr/bin
mc --help
```

## 5.2. docker 기반 mc 다운로드

``` bash
docker pull minio/mc
docker run -it --entrypoint=/bin/sh minio/mc
sh-4.4# mc --help
```

## 5.3. macOS 기반 mc 다운로드

``` bash
brew install minio/stable/mc
mc --help
```

# 6. mc 에 minio 서버 등록

``` bash
$ mc config host add <server alias> <minio server url> <access key> <secret key>
Added `<server alias>` successfully.
```

- server alias : 해당 minio 서버를 alias 로 설정하고자 하는 이름

등록 확인

``` bash
mc config host list
```

# 7. 등록한 minio 서버 테스트

## 7.1. 업로드 테스트

``` bash
$ mc cp ./ca.crt minio/test-bk
./ca.crt:                    1.18 KiB / 1.18 KiB ┃▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┃ 31.23 KiB/s 0s

$ mc ls minio/test-bk
[2021-10-06 19:57:11 KST] 1.2KiB ca.crt
```

## 7.2. 다운로드 테스트

``` bash
$ mc cp minio/test-bk/ca.crt .
.../test-bk/ca.crt:  1.18 KiB / 1.18 KiB ┃▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓┃ 113.22 KiB/s 0s

$ ls
ca.crt
```

## 7.3. 다운로드 URL 생성 및 테스트

- 이 기능을 활용하면 토큰이 없어도 minio 서버의 데이터를 다운로드할 수 있다.

``` bash
$ mc share download [--expire 1h] minio/test-bk/ca.crt

URL: https://minio.bellship.com/test-bk/ca.crt
Expire: 1 hours 0 minutes 0 seconds
Share: https://minio.bellship.com/test-bk/ca.crt?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=jbpark%2F20211006%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20211006T105951Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Signature=7c62d1d157f996754b7e85855c068cd2dfeebac0ae36d3cab06245c26bf269f9

$ wget -O ca.crt 'https://minio.bellship.com/test-bk/ca.crt?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=jbpark%2F20211006%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20211006T105951Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Signature=7c62d1d157f996754b7e85855c068cd2dfeebac0ae36d3cab06245c26bf269f9'
```

## 7.4. 업로드 URL 생성 및 테스트

- 이 기능을 활용하면 토큰이 없어도 minio 서버의 데이터를 업로드할 수 있다.

``` bash
$ mc share upload minio/test-bk/ca.crt
Expire: 6 days 22 hours 42 minutes 21 seconds
Share: curl https://minio.bellship.com/test-bk/ -F x-amz-date=20211006T113104Z -F x-amz-signature=817da73ed2b1d9124ecf5a87d949f1e0e2c4821d90a5483b486c38229f39c69f -F bucket=test-bk -F policy=eyJleHBpcmF0aW9uIjoiMjAyMS0xMC0xM1QxMTozMTowNC4xNjhaIiwiY29uZGl0aW9ucyI6W1siZXEiLCIkYnVja2V0IiwidGVzdC1iayJdLFsiZXEiLCIka2V5IiwiY2EuY3J0Il0sWyJlcSIsIiR4LWFtei1kYXRlIiwiMjAyMTEwMDZUMTEzMTA0WiJdLFsiZXEiLCIkeC1hbXotYWxnb3JpdGhtIiwiQVdTNC1ITUFDLVNIQTI1NiJdLFsiZXEiLCIkeC1hbXotY3JlZGVudGlhbCIsImxkY2NhaS8yMDIxMTAwNi91cy1lYXN0LTEvczMvYXdzNF9yZXF1ZXN0Il1dfQ== -F x-amz-algorithm=AWS4-HMAC-SHA256 -F x-amz-credential=jbpark/20211006/us-east-1/s3/aws4_request -F key=ca.crt -F file=@<FILE>

$ curl https://minio.bellship.com/test-bk/ -F x-amz-date=20211006T113104Z -F x-amz-signature=817da73ed2b1d9124ecf5a87d949f1e0e2c4821d90a5483b486c38229f39c69f -F bucket=test-bk -F policy=eyJleHBpcmF0aW9uIjoiMjAyMS0xMC0xM1QxMTozMTowNC4xNjhaIiwiY29uZGl0aW9ucyI6W1siZXEiLCIkYnVja2V0IiwidGVzdC1iayJdLFsiZXEiLCIka2V5IiwiY2EuY3J0Il0sWyJlcSIsIiR4LWFtei1kYXRlIiwiMjAyMTEwMDZUMTEzMTA0WiJdLFsiZXEiLCIkeC1hbXotYWxnb3JpdGhtIiwiQVdTNC1ITUFDLVNIQTI1NiJdLFsiZXEiLCIkeC1hbXotY3JlZGVudGlhbCIsImxkY2NhaS8yMDIxMTAwNi91cy1lYXN0LTEvczMvYXdzNF9yZXF1ZXN0Il1dfQ== -F x-amz-algorithm=AWS4-HMAC-SHA256 -F x-amz-credential=jbpark/20211006/us-east-1/s3/aws4_request -F key=ca.crt -F file=@ca.crt
```

## 7.5. share 목록 확인

``` bash
$ mc share list download
URL: https://minio.bellship.com/test-bk/ca.crt
Expire: 6 days 23 hours 20 minutes 20 seconds
Share: https://minio.bellship.com/test-bk/ca.crt?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=jbpark%2F20211006%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20211006T122035Z&X-Amz-Expires=604800&X-Amz-SignedHeaders=host&X-Amz-Signature=d636842c500425052fda067015c75cf000babc9bdb12e1d0783880aebb49c9ed

jbpark@cicd-ops-ai-m1:~/jongbae$ mc share list upload
URL: https://minio.bellship.com/test-bk/ca.crt
Expire: 6 days 22 hours 30 minutes 46 seconds
Share: curl https://minio.bellship.com/test-bk/ -F x-amz-date=20211006T113104Z -F x-amz-signature=817da73ed2b1d9124ecf5a87d949f1e0e2c4821d90a5483b486c38229f39c69f -F bucket=test-bk -F policy=eyJleHBpcmF0aW9uIjoiMjAyMS0xMC0xM1QxMTozMTowNC4xNjhaIiwiY29uZGl0aW9ucyI6W1siZXEiLCIkYnVja2V0IiwidGVzdC1iayJdLFsiZXEiLCIka2V5IiwiY2EuY3J0Il0sWyJlcSIsIiR4LWFtei1kYXRlIiwiMjAyMTEwMDZUMTEzMTA0WiJdLFsiZXEiLCIkeC1hbXotYWxnb3JpdGhtIiwiQVdTNC1ITUFDLVNIQTI1NiJdLFsiZXEiLCIkeC1hbXotY3JlZGVudGlhbCIsImxkY2NhaS8yMDIxMTAwNi91cy1lYXN0LTEvczMvYXdzNF9yZXF1ZXN0Il1dfQ== -F x-amz-algorithm=AWS4-HMAC-SHA256 -F x-amz-credential=jbpark/20211006/us-east-1/s3/aws4_request -F key=ca.crt -F file=@<FILE>
```