**요약**

- TorchServe GitHub 의 README.md 를 보고 내용을 학습, 정리, 실습 해보자.

**목차**

- [1. TorchServe 란?](#1-torchserve-란)
- [2. TorchServe 아키텍처](#2-torchserve-아키텍처)
  - [2.1. terminology](#21-terminology)
- [3. TorchServe 설치](#3-torchserve-설치)
  - [3.1. 전체 폴더 구조](#31-전체-폴더-구조)
  - [3.2. 의존성 설치](#32-의존성-설치)
  - [3.3. torchserve, torch-model-archiver, torch-workflow-archiver 설치](#33-torchserve-torch-model-archiver-torch-workflow-archiver-설치)
- [4. 모델 서빙](#4-모델-서빙)
  - [4.1. 모델 저장 (pth -> MAR)](#41-모델-저장-pth---mar)
  - [4.2. 모델 서빙을 위해 torchserve 시작](#42-모델-서빙을-위해-torchserve-시작)
  - [4.3. 모델로부터 예측값 얻기](#43-모델로부터-예측값-얻기)
    - [4.3.1. REST API 사용](#431-rest-api-사용)
    - [4.3.2. 추론 결과 로그](#432-추론-결과-로그)
    - [4.3.3. gRPC 사용](#433-grpc-사용)
- [5. 모델 중지](#5-모델-중지)
- [6. concurrency 와 worker 수](#6-concurrency-와-worker-수)
- [7. 기타](#7-기타)

**참고**

- [[TorchServe GitHub] README.md](https://github.com/pytorch/serve)

# 1. TorchServe 란?

- pytorch 모델을 serving 하기 위한 유연하고 쉬운 tool 이다.

# 2. TorchServe 아키텍처

![](/.uploads/2021-06-21-14-58-26.png)

## 2.1. terminology

- Frontend: 토치서브의 호출/응답 핸들링 컴포넌트.
- Model Workers: 모델 위에서 돌아가는 실제 inference 역할.
- Model: 모델의 형태는 `script_module` (JIT saced models) 또는 `eager_mode_models` 이다. 이런 모델은 state_dicts 같은 다른 model artifacts 와 함께 데이터의 전처리, 후처리가 가능하다.
- Plugins: startup time 중에 TorchServe 안에 들어갈 수 있는 custom endpoints 혹은 authz/authn 또는 batching 알고리즘.
- Model Store: load 될 모델들이 있는 디렉토리.

# 3. TorchServe 설치

## 3.1. 전체 폴더 구조

- 아래에 폴더 구조를 참고하면 검증하는데 이해가 더 잘 될 것이다.

``` bash
(mytorchserve) /torchserve$ tree -L 2
.
├── densenet161-8d451a50.pth
├── kitten_small.jpg
├── model_store
│   └── densenet161.mar
└── serve
    ├── benchmarks
    ├── binaries
    ├── captum
    ├── ci
    ├── cloudformation
    ├── CODE_OF_CONDUCT.md
    ├── _config.yml
    ├── CONTRIBUTING.md
    ├── docker
    ├── docs
    ├── examples
    ├── frontend
    ├── kubernetes
    ├── LICENSE
    ├── LICENSE.txt
    ├── link_check_config.json
    ├── MANIFEST.in
    ├── model-archiver
    ├── plugins
    ├── pull_request_template.md
    ├── PyPiDescription.rst
    ├── README.md
    ├── requirements
    ├── run_circleci_tests.py
    ├── serving-sdk
    ├── setup.py
    ├── test
    ├── torchserve_sanity.py
    ├── ts
    ├── ts_scripts
    └── workflow-archiver
```

## 3.2. 의존성 설치

- 먼저, torchserve repo. 를 클론하고 디렉토리를 변경하자.

``` bash
git clone https://github.com/pytorch/serve.git
cd serve
```

- conda, python v3.8 필요.
- debian, macOS 기반
  - CPU 환경

    ``` bash
    python ./ts_scripts/install_dependencies.py    
    ```

  - GPU 환경

    ``` bash
    python ./ts_scripts/install_dependencies.py --cuda=cu102
    ### cuda 10.2 -> cu102, 9.2 -> cu902, cuda11.1 -> cu111
    ```

## 3.3. torchserve, torch-model-archiver, torch-workflow-archiver 설치

- conda 기반

    ``` bash
    conda install torchserve torch-model-archiver torch-workflow-archiver -c pytorch
    ```

- pip 기반

    ``` bash
    pip install torchserve torch-model-archiver torch-workflow-archiver
    ```

# 4. 모델 서빙

- 설치가 완료 됐으면 이제 torchserve 를 사용해서 모델을 서빙하는 간단한 예제를 보자.

## 4.1. 모델 저장 (pth -> MAR)

- 먼저, 모델은 MAR 파일로 압축하자. model archiver 로 모델은 패키징할 수 있다. 이렇게, 압축된 모델을 저장하기 위해 `model store` 폴더를 만들자.
- 압축된 모델 MAR 를 저장할 폴더 생성.

``` bash
cd <torchserve 경로>
mkdir model_store
```

- 학습된 모델 다운로드.

``` bash
wget https://download.pytorch.org/models/densenet161-8d451a50.pth
```

- model archiver 로 모델 압축. `extra-files` 파라미터를 사용해 `TorchServe` repo. 로부터 파일을 사용할 수 있다. 즉, 필요하면 경로를 업데이트하자.

``` bash
torch-model-archiver --model-name densenet161 --version 1.0 --model-file ./serve/examples/image_classifier/densenet_161/model.py --serialized-file densenet161-8d451a50.pth --export-path model_store --extra-files ./serve/examples/image_classifier/index_to_name.json --handler image_classifier
```

- MAR 가 생성됐는지 확인

``` bash
$ ls model_store/
densenet161.mar
```

## 4.2. 모델 서빙을 위해 torchserve 시작

- 모델을 압축하고 저장한 뒤, `torchserve` 명령어를 사용해 모델을 서빙하자.

``` bash
torchserve --start --ncs --model-store model_store --models densenet161.mar
```

- 위 torchserve 명령어를 수행하고나면, torchserve 가 host 에서 실행되고 inference request 에 대해 listening 한다.
- torchserve 를 실행할 때, 특정 모델을 명시하면, 자동으로 backend worker 들을 사용 가능한 vCPU(CPU 인스턴스에서 돌린다면) 또는 GPU 개수(GPU 인스턴스에서 돌린다면) 숫자에 맞춰 auto scaling 해준다. 만약, 리소스가 충분한 호스트의 경우, start up 과 autoscaling 하는데 상당 시간이 걸릴 수 있다.
- 만약, torchserve 의 start up time 을 최소화하고 싶다면, start up time 동안에 그 모델을 registering 및 scaling 하는 것을 피하고 이 작업에 대해 Management API 를 사용하여 뒷단으로 옮기자.

## 4.3. 모델로부터 예측값 얻기

- 모델 서버를 테스트하기 위해 서버의 predictions API 로 호출을 보내자.
- TorchServe 는 모든 inference 에 대해 management API 를 통해 gRPC 와 HTTP/REST 를 지원한다.

### 4.3.1. REST API 사용

- 먼저, 고양이 이미지를 다운로드 하자.

``` bash
curl -O https://raw.githubusercontent.com/pytorch/serve/master/docs/.uploads/kitten_small.jpg
```

- 다운 받은 고양이 이미지로 prediction endpoint 로 호출하자.

``` bash
curl http://127.0.0.1:8080/predictions/densenet161 -T kitten_small.jpg
```

- 추론 결과 JSON 이 아래와 같다.

``` json
[
  {
    "tiger_cat": 0.46933549642562866
  },
  {
    "tabby": 0.4633878469467163
  },
  {
    "Egyptian_cat": 0.06456148624420166
  },
  {
    "lynx": 0.0012828214094042778
  },
  {
    "plastic_bag": 0.00023323034110944718
  }
]
```

### 4.3.2. 추론 결과 로그

- 이 endpoint 의 모든 interaction 은 `logs/` 디렉토리에 로깅된다.

### 4.3.3. gRPC 사용

# 5. 모델 중지

``` bash
torchserve --stop
```

# 6. concurrency 와 worker 수

- torchserve 는 사용자가 CPU 와 GPU 에 대해 워커 쓰레드의 수를 설정할 수 있도록 configuration 을 제공한다. 워크로드에 따라 서버를 가속할 수 있는 구성 요소가 있다.
- GPU 로 호스팅되는 torchserve 는 서버에게 모델 당 쓸 수 있는 GPU 수를 지정할 수 있는 `number_of_gpu` 구성 요소가 있다. 만약, 서버에 여러 개의 모델을 등록했다면, 이 `number_of_gpu` 는 모든 모델에 적용된다.
- 만약, `number_of_gpu` 를 0 또는 1 처럼 낮은 값으로 설정하면, GPU 의 under-utilization 이 발생한다. 반대로, 시스템 가용 GPU 수보다 높은 값으로 설정하면, 만은 워커들이 모델당 spawned 될 수 있다. 확실히 이런 부적절한 설정은 GPU 의 불필요한 자원 낭비나 최적화되지 않은 GPU 스케줄링을 초래할 수 있다.

``` text
ValueToSet = (Number of Hardware GPUs) / (Number of Unique Models)
```

# 7. 기타

실제 검증한 환경

``` bash
(mytorchserve) $ python -c 'import sys; print(sys.version_info[:])'
(3, 9, 5, 'final', 0)

$ pip list
Package                 Version
----------------------- ---------
beautifulsoup4          4.9.3
brotlipy                0.7.0
captum                  0.3.1
certifi                 2021.5.30
cffi                    1.14.5
chardet                 4.0.0
conda                   4.10.1
conda-build             3.21.4
conda-package-handling  1.7.3
cryptography            3.4.7
cycler                  0.10.0
enum-compat             0.0.3
filelock                3.0.12
future                  0.18.2
glob2                   0.7
idna                    2.10
Jinja2                  3.0.0
kiwisolver              1.3.1
libarchive-c            2.9
MarkupSafe              2.0.1
matplotlib              3.4.2
mkl-fft                 1.3.0
mkl-random              1.0.2
mkl-service             2.3.0
numpy                   1.21.0
packaging               20.9
Pillow                  8.2.0
pip                     21.1.2
pkginfo                 1.7.0
psutil                  5.8.0
pycosat                 0.6.3
pycparser               2.20
pyOpenSSL               20.0.1
pyparsing               2.4.7
PySocks                 1.7.1
python-dateutil         2.8.1
pytz                    2021.1
PyYAML                  5.4.1
requests                2.25.1
ruamel-yaml-conda       0.15.100
sentencepiece           0.1.96
setuptools              57.0.0
six                     1.16.0
soupsieve               2.2.1
torch                   1.8.1+cpu
torch-model-archiver    0.4.0
torch-workflow-archiver 0.1.0
torchaudio              0.8.1
torchserve              0.4.0
torchtext               0.9.1
torchvision             0.9.1+cpu
tqdm                    4.59.0
typing-extensions       3.7.4.3
urllib3                 1.26.4
wheel                   0.36.2
```