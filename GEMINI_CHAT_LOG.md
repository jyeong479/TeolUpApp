# [최종 보고서] 생성형 AI(Gemini)를 활용한 안드로이드 앱 개발 및 문제 해결 리포트

**개요:** 본 프로젝트(TeolUpApp)는 Android 기반의 냉장고 파먹기 어플리케이션으로, 개발 과정에서 발생한 아키텍처 설계, 비동기 통신, UI/UX 최적화 등의 난제를 AI Agent(Gemini)와의 긴밀한 협업을 통해 해결하였습니다.

---

## 1. [아키텍처] 다중 사용자(Multi-user) 데이터 격리 구조 설계

### 1.1 문제 상황
- 초기 프로토타입은 단일 사용자 기준으로 설계되어, `refri`(냉장고)와 `shopping`(장바구니) 데이터가 모든 기기에서 공유되는 보안 취약점 존재.

### 1.2 AI 솔루션 및 구현
- **Database Schema Redesign:** Firebase Realtime Database 구조를 `User -> Data`가 아닌 `Data Type -> UID -> Item` 계층 구조(`refri/{uid}/...`)로 정규화.
- **Context-Aware Logic:** `MyrefriFragment`, `CartFragment` 진입 시 `FirebaseAuth` 인스턴스로부터 현재 세션의 `UID`를 동적으로 추출하여, 해당 사용자의 데이터 공간(Node)에만 접근하도록 DAO 로직 전면 수정.

---

## 2. [AI 기술] On-Device + Cloud Hybrid 레시피 추천 시스템

### 2.1 문제 상황
- 기존의 정적(Static) 레시피 데이터베이스로는 사용자의 다양한 냉장고 상황(유통기한, 자투리 재료 등)을 반영한 추천이 불가능.

### 2.2 AI 솔루션 및 구현
- **SDK Integration:** `com.google.ai.client.generativeai` 라이브러리를 활용하여 Gemini Pro 비전/텍스트 모델 연동.
- **Dynamic Prompt Engineering:**
  - 사용자의 냉장고 DB(`Snapshot`)를 순회하며 재료 리스트를 텍스트로 직렬화.
  - *"유통기한이 오늘 내일 하는 {재료목록}을 최우선으로 소진할 수 있는 자취생 레시피를 알려줘"* 와 같은 페르소나(Persona) 기반 프롬프트 적용으로 답변 품질 향상.

---

## 3. [UX/알림] Android 13 대응 스마트 푸시 알림 시스템

### 3.1 문제 상황
- Android 13(API 33)부터 도입된 '알림 런타임 권한(`POST_NOTIFICATIONS`)' 미대응으로 인해 최신 기기에서 알림 기능 먹통 현상 발생.
- 백그라운드에서 유통기한을 체크하는 로직의 복잡성.

### 3.2 AI 솔루션 및 구현
- **Permission Handling:** `SettingFragment`에서 권한 허용 여부를 체크하고, `ActivityResultLauncher`를 통해 사용자에게 명시적으로 권한을 요청하는 표준 플로우 구현.
- **Date Logic Algorithm:** `java.time.LocalDate`와 `ChronoUnit.DAYS`를 사용하여, 단순 날짜 비교가 아닌 '남은 일수(D-Day)'를 정확히 계산하고 7일 이내 임박 상품만 필터링하는 알고리즘 작성.

---

## 4. [보안/데이터] 계정 생명주기 관리 및 Cascading Delete

### 4.1 문제 상황
- 회원 탈퇴 시 Authentication 계정만 삭제되고 Realtime Database에는 데이터가 잔존하여 저장소 비용 증가 및 개인정보 파기 원칙 위반 우려.

### 4.2 AI 솔루션 및 구현
- **Atomic Operation:** 회원 탈퇴 트랜잭션을 [DB 데이터 삭제] -> [Auth 계정 삭제] 순서로 설계.
- **Error Handling:** 로그인 세션이 만료된 상태에서 탈퇴 시도 시 발생하는 `FirebaseAuthRecentLoginRequiredException` 예외를 처리하고 재로그인을 유도하는 UX 시나리오 적용.

---

## 5. [UI 커스터마이징] 상태 기반(State-Driven) 체크박스 디자인

