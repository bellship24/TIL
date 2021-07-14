# 210508_iam_acess-key_mfa

## 배경

- AWS 계정 루트 사용자가 필요하지 않은 작업에는 루트 사용자를 사용하지 않는 것이 바람직함
- 그 대신에 관리자 액세스 권한(administrator access)이 필요한 사람마다 새 IAM User를 생성하여 managed policy 중에 AdministratorAccess를 연결하는 "관리자"(Administrators) Group에 사용자를 배치하여 그 사용자들을 관리자로 만들 것
- 그 후에 관리자 그룹에 속한 사용자들은 AWS 계정에 대한 그룹, 사용자 등을 설정해야 함
- 향후 모든 상호작용은 루트 사용자 대신에 AWS 계정 사용자와 그들의 고유 키를 통해 이루어져야 함
- 하지만 일부 계정 및 서비스 관리 작업을 수행하려면 루트 사용자 계정 자격 증명을 사용하여 로그인해야 함

## 관리자용 IAM 사용자 생성 (콘솔)

- 참고 URI: [https://docs.aws.amazon.com/ko_kr/IAM/latest/UserGuide/getting-started_create-admin-group.html](https://docs.aws.amazon.com/ko_kr/IAM/latest/UserGuide/getting-started_create-admin-group.html)

1. [**루트 사용자**]를 선택하고 AWS 계정 이메일 주소를 입력하여 계정 소유자로 [IAM 콘솔](https://console.aws.amazon.com/iam/)에 로그인합니다. 다음 페이지에서 암호를 입력합니다.

    **참고**
    **`Administrator`** IAM 사용자를 사용하는 아래 모범 사례를 준수하고, 루트 사용자 자격 증명을 안전하게 보관해 두는 것이 좋습니다. 몇 가지 [계정 및 서비스 관리 작업](https://docs.aws.amazon.com/general/latest/gr/aws_tasks-that-require-root.html)을 수행하려면 반드시 루트 사용자로 로그인해야 합니다.

2. 다음과 같이 생성할 IAM 관리자에 대한 결제 데이터 액세스를 활성화합니다.
    1. 탐색 표시줄에서 계정 이름을 선택한 다음 **내 계정**을 선택합니다.
    2. **결제 정보에 대한 IAM 사용자 및 역할 액세스** 옆에 있는 **편집**을 선택합니다.
    3. **IAM 액세스 활성화** 확인란을 선택하고 **업데이트**를 선택합니다.
    4. 탐색 표시줄에서 **서비스**를 선택한 다음, **IAM**을 선택해 IAM 대시보드로 돌아갑니다.
3. 탐색 창에서 **사용자**와 **사용자 추가**를 차례로 선택합니다.
4. **세부 정보** 페이지에서 다음을 수행합니다.
    1. [**User name**]에 **`Administrator`**를 입력합니다.
    2. **AWS Management 콘솔 액세스** 옆의 확인란을 선택하고 **사용자 지정 암호**를 선택한 다음 텍스트 상자에 새 암호를 입력합니다.
    3. 초기 상태는 AWS가 새로운 사용자가 로그인할 때 새 암호를 만들도록 요구합니다. 선택적으로 **User must create a new password at next sign-in(사용자는 다음번 로그인 시 새 암호를 생성해야 합니다)** 옆 확인란의 선택을 취소하여 새로운 사용자가 로그인한 후 암호를 재설정할 수 있습니다.
    4. **Next: Permissions(다음: 권한)**를 선택합니다.
5. **권한** 페이지에서 다음을 수행합니다.
    1. [**Add user to group**]을 선택합니다.
    2. **Create group**을 선택합니다.
    3. **그룹 생성** 대화 상자의 **그룹 이름**에 **`Administrators`**를 입력합니다.
    4. **AdministratorAccess** 정책 옆의 확인란을 선택합니다.
    5. **Create group**을 선택합니다.
    6. 그룹 목록이 있는 페이지로 돌아가 새 그룹 옆의 확인란을 선택합니다. 목록에 새 그룹이 표시되지 않으면 **새로 고침**을 선택합니다.
    7. **Next: Tags(다음: 태그)**를 선택합니다.
6. (선택 사항) **태그** 페이지에서 태그를 키-값 페어로 연결하여 메타데이터를 사용자에게 추가합니다. 자세한 내용은 [IAM 사용자 및 역할 태그 지정](https://docs.aws.amazon.com/ko_kr/IAM/latest/UserGuide/id_tags.html) 단원을 참조하십시오.
7. **Next: Review(다음: 검토)**를 선택합니다. 새 사용자에게 추가할 그룹 멤버십을 확인합니다. 계속 진행할 준비가 되었으면 **Create user**를 선택합니다.
8. (선택 사항) **완료** 페이지에서 사용자의 로그인 정보가 포함된 .csv 파일을 다운로드하거나 로그인 지침이 포함된 이메일을 사용자에게 보낼 수 있습니다.

이와 동일한 절차에 따라 그룹이나 사용자를 추가 생성하여 사용자에게 AWS 계정 리소스에 액세스할 수 있는 권한을 부여할 수 있습니다. 사용자 권한을 특정 AWS 리소스로 제한하는 정책을 사용하는 방법은 [AWS 리소스에 대한 액세스 관리](https://docs.aws.amazon.com/ko_kr/IAM/latest/UserGuide/access.html) 및 [IAM 자격 증명 기반 정책의 예](https://docs.aws.amazon.com/ko_kr/IAM/latest/UserGuide/access_policies_examples.html) 단원을 참조하십시오. 그룹을 생성한 후 추가로 사용자를 추가하려면 [IAM 그룹에서 사용자 추가 및 제거](https://docs.aws.amazon.com/ko_kr/IAM/latest/UserGuide/id_groups_manage_add-remove-users.html) 단원을 참조하십시오.

## 액세스 키 ID 및 보안 액세스 키

액세스 키는 액세스 키 ID 및 보안 액세스 키로 이루어져 있는데, 이를 사용하여 AWS에 보내는 프로그래밍 방식의 요청에 서명할 수 있습니다. 액세스 키가 없는 경우에는 AWS Management 콘솔에서 액세스 키를 생성할 수 있습니다. AWS 계정 루트 사용자 액세스 키가 필요하지 않은 작업에는 해당 액세스 키를 사용하지 않는 것이 가장 좋습니다. 대신 자신에 대한 액세스 키를 사용하여 [새 관리자 IAM 사용자](https://docs.aws.amazon.com/IAM/latest/UserGuide/getting-started_create-admin-group.html)를 생성하십시오.

보안 액세스 키는 액세스 키를 생성하는 시점에만 보고 다운로드할 수 있습니다. 나중에 복구할 수 없습니다. 하지만 언제든지 새 액세스 키를 생성할 수 있습니다. 필요한 IAM 작업을 수행할 수 있는 권한도 있어야 합니다. 자세한 내용은 *IAM 사용 설명서*의 [IAM 리소스에 액세스하는 데 필요한 권한](https://docs.aws.amazon.com/IAM/latest/UserGuide/access_permissions-required.html)을 참조하십시오.

**IAM 사용자에 대한 액세스 키를 생성하려면**

1. AWS Management 콘솔에 로그인한 다음 [https://console.aws.amazon.com/iam/](https://console.aws.amazon.com/iam/)에서 IAM 콘솔을 엽니다.
2. 탐색 창에서 **사용자**를 선택합니다.
3. 액세스 키를 생성할 사용자의 이름을 선택한 다음 **Security credentials(보안 자격 증명)** 탭을 선택합니다.
4. **액세스 키** 섹션에서 **Create access key(액세스 키 생성)**를 선택합니다.
5. 새 액세스 키 페어를 보려면 **표시**를 선택합니다. 이 대화 상자를 닫은 후에는 보안 액세스 키에 다시 액세스할 수 없습니다. 자격 증명은 다음과 비슷합니다.
    - 액세스 키 ID: AKIAIOSFODNN7EXAMPLE
    - 보안 액세스 키: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
6. 키 페어 파일을 다운로드하려면 [**Download .csv file**]을 선택합니다. 안전한 위치에 키를 저장합니다. 이 대화 상자를 닫은 후에는 보안 액세스 키에 다시 액세스할 수 없습니다.

    AWS 계정을 보호하기 위해 키를 기밀로 유지하고, 이메일로 전송하지 마십시오. AWS 또는 Amazon.com의 이름으로 문의가 온다 할지라도 조직 외부로 키를 공유하지 마십시오. Amazon을 합법적으로 대표하는 사람이라면 결코 보안 키를 요구하지 않을 것입니다.

7. `.csv` 파일을 다운로드한 후 **닫기**를 선택합니다. 액세스 키를 생성하면 키 페어가 기본적으로 활성화되므로 해당 페어를 즉시 사용할 수 있습니다.

## IAM 사용자에 대한 가상 MFA 디바이스 활성화(콘솔)

AWS Management 콘솔에서 IAM을 사용하여 계정의 IAM 사용자를 위한 가상 MFA 디바이스를 활성화 및 관리할 수 있습니다. AWS CLI 또는 AWS API를 사용하여 MFA 장치를 활성화하고 관리하려면 [가상 MFA 디바이스 활성화 및 관리(AWS CLI 또는 AWS API)](https://docs.aws.amazon.com/ko_kr/IAM/latest/UserGuide/id_credentials_mfa_enable_cliapi.html) 단원을 참조하십시오.

**참고**

MFA를 구성하려면 사용자의 가상 MFA 디바이스가 호스팅되는 하드웨어에 대한 물리적 액세스가 필요합니다. 예를 들어, 스마트폰에서 가상 MFA 디바이스를 실행하는 사용자에게 MFA를 구성할 수 있습니다. 이 경우 마법사를 완료하기 위해 스마트폰을 사용할 수 있어야 합니다. 이러한 이유로 사용자가 자신의 가상 MFA 디바이스를 직접 구성 및 관리할 수 있도록 허용하는 것이 좋습니다. 이 경우에는 사용자에게 필요한 IAM 작업 권한을 부여해야 합니다. 이러한 작업 권한을 부여하는 IAM 정책에 대한 자세한 내용과 예는 [AWS: MFA 인증 IAM 사용자가 내 보안 자격 증명(My Security Credentials) 페이지에서 자신의 MFA 디바이스를 관리할 수 있도록 허용합니다.](https://docs.aws.amazon.com/ko_kr/IAM/latest/UserGuide/reference_policies_examples_aws_my-sec-creds-self-manage-mfa-only.html) 단원을 참조하십시오.

**IAM 사용자에 대한 가상 MFA 디바이스 활성화(콘솔)**

1. AWS Management 콘솔에 로그인한 다음 [https://console.aws.amazon.com/iam/](https://console.aws.amazon.com/iam/)에서 IAM 콘솔을 엽니다.
2. 탐색 창에서 **Users(사용자)**를 선택합니다.
3. **사용자 이름** 목록에서 원하는 MFA 사용자 이름을 선택합니다.
4. **Security credentials(보안 자격 증명)** 탭을 선택합니다. **Assigned MFA device(할당된 MFA 디바이스)** 옆의 **관리**를 선택합니다.
5. **Manage MFA Device(할당된 MFA 디바이스)** 마법사에서 **Virtual MFA device(가상 MFA 디바이스 비활성화)**를 선택한 후 **계속**을 선택합니다.

    IAM은 QR 코드 그래픽을 포함하여 가상 MFA 디바이스의 구성 정보를 생성 및 표시합니다. 그래픽은 QR 코드를 지원하지 않는 디바이스 상에서 수동 입력할 수 있는 '보안 구성 키'를 표시한 것입니다.

6. 가상 MFA 앱을 엽니다. 가상 MFA 디바이스의 호스팅에 사용되는 앱 목록은 [멀티 팩터 인증](http://aws.amazon.com/iam/details/mfa/)을 참조하십시오.

    가상 MFA 앱이 다수의 가상 MFA 디바이스 또는 계정을 지원하는 경우 새로운 가상 MFA 디바이스 또는 계정을 생성하는 옵션을 선택합니다.

7. MFA 앱의 QR 코드 지원 여부를 결정한 후 다음 중 한 가지를 실행합니다.
    - 마법사에서 **Show QR code(QT 코드 표시)**를 선택한 다음 해당 앱을 사용하여 QR 코드를 스캔합니다. 예를 들어 카메라 모양의 아이콘을 선택하거나 **코드 스캔(Scan code)**과 비슷한 옵션을 선택한 다음, 디바이스의 카메라를 사용하여 코드를 스캔합니다.
    - **Manage MFA Device(MFA 디바이스 관리)** 마법사에서 **Show secret key(보안 키 표시)**을 선택한 다음 MFA 앱에 보안 키를 입력합니다.

    모든 작업을 마치면 가상 MFA 디바이스가 일회용 암호 생성을 시작합니다.

8. **Manage MFA Device(MFA 디바이스 관리)** 마법사의 **MFA code 1(MFA 코드 1)** 상자에 현재 가상 MFA 디바이스에 표시된 일회용 암호를 입력합니다. 디바이스가 새로운 일회용 암호를 생성할 때까지 최대 30초 기다립니다. 그런 다음 두 번째 일회용 암호를 **MFA code 2(MFA 코드 2)** 상자에 입력합니다. **Assign MFA(MFA 할당)**을 선택합니다.

    **중요**

    코드를 생성한 후 즉시 요청을 제출하십시오. 코드를 생성한 후 너무 오래 기다렸다 요청을 제출할 경우 MFA 디바이스가 사용자와 연결은 되지만 MFA 디바이스가 동기화되지 않습니다. 이는 시간 기반 일회용 암호(TOTP)가 잠시 후에 만료되기 때문입니다. 이 경우, [디바이스를 재동기화](https://docs.aws.amazon.com/ko_kr/IAM/latest/UserGuide/id_credentials_mfa_sync.html)할 수 있습니다.

이제 AWS에서 가상 MFA 디바이스를 사용할 준비가 끝났습니다. AWS Management 콘솔의 MFA 사용 방법에 대한 자세한 내용은 [IAM 로그인 페이지에 MFA 디바이스 사용](https://docs.aws.amazon.com/ko_kr/IAM/latest/UserGuide/console_sign-in-mfa.html) 단원을 참조하십시오.