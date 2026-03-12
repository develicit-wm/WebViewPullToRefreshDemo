# CLAUDE.md

## 프로젝트 개요

Android WebView + Pull-to-Refresh 데모 앱. Jetpack Compose 기반.

## 빌드

```bash
./gradlew assembleDebug
./gradlew installDebug  # 기기 연결 필요
```

## 핵심 파일

- `app/src/main/java/com/demo/webviewptr/MainActivity.kt` — Compose UI, WebView, Pull-to-Refresh 로직 전체
- `app/src/main/assets/editor.html` — WebView에 로드되는 HTML 페이지 (게시글 작성 폼 + 중첩 스크롤 테스트)
- `app/build.gradle.kts` — 의존성 관리 (Compose BOM, Material2/3)

## 아키텍처

- `ComponentActivity` + `setContent`로 Compose 진입
- `Scaffold` > `Column` > `Box(pullRefresh)` > `AndroidView(WebView)` 구조
- WebView ↔ Native 통신: `JavascriptInterface` (`AndroidBridge`)
- Pull-to-Refresh: Material2 `pullRefresh` modifier + `PullRefreshIndicator`

## 코딩 컨벤션

- Kotlin, Jetpack Compose 사용
- XML 레이아웃 사용하지 않음
- WebView 관련 콘텐츠는 `assets/` 내 HTML로 관리
- 한국어 UI, 한국어 주석 허용

## 의존성

- Kotlin 1.9.20, Compose Compiler 1.5.5
- Compose BOM 2024.01.00
- AGP 8.2.0, minSdk 24, targetSdk 34