### 5.1 문제 상황
- Material Component의 기본 테마 색상(`Primary`)이 강제 적용되어, 체크박스 선택 시 내부 체크 아이콘(Checkmark)의 시인성이 떨어지는 문제.

### 5.2 AI 솔루션 및 구현
- **Resource Selector:** `res/color/selector_checkbox_theme.xml`을 생성하여 체크 상태(`state_checked`)에 따른 색상 매핑 테이블 정의.
- **Attribute Correction:** 구버전 호환성을 위한 `app:buttonTint` 속성을 사용하여, 제조사/OS 버전과 무관하게 일관된 디자인(보라색 배경 + 흰색 체크)이 렌더링되도록 수정.

---

## 6. [UI 컴포넌트] 사용자 친화적 프로필 수정 다이얼로그

### 6.1 문제 상황
- 기본 `AlertDialog` 빌더만으로는 '닉네임 수정'과 '비밀번호 변경/확인'이라는 복합적인 입력 시나리오를 수용하기에 한계가 있음.

### 6.2 AI 솔루션 및 구현
- **Custom View Inflation:** `dialog_profile_edit.xml` 레이아웃을 별도로 설계하고 `LayoutInflater`를 통해 다이얼로그에 주입.
- **Validation Logic:** '새 비밀번호'와 '비밀번호 확인' 필드의 일치 여부, 닉네임 공백 여부 등을 클라이언트 단에서 즉시 검증하여 불필요한 서버 요청 방지.

---

## 7. [트러블 슈팅] 빌드 리소스 링크 에러 해결

### 7.1 문제 상황
- 기능 병합 과정에서 `Aapt2Exception: resource drawable/shape_dialog_bg not found` 에러 발생으로 빌드 파이프라인 중단.

### 7.2 AI 솔루션 및 구현
- **Rapid Debugging:** 에러 로그 분석을 통해 누락된 리소스 파일을 특정.
- **Resource Generation:** XML Shape 태그(`<shape>`, `<corners>`)를 활용하여 다이얼로그의 둥근 모서리 배경 리소스를 즉시 생성 및 복구.

---

## 8. [개발 환경] Gradle 종속성 및 라이브러리 관리

### 8.1 문제 상황
- Firebase, Gemini AI, Android Jetpack 등 다양한 라이브러리의 버전 충돌 및 BOM(Bill of Materials) 관리 필요성 대두.

### 8.2 AI 솔루션 및 구현
- **Dependency Management:** `com.google.firebase:firebase-bom`을 도입하여 Auth, Database 등 모듈 간 버전 호환성 자동 관리.
- **Module Configuration:** Generative AI SDK(`generativeai`)와 Coroutine 라이브러리 간의 의존성 흐름을 파악하고 최적화된 버전 구성 적용.

---

## 9. [코딩 컨벤션] 방어적 프로그래밍 (Defensive Programming)

### 9.1 문제 상황
- 비동기 데이터 로드 중 `NullPointerException`이나 앱 강제 종료(Crash) 발생 가능성.

### 9.2 AI 솔루션 및 구현
- **Safe Call & Scope:** Kotlin의 `?.`(Safe Call) 연산자와 `run`, `let` 스코프 함수를 적극 활용하여 Null Safety 확보.
- **Fragment Lifecycle:** `View`가 파괴된 후(`onDestroyView`) 데이터 콜백이 호출되어 발생하는 메모리 누수 및 크래시를 방지하기 위해 `viewLifecycleOwner` 및 `isAdded` 체크 로직 적용.

---

## 10. [협업/배포] Git 기반 버전 관리 시스템 구축

### 10.1 문제 상황
- 1인 개발임에도 기능 단위의 히스토리 관리 부재로 롤백(Rollback) 불가능 위험.
- API Key 등 민감 정보가 소스코드에 포함될 위험.

### 10.2 AI 솔루션 및 구현
- **Feature Branch Strategy:** 기능별(Logout, UI Fix, Profile)로 작업을 나누어 `git add` 및 `commit`을 수행하는 워크플로우 정립.
- **Security Advisory:** 소스코드 내 하드코딩된 API Key의 위험성을 인지하고, `.gitignore` 및 `local.properties`를 통한 환경 변수 분리 전략 수립(보고서 제언 사항).
